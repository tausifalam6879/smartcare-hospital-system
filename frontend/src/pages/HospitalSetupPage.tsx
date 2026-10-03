import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, messageFromError } from '../services/api'

type Hospital = { id: string; name: string; departments: { id: string; name: string }[] }
type Doctor = { id: string; name: string; schedules: { dayOfWeek: string; startTime: string; endTime: string }[] }
const input = 'mt-2 block w-full rounded-xl border border-blue-200 bg-white p-3'
const panel = 'rounded-3xl border border-blue-100 bg-white p-6 shadow-sm'
const button = 'mt-5 rounded-xl bg-blue-600 px-5 py-3 font-bold text-white disabled:opacity-50'
type Field = [string, string, string?, string?, number?, number?]
function Fields({ fields }: { fields: Field[] }) {
  return <div className="grid gap-4 xl:grid-cols-2">{fields.map(([name, label, type = 'text', value, min, max]) => <label key={name} className="text-sm font-semibold">{label}<input name={name} aria-label={label} className={input} type={type} required defaultValue={value} min={min} max={max} maxLength={type === 'text' ? 140 : undefined} step={name === 'consultationFee' ? '0.01' : undefined} /></label>)}</div>
}
export function HospitalSetupPage() {
  const [hospitals, setHospitals] = useState<Hospital[]>([]), [hospitalId, setHospitalId] = useState('')
  const [doctors, setDoctors] = useState<Doctor[]>([]), [busy, setBusy] = useState(false)
  const [error, setError] = useState(''), [success, setSuccess] = useState('')
  const hospital = hospitals.find(h => h.id === hospitalId)
  async function refresh() { setHospitals((await api.get<Hospital[]>('/api/v1/hospitals')).data) }
  async function refreshDoctors() { setDoctors((await api.get<{ content: Doctor[] }>('/api/v1/doctors', { params: { hospitalId, size: 100 } })).data.content) }
  useEffect(() => { let active = true; api.get<Hospital[]>('/api/v1/hospitals').then(({ data }) => { if (active) setHospitals(data) }).catch(e => { if (active) setError(messageFromError(e)) }); return () => { active = false } }, [])
  useEffect(() => { let active = true; setDoctors([]); if (hospitalId) api.get<{ content: Doctor[] }>('/api/v1/doctors', { params: { hospitalId, size: 100 } }).then(({ data }) => { if (active) setDoctors(data.content) }).catch(e => { if (active) setError(messageFromError(e)) }); return () => { active = false } }, [hospitalId])
  async function save(e: FormEvent<HTMLFormElement>, kind: string) {
    e.preventDefault(); const form = e.currentTarget; const data = Object.fromEntries(new FormData(form).entries())
    setBusy(true); setError(''); setSuccess('')
    try {
      if (kind === 'hospital') { const result = await api.post<Hospital>('/api/v1/hospitals', { ...data, active: true }); await refresh(); setHospitalId(result.data.id) }
      if (kind === 'department') { await api.post(`/api/v1/hospitals/${hospitalId}/departments`, data); await refresh() }
      if (kind === 'doctor') { await api.post('/api/v1/doctors', { ...data, hospitalId, consultationFee: Number(data.consultationFee), expectedConsultationMinutes: Number(data.expectedConsultationMinutes), dailyMaxCapacity: Number(data.dailyMaxCapacity), active: true }); await refreshDoctors() }
      if (kind === 'schedule') {
        if (String(data.startTime) >= String(data.endTime)) { setError('End time must be after start time.'); return }
        await api.post(`/api/v1/doctors/${data.doctorId}/schedules`, { dayOfWeek: data.dayOfWeek, startTime: data.startTime, endTime: data.endTime, slotDurationMinutes: Number(data.slotDurationMinutes), capacityOverride: null }); await refreshDoctors()
      }
      form.reset(); setSuccess('Saved successfully. These records are available in the directory and staff invitations.')
    } catch (err) { setError(messageFromError(err)) } finally { setBusy(false) }
  }
  return <section className="mx-auto max-w-6xl space-y-6">
    <header className={panel}><h1 className="text-3xl font-black">Hospital setup / अस्पताल सेटअप</h1><p className="mt-3 text-slate-600">Add a hospital, departments, doctor profiles and weekly OPD hours, then invite staff.</p><Link className="mt-4 inline-block font-bold text-blue-600" to="/staff/access">Staff invitations →</Link></header>
    {error && <p role="alert" className="rounded-xl bg-rose-50 p-4 text-rose-800">{error}</p>}{success && <p role="status" className="rounded-xl bg-blue-50 p-4">{success}</p>}
    <form className={panel} onSubmit={e => void save(e, 'hospital')}><h2 className="mb-5 text-xl font-bold">1. Add hospital</h2><Fields fields={[
      ['code','Hospital code'],['name','Hospital name'],['addressLine','Address'],['city','City'],['state','State'],['postalCode','Postal code'],['contactNumber','Contact number','tel'],['timeZone','Time zone','text','Asia/Kolkata']
    ]} /><p className="mt-3 text-sm text-slate-600">Code: 2–30 letters, digits, underscores or hyphens. Contact: international mobile format, e.g. +91 followed by the number.</p><button disabled={busy} className={button}>Add hospital</button></form>
    <div className={panel}><label className="font-bold">Hospital to manage<select className={input} value={hospitalId} onChange={e => { setHospitalId(e.target.value); setError(''); setSuccess('') }}><option value="">Choose hospital</option>{hospitals.map(h => <option key={h.id} value={h.id}>{h.name}</option>)}</select></label>{!hospitals.length && <p className="mt-3">Add your first hospital above to continue.</p>}</div>
    {hospital && <>
      <form className={panel} onSubmit={e => void save(e, 'department')}><h2 className="mb-5 text-xl font-bold">2. Add department</h2><Fields fields={ [['code','Department code'],['name','Department name']] } /><button disabled={busy} className={button}>Add department</button><p className="mt-4">Departments: {hospital.departments.map(d => d.name).join(', ') || 'None yet'}</p></form>
      <form className={panel} onSubmit={e => void save(e, 'doctor')}><h2 className="mb-5 text-xl font-bold">3. Add doctor profile</h2><label className="mb-4 block font-semibold">Department<select className={input} name="departmentId" required><option value="">Choose department</option>{hospital.departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}</select></label><Fields fields={[
        ['name','Doctor name'],['specialization','Specialization'],['registrationNumber','Medical registration number'],['consultationFee','Consultation fee (INR)','number','500',0],['expectedConsultationMinutes','Consultation minutes','number','15',5,240],['dailyMaxCapacity','Daily OPD capacity','number','20',1,1000],['building','Building'],['floorLabel','Floor'],['roomNumber','Room']
      ]} /><button disabled={busy || !hospital.departments.length} className={button}>Add doctor</button></form>
      <form className={panel} onSubmit={e => void save(e, 'schedule')}><h2 className="mb-5 text-xl font-bold">4. Add weekly OPD hours</h2><p className="mb-4">Add each working day separately. Times use the hospital time zone. A doctor needs OPD hours before patients can book.</p><label className="block font-semibold">Doctor<select className={input} name="doctorId" required><option value="">Choose doctor</option>{doctors.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}</select></label><label className="my-4 block font-semibold">Day<select className={input} name="dayOfWeek">{['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'].map(day => <option key={day}>{day}</option>)}</select></label><Fields fields={ [['startTime','Start time','time','09:00'],['endTime','End time','time','13:00'],['slotDurationMinutes','Slot minutes','number','15',5,240]] } /><button className={button} disabled={busy || !doctors.length}>Add OPD hours</button></form>
      <div className={panel}><h2 className="text-xl font-bold">Doctor profiles</h2>{doctors.map(d => <div key={d.id} className="border-b py-4"><p className="font-bold">{d.name}</p><p className="text-sm text-slate-600">{d.schedules.map(s => `${s.dayOfWeek} ${s.startTime}–${s.endTime}`).join(' · ') || 'No OPD hours yet'}</p></div>)}{!doctors.length && <p>No doctor profiles yet.</p>}</div>
    </>}
  </section>
}
