import { Ambulance, BadgeCheck, ClipboardList, Droplets, FlaskConical, LoaderCircle, Phone, RefreshCw, ShieldAlert } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api, messageFromError } from '../services/api'
import { getAmbulanceWorklist, type AmbulanceRequest } from '../services/ambulances'
import { getVerifiedDonorMatches, type BloodRequest, type DonorMatch } from '../services/bloodBank'
import { verifyDiagnosticResult, type DiagnosticOrder, type DiagnosticResultFlag } from '../services/diagnostics'
import { getNavigationHospitals } from '../services/navigation'

const operationalRoles = ['AMBULANCE_DISPATCHER', 'BLOOD_BANK_STAFF', 'LAB_TECHNICIAN', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']
const today = () => new Date().toISOString().slice(0, 10)

export function StaffTaskInboxPage() {
  const { session } = useAuth()
  const roles = session?.user.roles ?? []
  const isOperational = roles.some((role) => operationalRoles.includes(role))
  const seesAmbulance = roles.some((role) => ['AMBULANCE_DISPATCHER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role))
  const seesBlood = roles.some((role) => ['BLOOD_BANK_STAFF', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role))
  const seesLab = roles.some((role) => ['LAB_TECHNICIAN', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role))
  const [hospitalId, setHospitalId] = useState('')
  const [ambulanceTasks, setAmbulanceTasks] = useState<AmbulanceRequest[]>([])
  const [bloodTasks, setBloodTasks] = useState<BloodRequest[]>([])
  const [labTasks, setLabTasks] = useState<DiagnosticOrder[]>([])
  const [donorMatches, setDonorMatches] = useState<Record<string, DonorMatch[]>>({})
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState('')
  const [error, setError] = useState('')
  const [resultOrderId, setResultOrderId] = useState('')
  const [resultSummary, setResultSummary] = useState('')
  const [resultFlag, setResultFlag] = useState<DiagnosticResultFlag>('NORMAL')

  const refresh = useCallback(async (id: string) => {
    if (!id || !isOperational) return
    setError('')
    try {
      const [ambulanceData, bloodData, labData] = await Promise.all([
        seesAmbulance ? getAmbulanceWorklist(id) : Promise.resolve([]),
        seesBlood ? api.get<BloodRequest[]>('/api/v1/blood-requests', { params: { hospitalId: id } }).then((response) => response.data) : Promise.resolve([]),
        seesLab ? api.get<DiagnosticOrder[]>('/api/v1/diagnostics/worklist', { params: { hospitalId: id, date: today() } }).then((response) => response.data) : Promise.resolve([]),
      ])
      setAmbulanceTasks(ambulanceData.filter((item) => !['COMPLETED', 'CANCELLED'].includes(item.status)))
      setBloodTasks(bloodData.filter((item) => !['FULFILLED', 'CANCELLED'].includes(item.status)))
      setLabTasks(labData.filter((item) => item.status !== 'RESULT_VERIFIED' && item.status !== 'CANCELLED'))
    } catch (requestError) { setError(messageFromError(requestError)) }
  }, [isOperational, seesAmbulance, seesBlood, seesLab])

  useEffect(() => {
    getNavigationHospitals().then((hospitals) => {
      const id = hospitals[0]?.id ?? ''
      setHospitalId(id)
      return refresh(id)
    }).catch((requestError) => setError(messageFromError(requestError))).finally(() => setLoading(false))
  }, [refresh])

  useEffect(() => {
    if (!hospitalId || !isOperational) return
    const timer = window.setInterval(() => void refresh(hospitalId), 15_000)
    return () => window.clearInterval(timer)
  }, [hospitalId, isOperational, refresh])

  const total = ambulanceTasks.length + bloodTasks.length + labTasks.length
  const urgent = useMemo(() => ambulanceTasks.filter((item) => item.priority !== 'SCHEDULED').length + bloodTasks.filter((item) => item.urgency !== 'ROUTINE').length, [ambulanceTasks, bloodTasks])

  async function act(key: string, request: Promise<unknown>) {
    setWorking(key); setError('')
    try { await request; await refresh(hospitalId) } catch (requestError) { setError(messageFromError(requestError)) } finally { setWorking('') }
  }

  async function loadDonors(task: BloodRequest) {
    const key = `donors:${task.id}`
    setWorking(key); setError('')
    try {
      const matches = await getVerifiedDonorMatches(task.id)
      setDonorMatches((current) => ({ ...current, [task.id]: matches }))
    }
    catch (requestError) { setError(messageFromError(requestError)) }
    finally { setWorking('') }
  }

  async function submitVerifiedResult(task: DiagnosticOrder) {
    if (!resultSummary.trim()) {
      setError('A staff-authored result summary is required before verification.')
      return
    }
    const key = `verify:${task.id}`
    setWorking(key); setError('')
    try {
      await verifyDiagnosticResult(task.id, {
        summary: resultSummary.trim(), overallFlag: resultFlag, items: [],
      })
      setResultOrderId(''); setResultSummary(''); setResultFlag('NORMAL')
      await refresh(hospitalId)
    } catch (requestError) { setError(messageFromError(requestError)) }
    finally { setWorking('') }
  }

  if (loading) return <div className="grid min-h-[65vh] place-items-center bg-slate-50"><LoaderCircle className="size-7 animate-spin text-care-600" /></div>
  if (!isOperational) return <div className="mx-auto max-w-3xl px-4 py-16"><div className="rounded-3xl border border-amber-200 bg-amber-50 p-8 text-center"><ShieldAlert className="mx-auto size-9 text-amber-700" /><h1 className="mt-4 text-2xl font-black text-amber-950">Staff task inbox</h1><p className="mt-2 text-sm leading-6 text-amber-900">This workspace is visible only to ambulance dispatchers, blood-bank staff, lab technicians and hospital administrators.</p></div></div>

  return <div className="min-h-screen bg-[#f5f8fc]"><section className="bg-ink-950 text-white"><div className="mx-auto flex max-w-7xl flex-col gap-6 px-4 py-10 sm:px-6 lg:flex-row lg:items-end lg:justify-between lg:px-8"><div><p className="text-xs font-black uppercase tracking-[.18em] text-blue-300">Role-aware operations</p><h1 className="mt-2 text-4xl font-black">My assigned work</h1><p className="mt-3 max-w-2xl text-sm leading-6 text-blue-100/70">Requests become clear action cards with a next step. The inbox refreshes every 15 seconds.</p></div><div className="flex gap-3"><Stat value={total} label="Open tasks" /><Stat value={urgent} label="Urgent" /><button onClick={() => void refresh(hospitalId)} className="grid size-16 place-items-center rounded-2xl border border-white/10 bg-white/10" aria-label="Refresh tasks"><RefreshCw className="size-5" /></button></div></div></section><main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">{error && <p role="alert" className="mb-5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-bold text-rose-800">{error}</p>}{total === 0 && <div className="rounded-3xl border border-dashed border-slate-300 bg-white p-10 text-center"><BadgeCheck className="mx-auto size-10 text-emerald-500" /><h2 className="mt-3 font-black text-ink-950">No open task right now</h2><p className="mt-1 text-sm text-slate-500">New assignments will appear automatically.</p></div>}
    <div className="grid gap-6 xl:grid-cols-3">
      {seesAmbulance && <TaskColumn title="Ambulance dispatch" icon={Ambulance} count={ambulanceTasks.length}>{ambulanceTasks.map((task) => <article key={task.id} className="rounded-2xl border border-slate-200 bg-white p-4"><p className="text-[10px] font-black uppercase tracking-wider text-red-600">{task.priority} · {task.status.replaceAll('_', ' ')}</p><h3 className="mt-2 font-black text-ink-950">{task.pickupAddress}</h3><p className="mt-2 text-xs leading-5 text-slate-600"><strong>Next:</strong> {task.status === 'REQUESTED' ? 'Call if needed and assign an available vehicle.' : 'Record the next dispatcher-confirmed handoff.'}</p><div className="mt-4 flex gap-2"><a href={`tel:${task.contactNumber}`} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-care-200 px-3 text-xs font-black text-care-800"><Phone className="size-4" />Call</a><Link to="/ambulance" className="inline-flex min-h-10 flex-1 items-center justify-center rounded-xl bg-care-700 px-3 text-xs font-black text-white">Open dispatch</Link></div></article>)}</TaskColumn>}
      {seesBlood && <TaskColumn title="Blood bank" icon={Droplets} count={bloodTasks.length}>{bloodTasks.map((task) => <article key={task.id} className="rounded-2xl border border-slate-200 bg-white p-4"><p className="text-[10px] font-black uppercase tracking-wider text-rose-600">{task.urgency} · {task.status.replaceAll('_', ' ')}</p><h3 className="mt-2 font-black text-ink-950">{task.bloodGroup.replace('_', ' ')} · {task.component.replaceAll('_', ' ')}</h3><p className="mt-2 text-xs text-slate-600">{task.matchedUnits}/{task.requestedUnits} units matched</p><button disabled={working === `blood:${task.id}`} onClick={() => void act(`blood:${task.id}`, api.post(`/api/v1/blood-requests/${task.id}/${task.status === 'RESERVED' ? 'fulfil' : 'search'}`))} className="mt-4 min-h-10 w-full rounded-xl bg-rose-700 px-3 text-xs font-black text-white disabled:opacity-50">{task.status === 'RESERVED' ? 'Record fulfilment' : 'Run verified match'}</button>{['UNAVAILABLE', 'PARTIALLY_RESERVED'].includes(task.status) && <button disabled={working === `donors:${task.id}`} onClick={() => void loadDonors(task)} className="mt-2 min-h-10 w-full rounded-xl border border-rose-200 bg-rose-50 px-3 text-xs font-black text-rose-800 disabled:opacity-50">Check verified consenting donors</button>}{donorMatches[task.id] && <div className="mt-3 space-y-2 rounded-xl border border-rose-100 bg-rose-50/60 p-3"><p className="text-[10px] font-black uppercase tracking-wider text-rose-700">Private staff-only matches</p>{donorMatches[task.id].length === 0 ? <p className="text-xs text-slate-600">No eligible consenting donor is currently available.</p> : donorMatches[task.id].map((donor) => <div key={donor.donorOptInId} className="rounded-lg bg-white p-2 text-xs"><p className="font-black text-ink-950">{donor.displayName} · {donor.verifiedBloodGroup.replace('_', ' ')}</p><p className="mt-1 text-slate-500">Consent preference: {donor.contactPreference}</p><a href={`tel:${donor.mobileNumber}`} className="mt-2 inline-flex items-center gap-1 font-black text-rose-700"><Phone className="size-3.5" />Contact donor</a></div>)}</div>}</article>)}</TaskColumn>}
      {seesLab && <TaskColumn title="Diagnostic lab" icon={FlaskConical} count={labTasks.length}>{labTasks.map((task) => <article key={task.id} className="rounded-2xl border border-slate-200 bg-white p-4"><p className="text-[10px] font-black uppercase tracking-wider text-cyan-700">{task.priority} · {task.status.replaceAll('_', ' ')}</p><h3 className="mt-2 font-black text-ink-950">{task.procedureName}</h3><p className="mt-2 text-xs text-slate-600">Patient {task.patientNumber} · queue {task.queuePosition ?? 'not assigned'}</p>{task.status === 'SCHEDULED' && <button disabled={working === `lab:${task.id}`} onClick={() => void act(`lab:${task.id}`, api.post(`/api/v1/diagnostics/orders/${task.id}/collect`))} className="mt-4 min-h-10 w-full rounded-xl bg-cyan-700 px-3 text-xs font-black text-white">Record sample collection</button>}{task.status === 'SAMPLE_COLLECTED' && <button disabled={working === `lab:${task.id}`} onClick={() => void act(`lab:${task.id}`, api.post(`/api/v1/diagnostics/orders/${task.id}/start`))} className="mt-4 min-h-10 w-full rounded-xl bg-cyan-700 px-3 text-xs font-black text-white">Start processing</button>}{task.status === 'IN_PROGRESS' && (resultOrderId === task.id ? <div className="mt-4 space-y-2 rounded-xl border border-cyan-200 bg-cyan-50 p-3"><label className="block text-xs font-black text-cyan-950">Authorized result summary<textarea value={resultSummary} onChange={(event) => setResultSummary(event.target.value)} maxLength={2000} rows={4} className="mt-1 w-full rounded-lg border border-cyan-200 bg-white p-2 font-normal" placeholder="Enter the verified laboratory or imaging summary" /></label><label className="block text-xs font-black text-cyan-950">Overall flag<select value={resultFlag} onChange={(event) => setResultFlag(event.target.value as DiagnosticResultFlag)} className="mt-1 h-10 w-full rounded-lg border border-cyan-200 bg-white px-2 font-normal"><option value="NORMAL">Normal</option><option value="ABNORMAL">Abnormal</option><option value="CRITICAL">Critical</option><option value="INDETERMINATE">Indeterminate</option></select></label><p className="text-[11px] leading-4 text-cyan-900">Verification releases this result to the patient. Enter only authorized findings; clinical interpretation remains with the treating clinician.</p><div className="flex gap-2"><button onClick={() => setResultOrderId('')} className="min-h-10 flex-1 rounded-lg border border-cyan-200 bg-white text-xs font-black text-cyan-900">Cancel</button><button disabled={working === `verify:${task.id}`} onClick={() => void submitVerifiedResult(task)} className="min-h-10 flex-1 rounded-lg bg-cyan-800 text-xs font-black text-white disabled:opacity-50">Verify & release</button></div></div> : <button onClick={() => { setResultOrderId(task.id); setResultSummary(''); setResultFlag('NORMAL') }} className="mt-4 min-h-10 w-full rounded-xl bg-cyan-800 px-3 text-xs font-black text-white">Enter verified result</button>)}</article>)}</TaskColumn>}
    </div>
  </main></div>
}

function Stat({ value, label }: { value: number; label: string }) { return <div className="min-w-20 rounded-2xl border border-white/10 bg-white/10 p-3 text-center"><p className="text-2xl font-black">{value}</p><p className="text-[10px] font-bold text-blue-100/60">{label}</p></div> }
function TaskColumn({ title, icon: Icon, count, children }: { title: string; icon: typeof ClipboardList; count: number; children: React.ReactNode }) { return <section><div className="mb-4 flex items-center gap-3"><span className="grid size-10 place-items-center rounded-xl bg-care-50 text-care-700"><Icon className="size-5" /></span><h2 className="font-black text-ink-950">{title}</h2><span className="ml-auto rounded-full bg-white px-3 py-1 text-xs font-black text-slate-600">{count}</span></div><div className="space-y-3">{children}</div></section> }
