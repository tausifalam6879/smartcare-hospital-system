import { useEffect, useState, type FormEvent } from 'react'
import { accountTypes, AccountTypeSelect } from '../components/AccountTypeSelect'
import { useLanguage } from '../context/LanguageContext'
import { api, messageFromError } from '../services/api'
import { StaffAccountControls } from '../components/StaffAccountControls'
import { Link } from 'react-router-dom'

type Hospital = { id: string; name: string }
type Doctor = { id: string; name: string }
type Issued = { id: string; invitationCode: string; expiresAt: string }
export function StaffAccessPage() {
  const { text } = useLanguage()
  const [hospitals, setHospitals] = useState<Hospital[]>([])
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [hospitalId, setHospitalId] = useState(''), [doctorId, setDoctorId] = useState('')
  const [mobileNumber, setMobileNumber] = useState(''), [accountType, setAccountType] = useState('DOCTOR')
  const [issued, setIssued] = useState<Issued | null>(null), [error, setError] = useState(''), [busy, setBusy] = useState(false)
  useEffect(() => {
    let active = true
    api.get<Hospital[]>('/api/v1/hospitals').then(({ data }) => { if (active) setHospitals(data) }).catch(e => { if (active) setError(messageFromError(e)) })
    return () => { active = false }
  }, [])
  useEffect(() => {
    setDoctors([]); setDoctorId('')
    if (!hospitalId || accountType !== 'DOCTOR') return
    let active = true
    api.get<{ content: Doctor[] }>('/api/v1/doctors', { params: { hospitalId, size: 100 } })
      .then(({ data }) => { if (active) setDoctors(data.content) }).catch(e => { if (active) setError(messageFromError(e)) })
    return () => { active = false }
  }, [hospitalId, accountType])
  async function issue(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setIssued(null)
    try { setIssued((await api.post<Issued>('/api/v1/auth/staff-invitations', { mobileNumber: mobileNumber.trim(), accountType, hospitalId, doctorId: accountType === 'DOCTOR' ? doctorId : null })).data) }
    catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  async function revoke() {
    if (!issued) return
    setBusy(true); setError('')
    try { await api.delete(`/api/v1/auth/staff-invitations/${issued.id}`); setIssued(null) }
    catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  const input = 'mt-2 block h-12 w-full rounded-xl border border-blue-200 bg-white px-3'
  return <section className="mx-auto max-w-3xl rounded-3xl border border-blue-100 bg-white p-8 shadow-sm">
    <h1 className="text-3xl font-black text-blue-950">{text('Staff invitations', 'स्टाफ invitations')}</h1>
    <p className="my-4 text-sm leading-6 text-slate-600">{text('System administrator only. Verify the person and hospital role before issuing a code. Share it privately; it works once and expires in 24 hours.', 'केवल सिस्टम एडमिन। Code देने से पहले व्यक्ति और अस्पताल में उनकी भूमिका सत्यापित करें। Code निजी रूप से दें; यह एक बार उपयोग होता है और 24 घंटे में समाप्त होता है।')}</p>
    <form onSubmit={issue} className="grid gap-4">
      <label className="text-sm font-bold">{text('Hospital', 'अस्पताल')}<select required value={hospitalId} onChange={e => setHospitalId(e.target.value)} className={input}><option value="">{text('Choose hospital', 'अस्पताल चुनें')}</option>{hospitals.map(h => <option key={h.id} value={h.id}>{h.name}</option>)}</select></label>
      {!hospitals.length && <p className="rounded-xl bg-blue-50 p-4">{text('No hospitals available. ', 'अभी अस्पताल उपलब्ध नहीं है। ')}<Link className="font-bold text-blue-700" to="/admin/setup">{text('Open hospital setup', 'अस्पताल सेटअप खोलें')}</Link></p>}
      <AccountTypeSelect value={accountType} onChange={setAccountType} staffOnly />
      {accountType === 'PATIENT' && <p role="alert">{text('Patients register without an invitation. Choose a staff role.', 'मरीज़ बिना invitation रजिस्टर करते हैं। स्टाफ role चुनें।')}</p>}
      {accountType === 'DOCTOR' && <label className="text-sm font-bold">{text('Verified doctor profile', 'सत्यापित डॉक्टर प्रोफ़ाइल')}<select required value={doctorId} onChange={e => setDoctorId(e.target.value)} className={input}><option value="">{text('Choose doctor (must not be linked already)', 'डॉक्टर चुनें (पहले से linked नहीं होना चाहिए)')}</option>{doctors.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}</select></label>}
      <label className="text-sm font-bold">{text('Invited mobile number', 'आमंत्रित मोबाइल नंबर')}<input required type="tel" pattern="\+?[1-9][0-9]{7,14}" value={mobileNumber} onChange={e => setMobileNumber(e.target.value)} className={input} placeholder="+919876543210" /></label>
      <button disabled={busy || !hospitalId || (accountType === 'DOCTOR' && !doctorId) || accountType === 'PATIENT' || !accountTypes.some(t => t[0] === accountType)} className="rounded-xl bg-blue-600 px-4 py-3 font-bold text-white disabled:opacity-50">{text('Issue one-time invitation', 'एक बार उपयोग वाला invitation दें')}</button>
    </form>
    {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 p-4 text-rose-800">{error}</p>}
    {issued && <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-5">
      <p className="font-bold">{text('Copy privately now — this code is not saved in the browser.', 'Code अभी निजी रूप से कॉपी करें — browser में save नहीं होगा।')}</p>
      <code className="my-3 block break-all select-all rounded-lg bg-white p-3">{issued.invitationCode}</code>
      <p className="text-sm">{text('Expires:', 'समाप्ति:')} {new Date(issued.expiresAt).toLocaleString()}</p>
      <button type="button" disabled={busy} onClick={() => void revoke()} className="mt-3 rounded-lg border border-rose-300 px-3 py-2 text-sm font-bold text-rose-800">{text('Revoke this invitation', 'यह invitation रद्द करें')}</button>
    </div>}
    <StaffAccountControls hospitals={hospitals} />
  </section>
}
