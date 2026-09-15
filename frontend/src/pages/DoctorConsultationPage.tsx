import { CalendarCheck2, LoaderCircle, ShieldAlert, Stethoscope, UserRoundCheck } from 'lucide-react'
import { type FormEvent, useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { messageFromError } from '../services/api'
import { getDoctorAppointments, type Appointment } from '../services/appointments'
import { finalizeClinicalVisit } from '../services/medicalRecords'
import { serveNextPatient } from '../services/queues'

export function DoctorConsultationPage() {
  const { session } = useAuth()
  const isDoctor = session?.user.roles.includes('DOCTOR')
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [appointmentId, setAppointmentId] = useState('')
  const [diagnosis, setDiagnosis] = useState('')
  const [symptoms, setSymptoms] = useState('')
  const [notes, setNotes] = useState('')
  const [discharge, setDischarge] = useState('')
  const [recommendation, setRecommendation] = useState('')
  const [followUpDate, setFollowUpDate] = useState('')
  const [reminder, setReminder] = useState(true)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [calling, setCalling] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const loadAppointments = useCallback(async () => {
    const data = await getDoctorAppointments()
    setAppointments(data)
    setAppointmentId((current) => current || data.find((item) => item.status === 'IN_CONSULTATION')?.id || '')
  }, [])

  useEffect(() => {
    if (!isDoctor) { setLoading(false); return }
    loadAppointments().catch((cause) => setError(messageFromError(cause))).finally(() => setLoading(false))
  }, [isDoctor, loadAppointments])

  const eligible = useMemo(() => appointments.filter((item) => ['IN_CONSULTATION', 'COMPLETED'].includes(item.status)), [appointments])
  const activeConsultation = appointments.find((item) => item.status === 'IN_CONSULTATION')
  const nextCheckedIn = appointments.find((item) => item.status === 'CHECKED_IN')

  async function callNext() {
    if (!nextCheckedIn || activeConsultation) return
    setCalling(true); setError(''); setSuccess('')
    try {
      await serveNextPatient(nextCheckedIn.doctorId, nextCheckedIn.serviceDate)
      await loadAppointments()
      setSuccess('Next checked-in patient is now in consultation. The patient has been notified privately.')
    } catch (cause) { setError(messageFromError(cause)) }
    finally { setCalling(false) }
  }

  async function submit(event: FormEvent) {
    event.preventDefault(); setSaving(true); setError(''); setSuccess('')
    try {
      await finalizeClinicalVisit({ appointmentId, diagnosis, symptoms, doctorNotes: notes, dischargeSummary: discharge, followUpRecommendation: recommendation, followUpDate: followUpDate || undefined, medicationReminderEnabled: reminder })
      setSuccess('Visit finalized and queue status completed. The patient can now see the documented record and follow-up.')
      setAppointmentId(''); setDiagnosis(''); setSymptoms(''); setNotes(''); setDischarge(''); setRecommendation(''); setFollowUpDate('')
      await loadAppointments()
    } catch (cause) { setError(messageFromError(cause)) }
    finally { setSaving(false) }
  }

  if (!isDoctor) return <div className="mx-auto max-w-3xl px-4 py-16"><div className="rounded-3xl border border-amber-200 bg-amber-50 p-8 text-center"><ShieldAlert className="mx-auto size-9 text-amber-700" /><h1 className="mt-4 text-2xl font-black">Doctor access required</h1></div></div>
  return <div className="min-h-screen bg-slate-50"><section className="bg-ink-950 text-white"><div className="mx-auto max-w-5xl px-4 py-10 sm:px-6"><p className="text-xs font-black uppercase tracking-[.18em] text-blue-300">Doctor workspace</p><h1 className="mt-2 text-4xl font-black">Consultation, record & next patient</h1><p className="mt-3 text-sm text-blue-100/70">Only clinician-entered findings and guidance are stored.</p></div></section><main className="mx-auto max-w-5xl px-4 py-8 sm:px-6">{error && <p role="alert" className="mb-5 rounded-2xl bg-rose-50 p-4 font-bold text-rose-800">{error}</p>}{success && <p className="mb-5 rounded-2xl bg-emerald-50 p-4 font-bold text-emerald-800">{success}</p>}{loading ? <LoaderCircle className="mx-auto size-7 animate-spin text-care-600" /> : <><section className="mb-5 flex flex-col gap-4 rounded-2xl border border-care-200 bg-care-50 p-5 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-xs font-black uppercase tracking-wider text-care-700">Live OPD handoff</p><p className="mt-1 font-black text-ink-950">{activeConsultation ? 'Finish and finalize the current consultation before calling another patient.' : nextCheckedIn ? `OPD ${nextCheckedIn.queuePosition ?? '—'} is checked in and waiting.` : 'No checked-in patient is waiting.'}</p></div><button type="button" disabled={!nextCheckedIn || !!activeConsultation || calling} onClick={() => void callNext()} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-care-700 px-5 text-sm font-black text-white disabled:opacity-45">{calling ? <LoaderCircle className="size-5 animate-spin" /> : <UserRoundCheck className="size-5" />}Call next patient</button></section><form onSubmit={submit} className="grid gap-5 rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8"><label className="text-sm font-bold">Eligible appointment<select required value={appointmentId} onChange={(event) => setAppointmentId(event.target.value)} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3"><option value="">Choose appointment</option>{eligible.map((item) => <option key={item.id} value={item.id}>{item.serviceDate} · {item.departmentName} · OPD {item.queuePosition ?? '—'} · {item.status}</option>)}</select></label>{eligible.length === 0 && <p className="rounded-xl bg-amber-50 p-3 text-sm font-semibold text-amber-900">Call a checked-in patient to begin the consultation.</p>}<div className="grid gap-4 md:grid-cols-2"><Field label="Symptoms" value={symptoms} set={setSymptoms} /><Field label="Doctor-finalized diagnosis *" value={diagnosis} set={setDiagnosis} required /><Field label="Clinical notes" value={notes} set={setNotes} /><Field label="Discharge summary" value={discharge} set={setDischarge} /></div><label className="text-sm font-bold">Follow-up recommendation<textarea value={recommendation} onChange={(event) => setRecommendation(event.target.value)} maxLength={2000} rows={3} className="mt-2 w-full rounded-xl border border-slate-300 p-3" /></label><div className="grid gap-4 rounded-2xl border border-violet-200 bg-violet-50 p-5 md:grid-cols-2"><label className="text-sm font-bold text-violet-950"><CalendarCheck2 className="mr-2 inline size-5" />Follow-up date<input type="date" min={new Date().toISOString().slice(0, 10)} value={followUpDate} onChange={(event) => setFollowUpDate(event.target.value)} className="mt-2 h-12 w-full rounded-xl border border-violet-300 bg-white px-3" /></label><label className="flex items-center gap-3 self-end rounded-xl bg-white p-4 text-sm font-bold text-violet-950"><input type="checkbox" checked={reminder} onChange={(event) => setReminder(event.target.checked)} disabled={!followUpDate} className="size-5" />Enable medication reminder</label></div><button disabled={saving || !appointmentId} className="flex h-12 items-center justify-center gap-2 rounded-xl bg-care-700 font-black text-white disabled:opacity-50">{saving ? <LoaderCircle className="size-5 animate-spin" /> : <Stethoscope className="size-5" />}Finalize visit</button></form></>}</main></div>
}

function Field({ label, value, set, required = false }: { label: string; value: string; set: (value: string) => void; required?: boolean }) { return <label className="text-sm font-bold">{label}<textarea required={required} value={value} onChange={(event) => set(event.target.value)} rows={3} maxLength={4000} className="mt-2 w-full rounded-xl border border-slate-300 p-3" /></label> }
