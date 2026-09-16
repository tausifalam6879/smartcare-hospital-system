import {
  Activity, BookOpen, CalendarDays, CheckCircle2, CircleDot, Clock3, FlaskConical,
  HeartPulse, IndianRupee, LoaderCircle, MapPin, ScanLine, ShieldCheck,
  Stethoscope, TriangleAlert, XCircle,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { messageFromError } from '../services/api'
import {
  cancelDiagnosticOrder, getDiagnosticProcedures, getMyDiagnosticOrders,
  scheduleDiagnosticOrder, type DiagnosticModality, type DiagnosticOrder,
  type DiagnosticOrderStatus, type DiagnosticProcedure, type DiagnosticResultFlag,
} from '../services/diagnostics'
import { getNavigationHospitals, type HospitalSummary } from '../services/navigation'
import { localDateString } from '../utils/appointmentLifecycle'

const statusLabels: Record<DiagnosticOrderStatus, string> = {
  ORDERED: 'Awaiting schedule',
  SCHEDULED: 'Scheduled',
  SAMPLE_COLLECTED: 'Arrived / sample collected',
  IN_PROGRESS: 'Processing',
  RESULT_VERIFIED: 'Verified result ready',
  CANCELLED: 'Cancelled',
}

const statusStyles: Record<DiagnosticOrderStatus, string> = {
  ORDERED: 'bg-amber-50 text-amber-800 border-amber-200',
  SCHEDULED: 'bg-blue-50 text-blue-800 border-blue-200',
  SAMPLE_COLLECTED: 'bg-violet-50 text-violet-800 border-violet-200',
  IN_PROGRESS: 'bg-cyan-50 text-cyan-800 border-cyan-200',
  RESULT_VERIFIED: 'bg-emerald-50 text-emerald-800 border-emerald-200',
  CANCELLED: 'bg-slate-100 text-slate-600 border-slate-200',
}

const resultStyles: Record<DiagnosticResultFlag, string> = {
  NORMAL: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  ABNORMAL: 'border-amber-200 bg-amber-50 text-amber-950',
  CRITICAL: 'border-rose-300 bg-rose-50 text-rose-950',
  INDETERMINATE: 'border-slate-300 bg-slate-50 text-slate-800',
}

const modalityLabels: Record<DiagnosticModality, string> = {
  LAB: 'Laboratory', MRI: 'MRI', CT: 'CT scan', X_RAY: 'X-ray',
}

function iconFor(modality: DiagnosticModality) {
  return modality === 'LAB' ? FlaskConical : ScanLine
}

function diagnosticNavigationUrl(order: DiagnosticOrder) {
  const params = new URLSearchParams({ hospital: order.hospitalId })
  if (order.building) params.set('building', order.building)
  if (order.floorLabel) params.set('floor', order.floorLabel)
  if (order.roomNumber) params.set('room', order.roomNumber)
  return `/navigate?${params.toString()}`
}

function displayDate(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`))
}

function displayDateTime(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function tomorrow() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return localDateString(date)
}

function procedureGuide(procedure: DiagnosticProcedure) {
  if (procedure.modality === 'LAB') return {
    what: 'A trained collection team takes the sample listed in the clinician order and labels it against your patient details.',
    happens: 'Check in, confirm your name and order, give the requested sample, then track processing until an authorized professional verifies the result.',
  }
  if (procedure.modality === 'MRI') return {
    what: 'An imaging team uses an MRI scanner to create the images requested by your clinician.',
    happens: 'A safety screening is completed first. You lie on the scanner table and the team communicates with you throughout the scan.',
  }
  if (procedure.modality === 'CT') return {
    what: 'A CT imaging team creates the body images specified in your clinician order.',
    happens: 'The team confirms identity, pregnancy and contrast-related safety questions before positioning you for the scan.',
  }
  return {
    what: 'A radiography team creates the image requested in your clinician order.',
    happens: 'The team confirms identity, explains positioning, completes the image and sends it for authorized review.',
  }
}

export function DiagnosticsPage() {
  const [orders, setOrders] = useState<DiagnosticOrder[]>([])
  const [hospitals, setHospitals] = useState<HospitalSummary[]>([])
  const [procedures, setProcedures] = useState<DiagnosticProcedure[]>([])
  const [hospitalId, setHospitalId] = useState('')
  const [modality, setModality] = useState<DiagnosticModality | ''>('')
  const [dates, setDates] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [catalogLoading, setCatalogLoading] = useState(false)
  const [workingId, setWorkingId] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [selectedProcedure, setSelectedProcedure] = useState<DiagnosticProcedure | null>(null)

  async function loadOrders() {
    setOrders(await getMyDiagnosticOrders())
  }

  useEffect(() => {
    Promise.all([getMyDiagnosticOrders(), getNavigationHospitals()])
      .then(([orderData, hospitalData]) => {
        setOrders(orderData)
        setHospitals(hospitalData)
        setHospitalId(hospitalData[0]?.id ?? '')
      })
      .catch((requestError) => setError(messageFromError(requestError)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!hospitalId) { setProcedures([]); return }
    setCatalogLoading(true)
    getDiagnosticProcedures(hospitalId, modality || undefined)
      .then(setProcedures)
      .catch((requestError) => setError(messageFromError(requestError)))
      .finally(() => setCatalogLoading(false))
  }, [hospitalId, modality])

  const counts = useMemo(() => ({
    awaiting: orders.filter((item) => item.status === 'ORDERED').length,
    upcoming: orders.filter((item) => item.status === 'SCHEDULED').length,
    processing: orders.filter((item) => ['SAMPLE_COLLECTED', 'IN_PROGRESS'].includes(item.status)).length,
    verified: orders.filter((item) => item.status === 'RESULT_VERIFIED').length,
  }), [orders])

  async function schedule(order: DiagnosticOrder) {
    const serviceDate = dates[order.id] ?? tomorrow()
    setWorkingId(order.id)
    setError('')
    setSuccess('')
    try {
      await scheduleDiagnosticOrder(order.id, serviceDate)
      await loadOrders()
      setSuccess(`${order.procedureName} scheduled for ${displayDate(serviceDate)}.`)
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setWorkingId('')
    }
  }

  async function cancel(order: DiagnosticOrder) {
    setWorkingId(order.id)
    setError('')
    setSuccess('')
    try {
      await cancelDiagnosticOrder(order.id)
      await loadOrders()
      setSuccess(`${order.procedureName} was cancelled.`)
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setWorkingId('')
    }
  }

  if (loading) return <div className="grid min-h-[65vh] place-items-center bg-slate-50"><div className="flex items-center gap-3 text-sm font-bold text-slate-500"><LoaderCircle className="size-6 animate-spin text-care-600" />Opening your diagnostic care plan...</div></div>

  return (
    <div className="min-h-screen bg-[#f5f8fc]">
      <section className="overflow-hidden bg-[linear-gradient(120deg,#071b35_0%,#123a69_58%,#087f8c_100%)] text-white">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-11 sm:px-6 lg:grid-cols-[1fr_.72fr] lg:px-8 lg:py-16">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-cyan-200/20 bg-cyan-200/10 px-3 py-2 text-xs font-black uppercase tracking-[.18em] text-cyan-100"><ShieldCheck className="size-4" />Clinician ordered. Staff verified.</div>
            <h1 className="mt-5 text-4xl font-black tracking-tight sm:text-5xl">Tests without the uncertainty</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-blue-100/75 sm:text-base">Track lab, MRI, CT and X-ray orders from scheduling through a verified result. Capacity and queue positions come from the hospital workflow, not estimates invented by the app.</p>
          </div>
          <div className="grid grid-cols-2 gap-3 self-end">
            {[[counts.awaiting, 'To schedule'], [counts.upcoming, 'Upcoming'], [counts.processing, 'Processing'], [counts.verified, 'Verified']].map(([count, label]) => <div key={label} className="rounded-2xl border border-white/10 bg-white/8 p-4 text-center backdrop-blur"><p className="text-3xl font-black">{count}</p><p className="mt-1 text-[11px] font-bold text-blue-100/65">{label}</p></div>)}
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        {error && <div role="alert" className="mb-5 flex gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-800"><TriangleAlert className="size-5 shrink-0" />{error}</div>}
        {success && <div className="mb-5 flex gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-semibold text-emerald-800"><CheckCircle2 className="size-5 shrink-0" />{success}</div>}

        <section>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-black uppercase tracking-[.18em] text-care-700">My diagnostic journey</p><h2 className="mt-1 text-2xl font-black text-ink-950">Orders and verified results</h2></div><Link to="/records" className="text-sm font-black text-care-700 hover:underline">Open complete health record</Link></div>
          {orders.length === 0 ? <div className="mt-5 rounded-[2rem] border border-dashed border-slate-300 bg-white p-9 text-center"><Stethoscope className="mx-auto size-8 text-slate-300" /><h3 className="mt-4 font-black text-ink-950">No diagnostic order yet</h3><p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-slate-500">Tests must be ordered by your treating clinician during a real consultation. SmartCare does not let patients self-prescribe imaging or laboratory investigations.</p><Link to="/booking" className="mt-5 inline-flex rounded-xl bg-care-600 px-4 py-2.5 text-sm font-black text-white">Book a clinician visit</Link></div> : <div className="mt-5 space-y-5">{orders.map((order) => {
            const Icon = iconFor(order.modality)
            const canCancel = order.status === 'ORDERED' || order.status === 'SCHEDULED'
            return <article key={order.id} className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm">
              <div className="grid gap-6 p-5 sm:p-6 lg:grid-cols-[1fr_auto]">
                <div className="flex min-w-0 gap-4"><span className="grid size-13 shrink-0 place-items-center rounded-2xl bg-cyan-50 text-cyan-700"><Icon className="size-6" /></span><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h3 className="text-lg font-black text-ink-950">{order.procedureName}</h3>{order.priority === 'URGENT' && <span className="rounded-full bg-rose-100 px-2.5 py-1 text-[10px] font-black text-rose-800">Urgent</span>}</div><p className="mt-1 text-xs font-bold text-care-700">{modalityLabels[order.modality]} · {order.procedureCode}</p><p className="mt-3 text-sm text-slate-600">Ordered by <strong>{order.orderedByDoctor}</strong> at {order.hospitalName}</p>{order.clinicalNote && <p className="mt-2 text-sm leading-6 text-slate-500">Clinical indication: {order.clinicalNote}</p>}</div></div>
                <span className={`h-fit rounded-full border px-3 py-2 text-[11px] font-black ${statusStyles[order.status]}`}>{statusLabels[order.status]}</span>
              </div>

              <div className="grid gap-3 border-t border-slate-100 bg-slate-50/70 px-5 py-4 text-xs text-slate-600 sm:grid-cols-2 lg:grid-cols-4 sm:px-6">
                <p className="flex gap-2"><CalendarDays className="size-4 shrink-0 text-slate-400" />{order.scheduledDate ? displayDate(order.scheduledDate) : 'Date not selected'}</p>
                <p className="flex gap-2"><CircleDot className="size-4 shrink-0 text-slate-400" />{order.queuePosition ? `Diagnostic queue ${order.queuePosition}` : 'Queue assigned on scheduling'}</p>
                <p className="flex gap-2"><Clock3 className="size-4 shrink-0 text-slate-400" />Expected report: {order.turnaroundHours}h after processing</p>
                <p className="flex gap-2"><MapPin className="size-4 shrink-0 text-slate-400" />{[order.building, order.floorLabel, order.roomNumber].filter(Boolean).join(' · ') || 'Hospital desk confirms location'}</p>
              </div>

              {order.preparationInstructions && <div className="border-t border-amber-100 bg-amber-50/60 px-5 py-3 text-xs leading-5 text-amber-950 sm:px-6"><strong>Preparation:</strong> {order.preparationInstructions}</div>}

              {order.status === 'ORDERED' && <div className="flex flex-col gap-3 border-t border-slate-100 p-5 sm:flex-row sm:items-end sm:px-6"><label className="text-xs font-black text-slate-600">Choose service date<input type="date" min={localDateString()} value={dates[order.id] ?? tomorrow()} onChange={(event) => setDates((current) => ({ ...current, [order.id]: event.target.value }))} className="mt-2 h-11 w-full rounded-xl border border-slate-300 bg-white px-3 sm:w-52" /></label><button disabled={workingId === order.id} onClick={() => void schedule(order)} className="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-care-600 px-5 text-sm font-black text-white disabled:opacity-60">{workingId === order.id && <LoaderCircle className="size-4 animate-spin" />}Reserve capacity</button></div>}

              {order.status === 'SCHEDULED' && <div className="flex flex-wrap items-center gap-3 border-t border-slate-100 p-5 sm:px-6"><Link to={diagnosticNavigationUrl(order)} className="inline-flex h-11 items-center gap-2 rounded-xl bg-care-600 px-4 text-sm font-black text-white"><MapPin className="size-4" />Navigate to test room</Link><p className="text-xs text-slate-500">Arrive with the clinician order and follow the preparation instructions.</p></div>}

              {canCancel && <div className="border-t border-slate-100 px-5 py-3 text-right sm:px-6"><button disabled={workingId === order.id} onClick={() => void cancel(order)} className="inline-flex items-center gap-2 text-xs font-black text-rose-700 hover:underline disabled:opacity-50"><XCircle className="size-4" />Cancel diagnostic order</button></div>}

              {order.result && <div className="border-t border-slate-200 p-5 sm:p-6"><div className={`rounded-2xl border p-4 ${resultStyles[order.result.overallFlag]}`}><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-[10px] font-black uppercase tracking-[.16em]">Staff-verified diagnostic result</p><h4 className="mt-2 text-lg font-black">{order.result.overallFlag.replace('_', ' ')}</h4></div><ShieldCheck className="size-6" /></div><p className="mt-3 text-sm leading-6">{order.result.summary}</p>{order.result.impression && <p className="mt-2 text-sm leading-6"><strong>Impression:</strong> {order.result.impression}</p>}<p className="mt-3 text-[11px] font-bold opacity-70">Verified by {order.result.verifiedBy} · {displayDateTime(order.result.verifiedAt)}</p></div>
                {order.result.items.length > 0 && <div className="mt-4 overflow-x-auto rounded-2xl border border-slate-200"><table className="min-w-full text-left text-sm"><thead className="bg-slate-50 text-xs text-slate-500"><tr><th className="px-4 py-3">Test</th><th className="px-4 py-3">Result</th><th className="px-4 py-3">Reference</th><th className="px-4 py-3">Flag</th></tr></thead><tbody className="divide-y divide-slate-100">{order.result.items.map((item) => <tr key={`${item.name}-${item.value}`}><td className="px-4 py-3 font-bold text-ink-950">{item.name}</td><td className="px-4 py-3">{item.value}{item.unit ? ` ${item.unit}` : ''}</td><td className="px-4 py-3 text-slate-500">{item.referenceRange ?? 'Not supplied'}</td><td className="px-4 py-3 font-black">{item.flag}</td></tr>)}</tbody></table></div>}
                <div className="mt-4 flex gap-3 rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-600"><HeartPulse className="size-5 shrink-0 text-care-700" /><p>Results are displayed exactly as verified by diagnostic staff. Discuss meaning and next steps with your qualified clinician; SmartCare does not diagnose from these values.</p></div>
              </div>}
            </article>
          })}</div>}
        </section>

        <section className="mt-12 border-t border-slate-200 pt-10">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between"><div><p className="text-xs font-black uppercase tracking-[.18em] text-cyan-700">Hospital catalogue</p><h2 className="mt-1 text-2xl font-black text-ink-950">Diagnostic services and preparation</h2><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">Browse only. A treating clinician must create the order before capacity can be reserved.</p></div><div className="grid gap-3 sm:grid-cols-2"><select value={hospitalId} onChange={(event) => setHospitalId(event.target.value)} className="h-11 min-w-52 rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold"><option value="">Choose hospital</option>{hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}</select><select value={modality} onChange={(event) => setModality(event.target.value as DiagnosticModality | '')} className="h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold"><option value="">All services</option>{Object.entries(modalityLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div></div>
          {catalogLoading ? <div className="mt-6 flex min-h-32 items-center justify-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin" />Loading verified hospital catalogue...</div> : procedures.length === 0 ? <div className="mt-6 rounded-3xl border border-dashed border-slate-300 bg-white p-7 text-center text-sm text-slate-500">No active diagnostic service is published for this selection.</div> : <div className="mt-6 grid gap-4 md:grid-cols-2 lg:grid-cols-3">{procedures.map((procedure) => {
            const Icon = iconFor(procedure.modality)
            return <article key={procedure.id} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-cyan-300 hover:shadow-md">
              <div className="flex items-start justify-between gap-3"><span className="grid size-11 place-items-center rounded-2xl bg-cyan-50 text-cyan-700"><Icon className="size-5" /></span><span className="rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-black text-slate-600">{modalityLabels[procedure.modality]}</span></div>
              <h3 className="mt-4 font-black text-ink-950">{procedure.name}</h3><p className="mt-1 text-xs font-bold text-care-700">{procedure.code} · {procedure.turnaroundHours}h turnaround</p>
              <div className="mt-4 space-y-2 text-xs text-slate-500"><p className="flex gap-2"><MapPin className="size-4 shrink-0" />{[procedure.building, procedure.floorLabel, procedure.roomNumber].filter(Boolean).join(' · ') || procedure.hospitalName}</p><p className="flex gap-2"><IndianRupee className="size-4 shrink-0" />Published fee {procedure.fee.toLocaleString('en-IN')}</p></div>
              {procedure.preparationInstructions && <p className="mt-4 line-clamp-2 rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-950">{procedure.preparationInstructions}</p>}
              <button onClick={() => setSelectedProcedure(procedure)} className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl border border-cyan-200 bg-cyan-50 px-4 py-2.5 text-xs font-black text-cyan-900"><BookOpen className="size-4" />What happens in this test?</button>
            </article>
          })}</div>}
        </section>

        <section className="mt-10 grid gap-4 sm:grid-cols-3"><div className="rounded-2xl border border-slate-200 bg-white p-4"><Activity className="size-5 text-cyan-700" /><h3 className="mt-3 text-sm font-black text-ink-950">No invented status</h3><p className="mt-1 text-xs leading-5 text-slate-500">Every transition is recorded by authorized hospital staff.</p></div><div className="rounded-2xl border border-slate-200 bg-white p-4"><ShieldCheck className="size-5 text-emerald-700" /><h3 className="mt-3 text-sm font-black text-ink-950">Verified before release</h3><p className="mt-1 text-xs leading-5 text-slate-500">Draft or unverified findings are not shown as final patient results.</p></div><div className="rounded-2xl border border-slate-200 bg-white p-4"><HeartPulse className="size-5 text-rose-700" /><h3 className="mt-3 text-sm font-black text-ink-950">Clinical interpretation stays human</h3><p className="mt-1 text-xs leading-5 text-slate-500">A qualified clinician explains significance and treatment decisions.</p></div></section>
      </main>
      {selectedProcedure && (() => {
        const guide = procedureGuide(selectedProcedure)
        const Icon = iconFor(selectedProcedure.modality)
        return <div className="fixed inset-0 z-[70] grid place-items-center bg-ink-950/70 p-4 backdrop-blur-sm" role="dialog" aria-modal="true" aria-labelledby="test-guide-title"><div className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-[2rem] bg-white shadow-2xl"><div className="flex items-start justify-between gap-4 bg-[linear-gradient(120deg,#071b35,#087f8c)] p-6 text-white sm:p-8"><div className="flex gap-4"><span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-white/10"><Icon className="size-6" /></span><div><p className="text-xs font-black uppercase tracking-wider text-cyan-200">Plain-language test guide</p><h2 id="test-guide-title" className="mt-2 text-2xl font-black">{selectedProcedure.name}</h2><p className="mt-1 text-sm text-blue-100/70">{modalityLabels[selectedProcedure.modality]} · about {selectedProcedure.estimatedDurationMinutes} minutes</p></div></div><button onClick={() => setSelectedProcedure(null)} className="rounded-xl border border-white/15 px-3 py-2 text-xs font-black">Close</button></div><div className="grid gap-5 p-6 sm:p-8"><GuideBlock number="1" title="What is it?" body={guide.what} /><GuideBlock number="2" title="What happens on the day?" body={guide.happens} /><GuideBlock number="3" title="How should I prepare?" body={selectedProcedure.preparationInstructions || 'The collection or imaging desk will confirm preparation from your clinician order.'} /><div className="grid gap-3 rounded-2xl bg-slate-50 p-4 text-sm sm:grid-cols-3"><div><p className="text-xs text-slate-500">Published fee</p><p className="mt-1 font-black">₹{selectedProcedure.fee.toLocaleString('en-IN')}</p></div><div><p className="text-xs text-slate-500">Expected report</p><p className="mt-1 font-black">{selectedProcedure.turnaroundHours} hours</p></div><div><p className="text-xs text-slate-500">Location</p><p className="mt-1 font-black">{selectedProcedure.roomNumber ?? selectedProcedure.hospitalName}</p></div></div><div className="flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs leading-5 text-amber-950"><TriangleAlert className="mt-0.5 size-5 shrink-0" /><p>This guide explains workflow only. The ordering clinician and diagnostic team decide whether a test is appropriate and interpret the result.</p></div></div></div></div>
      })()}
    </div>
  )
}

function GuideBlock({ number, title, body }: { number: string; title: string; body: string }) {
  return <div className="grid grid-cols-[2.5rem_1fr] gap-3"><span className="grid size-10 place-items-center rounded-xl bg-cyan-50 text-sm font-black text-cyan-800">{number}</span><div><h3 className="font-black text-ink-950">{title}</h3><p className="mt-1 text-sm leading-6 text-slate-600">{body}</p></div></div>
}
