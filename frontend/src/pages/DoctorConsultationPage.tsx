import { CalendarCheck2, ClipboardList, Droplets, FlaskConical, LoaderCircle, Pill, ShieldAlert, Stethoscope, UserRoundCheck } from 'lucide-react'
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { IdentityAvatar } from '../components/IdentityAvatar'
import { useAuth } from '../context/AuthContext'
import { publicAsset } from '../config/runtime'
import { messageFromError } from '../services/api'
import { appointmentStatusLabel, getDoctorAppointments, type Appointment } from '../services/appointments'
import { createDiagnosticOrder, getDiagnosticProcedures, type DiagnosticPriority, type DiagnosticProcedure } from '../services/diagnostics'
import { createBloodRequest, type BloodComponent, type BloodGroup } from '../services/bloodBank'
import { finalizeClinicalVisit, type AllergySeverity } from '../services/medicalRecords'
import { serveNextPatient } from '../services/queues'
import { localDateString } from '../utils/appointmentLifecycle'

const inputClass = 'h-11 rounded-xl border border-slate-300 bg-white px-3 text-sm'

export function DoctorConsultationPage() {
  const { session } = useAuth()
  const isDoctor = session?.user.roles.includes('DOCTOR')
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [appointmentId, setAppointmentId] = useState('')
  const [diagnosis, setDiagnosis] = useState(''), [symptoms, setSymptoms] = useState('')
  const [notes, setNotes] = useState(''), [discharge, setDischarge] = useState('')
  const [recommendation, setRecommendation] = useState(''), [followUpDate, setFollowUpDate] = useState('')
  const [reminder, setReminder] = useState(true)
  const [medicineName, setMedicineName] = useState(''), [dosage, setDosage] = useState('')
  const [frequency, setFrequency] = useState(''), [duration, setDuration] = useState('')
  const [prescriptionInstructions, setPrescriptionInstructions] = useState('')
  const [allergySubstance, setAllergySubstance] = useState(''), [allergyReaction, setAllergyReaction] = useState('')
  const [allergySeverity, setAllergySeverity] = useState<AllergySeverity>('MODERATE')
  const [procedures, setProcedures] = useState<DiagnosticProcedure[]>([]), [procedureId, setProcedureId] = useState('')
  const [diagnosticPriority, setDiagnosticPriority] = useState<DiagnosticPriority>('ROUTINE')
  const [diagnosticNote, setDiagnosticNote] = useState('')
  const [bloodGroup, setBloodGroup] = useState<BloodGroup>('O_POSITIVE')
  const [bloodComponent, setBloodComponent] = useState<BloodComponent>('PACKED_RED_CELLS')
  const [bloodUnits, setBloodUnits] = useState(1), [bloodUrgency, setBloodUrgency] = useState<'ROUTINE' | 'URGENT' | 'EMERGENCY'>('URGENT')
  const [bloodReason, setBloodReason] = useState('')
  const [loading, setLoading] = useState(true), [saving, setSaving] = useState(false)
  const [calling, setCalling] = useState(false), [orderingTest, setOrderingTest] = useState(false), [requestingBlood, setRequestingBlood] = useState(false)
  const [error, setError] = useState(''), [success, setSuccess] = useState('')

  const loadAppointments = useCallback(async () => {
    const data = await getDoctorAppointments()
    setAppointments(data)
    setAppointmentId((current) => data.some(item => item.id === current && item.status === 'IN_CONSULTATION' && item.serviceDate === localDateString()) ? current : data.find((item) => item.status === 'IN_CONSULTATION' && item.serviceDate === localDateString())?.id || '')
  }, [])

  useEffect(() => {
    if (!isDoctor) { setLoading(false); return }
    loadAppointments().catch((cause) => setError(messageFromError(cause))).finally(() => setLoading(false))
  }, [isDoctor, loadAppointments])

  useEffect(() => {
    if (!isDoctor || saving || calling) return
    const refresh = () => { if (!document.hidden) void loadAppointments().catch(cause => setError(messageFromError(cause))) }
    const timer = window.setInterval(refresh, 15000)
    window.addEventListener('focus', refresh)
    return () => { window.clearInterval(timer); window.removeEventListener('focus', refresh) }
  }, [isDoctor, saving, calling, loadAppointments])

  const todayAppointments = useMemo(() => appointments.filter((item) => item.serviceDate === localDateString()).sort((a, b) => (a.queuePosition ?? Number.MAX_SAFE_INTEGER) - (b.queuePosition ?? Number.MAX_SAFE_INTEGER)), [appointments])
  const eligible = useMemo(() => todayAppointments.filter((item) => item.status === 'IN_CONSULTATION'), [todayAppointments])
  const activeConsultation = todayAppointments.find((item) => item.status === 'IN_CONSULTATION')
  const nextCheckedIn = todayAppointments.find((item) => item.status === 'CHECKED_IN')
  const selectedAppointment = appointments.find((item) => item.id === appointmentId)

  useEffect(() => {
    setProcedures([]); setProcedureId('')
    if (!selectedAppointment) return
    getDiagnosticProcedures(selectedAppointment.hospitalId).then(setProcedures)
      .catch((cause) => setError(messageFromError(cause)))
  }, [selectedAppointment?.hospitalId])

  async function callNext() {
    if (!nextCheckedIn || activeConsultation) return
    setCalling(true); setError(''); setSuccess('')
    try {
      await serveNextPatient(nextCheckedIn.doctorId, nextCheckedIn.serviceDate)
      await loadAppointments()
      setSuccess('Next checked-in patient is now in consultation and has been notified privately.')
    } catch (cause) { setError(messageFromError(cause)) }
    finally { setCalling(false) }
  }

  async function orderDiagnostic() {
    if (!appointmentId || !procedureId) return
    setOrderingTest(true); setError(''); setSuccess('')
    try {
      const order = await createDiagnosticOrder({ appointmentId, procedureId, priority: diagnosticPriority, clinicalNote: diagnosticNote.trim() || undefined })
      setSuccess(`${order.procedureName} ordered. The patient can schedule it from Tests.`)
      setProcedureId(''); setDiagnosticNote(''); setDiagnosticPriority('ROUTINE')
    } catch (cause) { setError(messageFromError(cause)) }
    finally { setOrderingTest(false) }
  }

  async function requestBloodSupport() {
    if (!selectedAppointment || !bloodReason.trim()) return
    setRequestingBlood(true); setError(''); setSuccess('')
    try {
      const request = await createBloodRequest({
        patientNumber: selectedAppointment.patientNumber,
        hospitalId: selectedAppointment.hospitalId,
        appointmentId: selectedAppointment.id,
        bloodGroup, component: bloodComponent, units: bloodUnits, urgency: bloodUrgency,
        clinicalReason: bloodReason.trim(), idempotencyKey: crypto.randomUUID(),
      })
      setSuccess(`Blood-support request recorded: ${request.matchedUnits}/${request.requestedUnits} verified units matched.`)
      setBloodReason(''); setBloodUnits(1); setBloodUrgency('URGENT')
    } catch (cause) { setError(messageFromError(cause)) }
    finally { setRequestingBlood(false) }
  }

  async function submit(event: FormEvent) {
    event.preventDefault(); setSaving(true); setError(''); setSuccess('')
    try {
      await finalizeClinicalVisit({
        appointmentId, diagnosis, symptoms, doctorNotes: notes, dischargeSummary: discharge,
        followUpRecommendation: recommendation, prescriptionInstructions,
        medicines: medicineName.trim() ? [{ medicineName: medicineName.trim(), dosage: dosage.trim(), frequency: frequency.trim(), duration: duration.trim() }] : [],
        allergies: allergySubstance.trim() ? [{ substance: allergySubstance.trim(), reaction: allergyReaction.trim() || undefined, severity: allergySeverity }] : [],
        followUpDate: followUpDate || undefined, medicationReminderEnabled: reminder && Boolean(medicineName.trim()),
      })
      setSuccess('Visit, prescription and safety record finalized. Queue status is completed.')
      setAppointmentId(''); setDiagnosis(''); setSymptoms(''); setNotes(''); setDischarge(''); setRecommendation(''); setFollowUpDate('')
      setMedicineName(''); setDosage(''); setFrequency(''); setDuration(''); setPrescriptionInstructions('')
      setAllergySubstance(''); setAllergyReaction('')
      await loadAppointments()
    } catch (cause) { setError(messageFromError(cause)) }
    finally { setSaving(false) }
  }

  if (!isDoctor) return <div className="mx-auto max-w-3xl px-4 py-16"><div className="rounded-3xl border border-amber-200 bg-amber-50 p-8 text-center"><ShieldAlert className="mx-auto size-9 text-amber-700" /><h1 className="mt-4 text-2xl font-black">Doctor access required</h1></div></div>
  return <div className="doctor-workspace min-h-screen bg-[#f4f9fd]">
    <section className="relative overflow-hidden border-b border-blue-100 bg-gradient-to-r from-blue-50 via-indigo-50 to-white"><img src={publicAsset('images/smartcare-clinical-team.png')} alt="SmartCare care team" className="absolute inset-y-0 right-0 hidden h-full w-[46%] object-cover object-top opacity-90 md:block" /><div className="absolute inset-0 bg-gradient-to-r from-blue-50 via-indigo-50/95 to-white/10" /><div className="relative mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:py-14"><p className="text-xs font-black uppercase tracking-[.18em] text-blue-700">Doctor workspace</p><h1 className="mt-2 max-w-2xl text-4xl font-black text-ink-950">Today's patients and consultations</h1><p className="mt-3 max-w-xl text-sm text-slate-600">Check the live queue, call the next patient and complete the clinical visit from one place.</p></div></section>
    <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6">
      {error && <p role="alert" className="mb-5 rounded-2xl bg-rose-50 p-4 font-bold text-rose-800">{error}</p>}
      {success && <p className="mb-5 rounded-2xl bg-emerald-50 p-4 font-bold text-emerald-800">{success}</p>}
      {loading ? <LoaderCircle className="mx-auto size-7 animate-spin text-care-600" /> : <>
        <section className="mb-5 grid gap-3 sm:grid-cols-3"><div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"><p className="text-xs font-bold text-slate-500">Today's appointments</p><p className="mt-2 text-3xl font-black text-ink-950">{todayAppointments.length}</p></div><div className="rounded-2xl border border-blue-100 bg-blue-50 p-4 shadow-sm"><p className="text-xs font-bold text-blue-700">Checked in & waiting</p><p className="mt-2 text-3xl font-black text-blue-800">{todayAppointments.filter((item) => item.status === 'CHECKED_IN').length}</p></div><div className="rounded-2xl border border-indigo-100 bg-indigo-50 p-4 shadow-sm"><p className="text-xs font-bold text-indigo-700">Completed visits</p><p className="mt-2 text-3xl font-black text-indigo-800">{todayAppointments.filter((item) => item.status === 'COMPLETED').length}</p></div></section>
        <section className="mb-5 flex flex-col gap-4 rounded-2xl border border-care-200 bg-care-50 p-5 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-xs font-black uppercase tracking-wider text-care-700">Live OPD handoff</p><p className="mt-1 font-black text-ink-950">{activeConsultation ? 'Finalize the current consultation before calling another patient.' : nextCheckedIn ? `OPD ${nextCheckedIn.queuePosition ?? '—'} is checked in and waiting.` : 'No checked-in patient is waiting.'}</p></div><button type="button" disabled={!nextCheckedIn || !!activeConsultation || calling} onClick={() => void callNext()} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-care-700 px-5 text-sm font-black text-white disabled:opacity-45">{calling ? <LoaderCircle className="size-5 animate-spin" /> : <UserRoundCheck className="size-5" />}Call next patient</button></section>
        <section className="mb-5 overflow-hidden rounded-3xl border border-blue-100 bg-white shadow-sm"><div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-5"><div><p className="text-xs font-black uppercase tracking-[.15em] text-blue-700">Live worklist</p><h2 className="mt-1 text-xl font-black text-ink-950">Patient queue</h2><p className="mt-1 text-xs text-slate-500">Patient numbers protect identities in this shared view.</p></div><Link to="/operations" className="inline-flex items-center gap-2 rounded-xl border border-blue-200 px-3 py-2 text-xs font-black text-blue-700"><ClipboardList className="size-4" />Day operations</Link></div><div className="overflow-x-auto"><table className="w-full min-w-[560px] text-left text-sm"><thead className="bg-blue-50/60 text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-5 py-3">OPD</th><th className="px-5 py-3">Patient</th><th className="px-5 py-3">Department</th><th className="px-5 py-3">Status</th></tr></thead><tbody className="divide-y divide-slate-100">{todayAppointments.map((item) => <tr key={item.id} className="hover:bg-blue-50/40"><td className="px-5 py-3 font-black text-blue-700">{item.queuePosition ?? '—'}</td><td className="px-5 py-3"><span className="flex items-center gap-3"><IdentityAvatar size="sm" /><span className="font-mono text-xs text-slate-700">{item.patientNumber}</span></span></td><td className="px-5 py-3 text-slate-600">{item.departmentName}</td><td className="px-5 py-3 font-bold text-slate-700">{appointmentStatusLabel[item.status]}</td></tr>)}{todayAppointments.length === 0 && <tr><td colSpan={4} className="px-5 py-8 text-center text-slate-500">No patient visits scheduled for today.</td></tr>}</tbody></table></div></section>
        <form onSubmit={submit} className="grid gap-5 rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
          <label className="text-sm font-bold">Current consultation<select required disabled={eligible.length === 0} value={appointmentId} onChange={(event) => setAppointmentId(event.target.value)} className={`mt-2 w-full disabled:cursor-not-allowed disabled:bg-slate-100 ${inputClass}`}><option value="">{eligible.length === 0 ? 'No patient is currently in consultation' : 'Choose patient appointment'}</option>{eligible.map((item) => <option key={item.id} value={item.id}>{item.patientNumber} · {item.serviceDate} · {item.departmentName} · OPD {item.queuePosition ?? '—'}</option>)}</select></label>
          {eligible.length === 0 && <p className="rounded-xl bg-amber-50 p-3 text-sm font-semibold leading-6 text-amber-900">No form can be opened yet. The patient must have today’s confirmed appointment, complete check-in at the operations desk, and then be called using <strong>Call next patient</strong>.</p>}
          {selectedAppointment && <div className="flex flex-col gap-4 rounded-2xl border border-blue-200 bg-blue-50 p-4 text-sm text-blue-950 sm:flex-row sm:items-center"><span className="grid size-12 shrink-0 place-items-center rounded-full bg-care-700 text-base font-black text-white" aria-hidden="true">{selectedAppointment.patientNumber.slice(-2)}</span><div className="grid flex-1 gap-2 sm:grid-cols-3"><p><strong>Patient ID:</strong> {selectedAppointment.patientNumber}</p><p><strong>OPD:</strong> {selectedAppointment.queuePosition ?? '—'}</p><p><strong>Status:</strong> {selectedAppointment.status.replaceAll('_', ' ')}</p></div></div>}
          <div className="grid gap-4 md:grid-cols-2"><Field label="Symptoms" value={symptoms} set={setSymptoms} /><Field label="Doctor-finalized diagnosis *" value={diagnosis} set={setDiagnosis} required /><Field label="Clinical notes" value={notes} set={setNotes} /><Field label="Discharge summary" value={discharge} set={setDischarge} /></div>
          <section className="grid gap-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-5"><h2 className="flex items-center gap-2 font-black text-emerald-950"><Pill className="size-5" />Prescription (optional)</h2><div className="grid gap-3 md:grid-cols-4"><input value={medicineName} onChange={(e) => setMedicineName(e.target.value)} placeholder="Medicine name" className={inputClass} /><input required={!!medicineName} value={dosage} onChange={(e) => setDosage(e.target.value)} placeholder="Dose" className={inputClass} /><input required={!!medicineName} value={frequency} onChange={(e) => setFrequency(e.target.value)} placeholder="Frequency" className={inputClass} /><input required={!!medicineName} value={duration} onChange={(e) => setDuration(e.target.value)} placeholder="Duration" className={inputClass} /></div><textarea value={prescriptionInstructions} onChange={(e) => setPrescriptionInstructions(e.target.value)} maxLength={1200} rows={2} placeholder="General instructions" className="rounded-xl border border-emerald-200 p-3 text-sm" /></section>
          <section className="grid gap-4 rounded-2xl border border-rose-200 bg-rose-50 p-5"><h2 className="font-black text-rose-950">Clinician-recorded allergy (optional)</h2><div className="grid gap-3 md:grid-cols-3"><input value={allergySubstance} onChange={(e) => setAllergySubstance(e.target.value)} placeholder="Substance or medicine" className={inputClass} /><input value={allergyReaction} onChange={(e) => setAllergyReaction(e.target.value)} placeholder="Reaction" className={inputClass} /><select value={allergySeverity} onChange={(e) => setAllergySeverity(e.target.value as AllergySeverity)} className={inputClass}><option value="LOW">Low</option><option value="MODERATE">Moderate</option><option value="HIGH">High</option><option value="CRITICAL">Critical</option></select></div></section>
          <section className="grid gap-4 rounded-2xl border border-cyan-200 bg-cyan-50 p-5"><h2 className="flex items-center gap-2 font-black text-cyan-950"><FlaskConical className="size-5" />Diagnostic order (optional)</h2><div className="grid gap-3 md:grid-cols-[1fr_10rem]"><select value={procedureId} onChange={(e) => setProcedureId(e.target.value)} disabled={!appointmentId} className={inputClass}><option value="">Choose hospital-published test</option>{procedures.map((procedure) => <option key={procedure.id} value={procedure.id}>{procedure.name} · {procedure.modality}</option>)}</select><select value={diagnosticPriority} onChange={(e) => setDiagnosticPriority(e.target.value as DiagnosticPriority)} className={inputClass}><option value="ROUTINE">Routine</option><option value="URGENT">Urgent</option></select></div><textarea value={diagnosticNote} onChange={(e) => setDiagnosticNote(e.target.value)} maxLength={2000} rows={2} placeholder="Clinical reason" className="rounded-xl border border-cyan-200 p-3 text-sm" /><button type="button" disabled={!appointmentId || !procedureId || orderingTest} onClick={() => void orderDiagnostic()} className="min-h-11 rounded-xl bg-cyan-800 text-sm font-black text-white disabled:opacity-45">{orderingTest ? 'Ordering…' : 'Create diagnostic order'}</button><p className="text-xs text-cyan-900">The patient schedules it; lab staff releases only an authorized verified result.</p></section>
          <section className="grid gap-4 rounded-2xl border border-rose-200 bg-rose-50 p-5"><h2 className="flex items-center gap-2 font-black text-rose-950"><Droplets className="size-5" />Blood support request (when clinically required)</h2><div className="grid gap-3 md:grid-cols-4"><select value={bloodGroup} onChange={(e) => setBloodGroup(e.target.value as BloodGroup)} className={inputClass}><option value="A_POSITIVE">A+</option><option value="A_NEGATIVE">A−</option><option value="B_POSITIVE">B+</option><option value="B_NEGATIVE">B−</option><option value="AB_POSITIVE">AB+</option><option value="AB_NEGATIVE">AB−</option><option value="O_POSITIVE">O+</option><option value="O_NEGATIVE">O−</option></select><select value={bloodComponent} onChange={(e) => setBloodComponent(e.target.value as BloodComponent)} className={inputClass}><option value="WHOLE_BLOOD">Whole blood</option><option value="PACKED_RED_CELLS">Packed red cells</option><option value="PLATELETS">Platelets</option><option value="FRESH_FROZEN_PLASMA">Fresh frozen plasma</option></select><input type="number" min={1} max={20} value={bloodUnits} onChange={(e) => setBloodUnits(Math.max(1, Math.min(20, Number(e.target.value) || 1)))} className={inputClass} aria-label="Required units" /><select value={bloodUrgency} onChange={(e) => setBloodUrgency(e.target.value as typeof bloodUrgency)} className={inputClass}><option value="ROUTINE">Routine</option><option value="URGENT">Urgent</option><option value="EMERGENCY">Emergency</option></select></div><textarea value={bloodReason} onChange={(e) => setBloodReason(e.target.value)} maxLength={2000} rows={2} placeholder="Required clinical reason" className="rounded-xl border border-rose-200 p-3 text-sm" /><button type="button" disabled={!selectedAppointment || !bloodReason.trim() || requestingBlood} onClick={() => void requestBloodSupport()} className="min-h-11 rounded-xl bg-rose-700 text-sm font-black text-white disabled:opacity-45">{requestingBlood ? 'Submitting…' : 'Send to blood-bank staff'}</button><p className="text-xs text-rose-900">The selected group is a clinician-entered requirement, not an automated blood-group diagnosis. Only recently verified inventory can be reserved.</p></section>
          <label className="text-sm font-bold">Follow-up recommendation<textarea value={recommendation} onChange={(e) => setRecommendation(e.target.value)} maxLength={2000} rows={3} className="mt-2 w-full rounded-xl border border-slate-300 p-3" /></label>
          <div className="grid gap-4 rounded-2xl border border-violet-200 bg-violet-50 p-5 md:grid-cols-2"><label className="text-sm font-bold text-violet-950"><CalendarCheck2 className="mr-2 inline size-5" />Follow-up date<input type="date" min={localDateString()} value={followUpDate} onChange={(e) => setFollowUpDate(e.target.value)} className={`mt-2 w-full ${inputClass}`} /></label><label className="self-end rounded-xl bg-white p-4 text-sm font-bold text-violet-950"><span className="flex items-center gap-3"><input type="checkbox" checked={reminder && Boolean(medicineName.trim())} onChange={(e) => setReminder(e.target.checked)} disabled={!followUpDate || !medicineName.trim()} className="size-5" />Enable medication reminder</span><span className="mt-2 block text-[11px] font-medium text-violet-700">Available only when a structured prescription and follow-up date are recorded.</span></label></div>
          <button disabled={saving || !appointmentId} className="flex h-12 items-center justify-center gap-2 rounded-xl bg-care-700 font-black text-white disabled:opacity-50">{saving ? <LoaderCircle className="size-5 animate-spin" /> : <Stethoscope className="size-5" />}Finalize visit</button>
        </form>
      </>}
    </main>
  </div>
}

function Field({ label, value, set, required = false }: { label: string; value: string; set: (value: string) => void; required?: boolean }) {
  return <label className="text-sm font-bold">{label}<textarea required={required} value={value} onChange={(event) => set(event.target.value)} rows={3} maxLength={4000} className="mt-2 w-full rounded-xl border border-slate-300 p-3" /></label>
}
