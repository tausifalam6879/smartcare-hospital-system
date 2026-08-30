import {
  Activity, AlertTriangle, Ambulance, BellRing, CalendarClock, CheckCircle2, Clock3,
  Droplets, FlaskConical, LoaderCircle, RefreshCw, ShieldCheck, Stethoscope, Users,
} from 'lucide-react'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api, messageFromError } from '../services/api'
import { appointmentStatusLabel } from '../services/appointments'
import {
  getMyRecoveryCases, getOperationsDashboard, markAppointmentNoShow, resolveRecovery,
  updateDoctorDayStatus, type DoctorDayStatus, type OperationsDashboard, type RecoveryCase,
  type RecoveryChoice,
} from '../services/operations'

type Hospital = { id: string; name: string; city: string }
type Doctor = { id: string; name: string; specialization: string; departmentId?: string }
type Page<T> = { content: T[] }

const staffRoles = ['DOCTOR', 'RECEPTIONIST', 'CASHIER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']
const managingRoles = ['DOCTOR', 'RECEPTIONIST', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']

const statusLabels: Record<DoctorDayStatus, string> = {
  ON_TIME: 'On time',
  DELAYED_30: 'About 30 min delayed',
  DELAYED_60: 'About 60 min delayed',
  EMERGENCY_INTERRUPTION: 'Emergency interruption',
  TEMPORARILY_UNAVAILABLE: 'Temporarily unavailable',
  CANCELLED_FOR_DAY: 'Cancelled for today',
}

const choiceLabels: Record<RecoveryChoice, string> = {
  RESCHEDULE_SAME_DOCTOR: 'Same doctor, another date',
  MOVE_TO_ELIGIBLE_DOCTOR: 'Another eligible doctor',
  PRIORITY_FUTURE_QUEUE: 'Priority future queue',
  REFUND_REVIEW: 'Cancel and request refund review',
}

function localDate(offsetDays = 0) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function RecoveryCard({ item, onResolved }: { item: RecoveryCase; onResolved: (next: RecoveryCase) => void }) {
  const [choice, setChoice] = useState<RecoveryChoice>('RESCHEDULE_SAME_DOCTOR')
  const [targetDate, setTargetDate] = useState(localDate(1))
  const [eligibleDoctors, setEligibleDoctors] = useState<Doctor[]>([])
  const [targetDoctorId, setTargetDoctorId] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const pending = item.status === 'AWAITING_PATIENT_CHOICE'

  useEffect(() => {
    if (choice !== 'MOVE_TO_ELIGIBLE_DOCTOR') return
    api.get<Page<Doctor>>('/api/v1/doctors', {
      params: { hospitalId: item.hospitalId, departmentId: item.departmentId, size: 100 },
    }).then(({ data }) => {
      const eligible = data.content.filter((doctor) => doctor.id !== item.originalDoctorId)
      setEligibleDoctors(eligible)
      setTargetDoctorId((current) => eligible.some((doctor) => doctor.id === current)
        ? current : (eligible[0]?.id ?? ''))
    }).catch((requestError) => setError(messageFromError(requestError)))
  }, [choice, item.departmentId, item.hospitalId, item.originalDoctorId])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (choice === 'REFUND_REVIEW' && !window.confirm('Cancel this booking and send it for refund review? A refund is not guaranteed until staff review it.')) return
    setBusy(true)
    setError('')
    try {
      const needsDate = choice !== 'REFUND_REVIEW'
      const next = await resolveRecovery(item.id, {
        choice,
        ...(needsDate ? { targetDate } : {}),
        ...(choice === 'MOVE_TO_ELIGIBLE_DOCTOR' ? { targetDoctorId } : {}),
      })
      onResolved(next)
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <article className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div><p className="text-xs font-extrabold uppercase tracking-wider text-amber-700">Care disruption</p><h2 className="mt-1 text-lg font-black text-ink-950">{item.originalDoctorName}</h2><p className="mt-1 text-sm text-slate-500">{item.hospitalName} · {item.originalDate}{item.originalQueuePosition ? ` · OPD ${item.originalQueuePosition}` : ''}</p></div>
        <span className={`rounded-full px-3 py-1.5 text-xs font-extrabold ${pending ? 'bg-amber-100 text-amber-900' : 'bg-emerald-100 text-emerald-900'}`}>{pending ? 'Your decision needed' : item.status === 'RESCHEDULED' ? 'Rescheduled' : 'Refund review requested'}</span>
      </div>
      <p className="mt-4 rounded-2xl bg-amber-50 p-4 text-sm leading-6 text-amber-950">{item.interruptionReason || 'The doctor is unavailable for this visit.'}</p>
      {pending ? (
        <form onSubmit={submit} className="mt-5 space-y-4">
          <p className="flex gap-2 text-sm font-bold text-slate-700"><ShieldCheck className="mt-0.5 size-4 shrink-0 text-care-700" />RaahMediQ Health will not move your doctor or date without your approval.</p>
          <label className="block text-xs font-bold text-slate-600">Choose what happens next
            <select value={choice} onChange={(event) => setChoice(event.target.value as RecoveryChoice)} className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-ink-950">
              <option value="RESCHEDULE_SAME_DOCTOR">{choiceLabels.RESCHEDULE_SAME_DOCTOR}</option>
              <option value="MOVE_TO_ELIGIBLE_DOCTOR">{choiceLabels.MOVE_TO_ELIGIBLE_DOCTOR}</option>
              <option value="PRIORITY_FUTURE_QUEUE">{choiceLabels.PRIORITY_FUTURE_QUEUE}</option>
              <option value="REFUND_REVIEW">{choiceLabels.REFUND_REVIEW}</option>
            </select>
          </label>
          {choice === 'MOVE_TO_ELIGIBLE_DOCTOR' && <label className="block text-xs font-bold text-slate-600">Eligible {item.departmentName} doctor
            <select value={targetDoctorId} onChange={(event) => setTargetDoctorId(event.target.value)} required className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-ink-950">
              <option value="">Select a doctor</option>
              {eligibleDoctors.map((doctor) => <option key={doctor.id} value={doctor.id}>{doctor.name} · {doctor.specialization}</option>)}
            </select>
            {eligibleDoctors.length === 0 && <span className="mt-1 block text-xs font-medium text-amber-700">No other active doctor is currently listed in this department.</span>}
          </label>}
          {choice !== 'REFUND_REVIEW' && <label className="block text-xs font-bold text-slate-600">Preferred future date
            <input type="date" min={localDate(1)} value={targetDate} onChange={(event) => setTargetDate(event.target.value)} required className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 px-3 text-sm" />
          </label>}
          {choice === 'REFUND_REVIEW' && <p className="rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-600">This cancels the appointment and opens a staff review. It does not claim that money has already been refunded.</p>}
          {error && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm font-semibold text-rose-800">{error}</p>}
          <button disabled={busy} className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-care-600 text-sm font-extrabold text-white disabled:opacity-60">{busy && <LoaderCircle className="size-4 animate-spin" />}Confirm my choice</button>
        </form>
      ) : <p className="mt-4 flex gap-2 text-sm font-bold text-emerald-800"><CheckCircle2 className="size-5 shrink-0" />{item.patientChoice ? choiceLabels[item.patientChoice] : 'Your decision is recorded'}{item.targetDate ? ` for ${item.targetDate}` : ''}.</p>}
    </article>
  )
}

function PatientRecovery() {
  const [cases, setCases] = useState<RecoveryCase[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    getMyRecoveryCases().then(setCases).catch((requestError) => setError(messageFromError(requestError))).finally(() => setLoading(false))
  }, [])
  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <p className="text-xs font-extrabold uppercase tracking-[.2em] text-care-700">Appointment recovery</p>
      <h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">You stay in control when plans change</h1>
      <p className="mt-3 max-w-3xl leading-7 text-slate-600">Review doctor cancellations and approve the next step yourself. Capacity is checked again before a new OPD number is issued.</p>
      {loading && <div className="mt-8 flex items-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin" />Loading recovery options…</div>}
      {error && <p role="alert" className="mt-8 rounded-2xl bg-rose-50 p-4 text-sm font-semibold text-rose-800">{error}</p>}
      {!loading && !error && cases.length === 0 && <div className="mt-8 rounded-3xl border border-slate-200 bg-white p-8 text-center"><CheckCircle2 className="mx-auto size-10 text-emerald-600" /><h2 className="mt-3 font-black text-ink-950">No appointment disruption needs your decision</h2><p className="mt-1 text-sm text-slate-500">If a doctor becomes unavailable, the choices will appear here and in notifications.</p></div>}
      <div className="mt-8 grid gap-5 md:grid-cols-2">{cases.map((item) => <RecoveryCard key={item.id} item={item} onResolved={(next) => setCases((all) => all.map((entry) => entry.id === next.id ? next : entry))} />)}</div>
    </div>
  )
}

function Metric({ label, value, icon: Icon, tone = 'blue' }: { label: string; value: number; icon: typeof Activity; tone?: 'blue' | 'amber' | 'red' | 'emerald' }) {
  const colors = { blue: 'bg-blue-50 text-blue-700', amber: 'bg-amber-50 text-amber-700', red: 'bg-rose-50 text-rose-700', emerald: 'bg-emerald-50 text-emerald-700' }
  return <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><span className={`grid size-9 place-items-center rounded-xl ${colors[tone]}`}><Icon className="size-4" /></span><p className="mt-4 text-2xl font-black text-ink-950">{value}</p><p className="text-xs font-bold text-slate-500">{label}</p></div>
}

function StaffOperations({ canManage }: { canManage: boolean }) {
  const [hospitals, setHospitals] = useState<Hospital[]>([])
  const [hospitalId, setHospitalId] = useState('')
  const [date, setDate] = useState(localDate())
  const [dashboard, setDashboard] = useState<OperationsDashboard | null>(null)
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [doctorId, setDoctorId] = useState('')
  const [status, setStatus] = useState<DoctorDayStatus>('ON_TIME')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get<Hospital[]>('/api/v1/hospitals').then(({ data }) => {
      setHospitals(data)
      if (data[0]) setHospitalId(data[0].id)
    }).catch((requestError) => setError(messageFromError(requestError)))
  }, [])

  useEffect(() => {
    if (!hospitalId) return
    api.get<Page<Doctor>>('/api/v1/doctors', { params: { hospitalId, size: 100 } }).then(({ data }) => {
      setDoctors(data.content)
      setDoctorId((current) => data.content.some((doctor) => doctor.id === current) ? current : (data.content[0]?.id ?? ''))
    }).catch(() => setDoctors([]))
  }, [hospitalId])

  async function refresh() {
    if (!hospitalId) return
    setLoading(true)
    setError('')
    try { setDashboard(await getOperationsDashboard(hospitalId, date)) }
    catch (requestError) { setError(messageFromError(requestError)); setDashboard(null) }
    finally { setLoading(false) }
  }

  useEffect(() => { void refresh() }, [hospitalId, date])

  async function saveStatus(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await updateDoctorDayStatus({ doctorId, serviceDate: date, status, reason: reason.trim() || undefined })
      setReason('')
      await refresh()
    } catch (requestError) { setError(messageFromError(requestError)) }
    finally { setSaving(false) }
  }

  async function noShow(appointmentId: string) {
    if (!window.confirm('Mark this confirmed patient as no-show and release the occupied queue capacity?')) return
    setSaving(true)
    setError('')
    try { await markAppointmentNoShow(appointmentId); await refresh() }
    catch (requestError) { setError(messageFromError(requestError)) }
    finally { setSaving(false) }
  }

  const metricData = useMemo(() => dashboard ? [
    ['Today appointments', dashboard.totalAppointments, CalendarClock, 'blue'],
    ['Checked in', dashboard.checkedIn, Users, 'emerald'],
    ['In consultation', dashboard.inConsultation, Stethoscope, 'blue'],
    ['Waitlisted', dashboard.waitlisted, Clock3, 'amber'],
    ['No-shows', dashboard.noShows, AlertTriangle, 'red'],
    ['Pending payments', dashboard.pendingPayments, Activity, 'amber'],
    ['Diagnostic load', dashboard.diagnosticLoad, FlaskConical, 'blue'],
    ['Blood alerts', dashboard.bloodInventoryAlerts, Droplets, 'red'],
    ['Ambulances available', dashboard.ambulancesAvailable, Ambulance, 'emerald'],
    ['Notification failures', dashboard.globalNotificationFailures ?? 0, BellRing, 'red'],
  ] as const : [], [dashboard])

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"><div><p className="text-xs font-extrabold uppercase tracking-[.2em] text-care-700">Hospital operations</p><h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">Today’s care flow</h1><p className="mt-2 text-slate-600">Queues, disruptions and clinical support load in one role-aware view.</p></div><div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-bold text-slate-600">Hospital<select value={hospitalId} onChange={(event) => setHospitalId(event.target.value)} className="mt-1.5 h-11 w-full min-w-52 rounded-xl border border-slate-300 bg-white px-3 text-sm">{hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}</select></label><label className="text-xs font-bold text-slate-600">Service date<input type="date" value={date} onChange={(event) => setDate(event.target.value)} className="mt-1.5 h-11 w-full rounded-xl border border-slate-300 px-3 text-sm" /></label></div></div>
      {error && <p role="alert" className="mt-6 rounded-2xl bg-rose-50 p-4 text-sm font-semibold text-rose-800">{error}</p>}
      {loading && <div className="mt-8 flex items-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin" />Refreshing operational view…</div>}
      {dashboard && <>
        <div className="mt-8 grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-5">{metricData.map(([label, value, Icon, tone]) => <Metric key={label} label={label} value={value} icon={Icon} tone={tone} />)}</div>
        <div className="mt-4 flex flex-wrap gap-3 rounded-2xl bg-ink-950 p-4 text-sm font-bold text-white"><span>{dashboard.activeDoctors} active doctors</span><span className="text-slate-500">•</span><span>{dashboard.delayedOrUnavailableDoctors} delayed/unavailable</span><span className="text-slate-500">•</span><span>Average recorded wait {dashboard.averageRecordedWaitMinutes} min</span></div>

        {canManage && <form onSubmit={saveStatus} className="mt-8 grid gap-4 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm lg:grid-cols-[1.2fr_1fr_1.5fr_auto]"><label className="text-xs font-bold text-slate-600">Doctor<select value={doctorId} onChange={(event) => setDoctorId(event.target.value)} required className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm">{doctors.map((doctor) => <option key={doctor.id} value={doctor.id}>{doctor.name} · {doctor.specialization}</option>)}</select></label><label className="text-xs font-bold text-slate-600">Operational status<select value={status} onChange={(event) => setStatus(event.target.value as DoctorDayStatus)} className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm">{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><label className="text-xs font-bold text-slate-600">Reason / patient guidance<input value={reason} onChange={(event) => setReason(event.target.value)} required={status !== 'ON_TIME'} maxLength={300} placeholder={status === 'ON_TIME' ? 'Optional' : 'Required for affected patients'} className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 px-3 text-sm" /></label><button disabled={saving || !doctorId} className="flex h-12 items-center justify-center gap-2 self-end rounded-xl bg-care-600 px-5 text-sm font-extrabold text-white disabled:opacity-60">{saving ? <LoaderCircle className="size-4 animate-spin" /> : <RefreshCw className="size-4" />}Update</button><p className="text-xs leading-5 text-slate-500 lg:col-span-4">“Cancelled for today” creates a patient recovery case. It never silently transfers the appointment.</p></form>}

        <section className="mt-8 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm"><div className="flex items-center justify-between border-b border-slate-100 p-5"><div><p className="text-xs font-extrabold uppercase tracking-wider text-care-700">Live worklist</p><h2 className="mt-1 text-xl font-black text-ink-950">Patient queue</h2></div><span className="rounded-full bg-slate-100 px-3 py-1.5 text-xs font-bold text-slate-600">{dashboard.queue.length} records</span></div><div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left text-sm"><thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500"><tr><th className="px-5 py-3">OPD</th><th className="px-5 py-3">Patient</th><th className="px-5 py-3">Doctor</th><th className="px-5 py-3">Status</th><th className="px-5 py-3">Payment</th>{canManage && <th className="px-5 py-3">Action</th>}</tr></thead><tbody className="divide-y divide-slate-100">{dashboard.queue.map((row) => <tr key={row.appointmentId}><td className="px-5 py-4 font-black text-care-700">{row.queuePosition ?? '—'}</td><td className="px-5 py-4 font-mono text-xs text-slate-600">{row.patientNumber}</td><td className="px-5 py-4 font-bold text-ink-950">{row.doctorName}</td><td className="px-5 py-4">{appointmentStatusLabel[row.status]}</td><td className="px-5 py-4">{row.paymentMethod === 'CASH' ? 'Cash' : 'Online'}</td>{canManage && <td className="px-5 py-4">{row.status === 'CONFIRMED' && date <= localDate() ? <button disabled={saving} onClick={() => void noShow(row.appointmentId)} className="text-xs font-extrabold text-rose-700 hover:underline">Mark no-show</button> : <span className="text-slate-300">—</span>}</td>}</tr>)}{dashboard.queue.length === 0 && <tr><td colSpan={canManage ? 6 : 5} className="px-5 py-10 text-center text-slate-500">No appointments for this view.</td></tr>}</tbody></table></div></section>
        <div className="mt-6 flex flex-wrap gap-3"><Link to="/diagnostics" className="rounded-xl border border-cyan-200 bg-cyan-50 px-4 py-2 text-xs font-extrabold text-cyan-800">Diagnostics</Link><Link to="/blood-support" className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-2 text-xs font-extrabold text-rose-800">Blood support</Link><Link to="/ambulance" className="rounded-xl border border-red-200 bg-red-50 px-4 py-2 text-xs font-extrabold text-red-800">Ambulance desk</Link></div>
      </>}
    </div>
  )
}

export function OperationsPage() {
  const { session } = useAuth()
  if (!session) return null
  const isStaff = session.user.roles.some((role) => staffRoles.includes(role))
  const canManage = session.user.roles.some((role) => managingRoles.includes(role))
  return isStaff ? <StaffOperations canManage={canManage} /> : <PatientRecovery />
}
