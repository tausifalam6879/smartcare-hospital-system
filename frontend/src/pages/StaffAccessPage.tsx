import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { accountTypes, AccountTypeSelect } from '../components/AccountTypeSelect'
import { StaffAccountControls } from '../components/StaffAccountControls'
import { StaffInvitationHistory } from '../components/StaffInvitationHistory'
import { StaffRequestInbox, type StaffRequest } from '../components/StaffRequestInbox'
import { useLanguage } from '../context/LanguageContext'
import { api, messageFromError } from '../services/api'

type Hospital = { id: string; name: string }
type Doctor = { id: string; name: string; registrationNumber: string }
type Issued = { id: string; invitationCode: string; expiresAt: string; mobileNumber: string; role: string; hospitalName: string }
export function StaffAccessPage() {
  const { text } = useLanguage()
  const [hospitals, setHospitals] = useState<Hospital[]>([]), [doctors, setDoctors] = useState<Doctor[]>([])
  const [hospitalId, setHospitalId] = useState(''), [doctorId, setDoctorId] = useState('')
  const [mobileNumber, setMobileNumber] = useState(''), [accountType, setAccountType] = useState('DOCTOR')
  const [request, setRequest] = useState<StaffRequest | null>(null), [verified, setVerified] = useState(false)
  const [issued, setIssued] = useState<Issued | null>(null), [error, setError] = useState(''), [busy, setBusy] = useState(false)
  const [hospitalsLoading, setHospitalsLoading] = useState(true), [hospitalError, setHospitalError] = useState('')
  const [doctorsLoading, setDoctorsLoading] = useState(false), [doctorError, setDoctorError] = useState('')
  const [retry, setRetry] = useState(0), [refresh, setRefresh] = useState(0), [copyMessage, setCopyMessage] = useState('')
  const formRef = useRef<HTMLFormElement>(null)
  useEffect(() => {
    let active = true
    setHospitalsLoading(true); setHospitalError('')
    api.get<Hospital[]>('/api/v1/hospitals').then(({ data }) => { if (active) setHospitals(data) })
      .catch(e => { if (active) setHospitalError(messageFromError(e)) }).finally(() => { if (active) setHospitalsLoading(false) })
    return () => { active = false }
  }, [retry])
  useEffect(() => {
    setDoctors([]); setDoctorId(''); setDoctorError(''); setDoctorsLoading(false)
    if (!hospitalId || accountType !== 'DOCTOR') return
    let active = true
    setDoctorsLoading(true)
    api.get<Doctor[]>('/api/v1/auth/staff-invitations/doctors', { params: { hospitalId } })
      .then(({ data }) => { if (active) setDoctors(data) })
      .catch(e => { if (active) setDoctorError(messageFromError(e)) }).finally(() => { if (active) setDoctorsLoading(false) })
    return () => { active = false }
  }, [hospitalId, accountType, retry, refresh])
  const roleName = (role: string) => { const item = accountTypes.find(t => t[0] === role); return item ? text(item[1], item[2]) : role }
  function review(item: StaffRequest) {
    setRequest(item); setAccountType(item.accountType); setMobileNumber(item.mobileNumber); setVerified(false); setError('')
    formRef.current?.scrollIntoView?.({ behavior: 'smooth', block: 'center' })
  }
  async function issue(event: FormEvent) {
    event.preventDefault()
    if (busy || issued || !verified || !hospitalId || (accountType === 'DOCTOR' && !doctorId)) return
    setBusy(true); setError(''); setCopyMessage('')
    const details = { mobileNumber: mobileNumber.trim(), role: roleName(accountType), hospitalName: hospitals.find(h => h.id === hospitalId)?.name ?? '' }
    try {
      const { data } = await api.post<Issued>('/api/v1/auth/staff-invitations', {
        mobileNumber: details.mobileNumber, accountType, hospitalId, doctorId: accountType === 'DOCTOR' ? doctorId : null,
        ...(request ? { requestId: request.id } : {}),
      })
      setIssued({ ...data, ...details }); setVerified(false); setRequest(null); setRefresh(r => r + 1)
    } catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  async function revoke() {
    if (!issued) return
    setBusy(true); setError('')
    try { await api.delete(`/api/v1/auth/staff-invitations/${issued.id}`); setIssued(null); setRefresh(r => r + 1) }
    catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  async function copy(details: boolean) {
    if (!issued) return
    const value = details ? `SmartCare staff invitation\nHospital: ${issued.hospitalName}\nRole: ${issued.role}\nMobile: ${issued.mobileNumber}\nCode: ${issued.invitationCode}\nExpires: ${new Date(issued.expiresAt).toLocaleString()}\nActivate: ${window.location.origin}${import.meta.env.BASE_URL}register\nSelect the invited role and use the exact mobile number above. Paste the entire code. New users create a password; existing users select the existing-account option and enter their current password. Use the code once, then sign in with your password. Do not forward this invitation.` : issued.invitationCode
    try { await navigator.clipboard.writeText(value); setCopyMessage(text('Copied. Share privately with the verified staff member.', 'कॉपी हो गया। सत्यापित स्टाफ से निजी रूप से साझा करें।')) }
    catch { setCopyMessage(text('Clipboard is unavailable. Select and copy the code manually.', 'क्लिपबोर्ड उपलब्ध नहीं है। कोड चुनकर स्वयं कॉपी करें।')) }
  }
  const input = 'mt-2 block min-h-12 w-full rounded-xl border border-blue-200 bg-white px-3 py-2'
  return <section className="mx-auto max-w-4xl rounded-3xl border border-blue-100 bg-white p-8 shadow-sm">
    <h1 className="text-3xl font-black text-blue-950">{text('Staff access and invitations', 'स्टाफ एक्सेस और invitation')}</h1>
    <p className="my-4 leading-6 text-slate-600">{text('Staff request access → you verify their identity and hospital role → issue a private invitation → staff activate once and sign in with their own password.', 'स्टाफ एक्सेस अनुरोध करता है → आप पहचान और अस्पताल की भूमिका सत्यापित करते हैं → निजी invitation देते हैं → स्टाफ एक बार अकाउंट चालू करके अपने पासवर्ड से लॉगिन करता है।')}</p>
    <p className="mb-4 text-sm"><Link to="/staff/request-access" className="font-bold text-blue-700">{text('Open staff request form', 'स्टाफ अनुरोध फॉर्म खोलें')}</Link> · {text('Only the system administrator can approve access. Requests never grant permissions automatically.', 'केवल सिस्टम एडमिन एक्सेस मंजूर कर सकता है। अनुरोध से अपने आप अधिकार नहीं मिलते।')}</p>
    <label className="text-sm font-bold">{text('Hospital', 'अस्पताल')}<select required disabled={busy || hospitalsLoading} value={hospitalId} onChange={e => { setHospitalId(e.target.value); setRequest(null); setVerified(false); setError('') }} className={input}><option value="">{text('Choose hospital', 'अस्पताल चुनें')}</option>{hospitals.map(h => <option key={h.id} value={h.id}>{h.name}</option>)}</select></label>
    {hospitalsLoading && <p role="status">{text('Loading hospitals…', 'अस्पताल लोड हो रहे हैं…')}</p>}
    {hospitalError && <p role="alert" className="my-3 text-rose-800">{hospitalError} <button onClick={() => setRetry(r => r + 1)} className="font-bold text-blue-700">{text('Retry hospitals', 'अस्पताल फिर लोड करें')}</button></p>}
    {!hospitalsLoading && !hospitalError && !hospitals.length && <p className="my-3">{text('No hospitals available. ', 'अभी अस्पताल उपलब्ध नहीं है। ')}<Link className="font-bold text-blue-700" to="/admin/setup">{text('Open hospital setup', 'अस्पताल सेटअप खोलें')}</Link></p>}
    <StaffRequestInbox key={`requests-${hospitalId}`} hospitalId={hospitalId} refresh={refresh} onReview={review} />
    <h2 className="mb-4 text-xl font-bold">{text('Verify and issue invitation', 'सत्यापित करके invitation दें')}</h2>
    {request && <div className="mb-4 rounded-xl bg-blue-50 p-4"><p>{text('Reviewing request from', 'अनुरोध की समीक्षा')} <strong>{request.name}</strong> · {request.mobileNumber}</p><p className="text-sm">{request.message}</p><button onClick={() => { setRequest(null); setVerified(false) }} className="mt-2 font-bold text-blue-700">{text('Cancel review', 'समीक्षा रद्द करें')}</button></div>}
    <form ref={formRef} onSubmit={issue} className="grid gap-4">
      <fieldset disabled={busy || !!issued} className="grid gap-4">
        {request ? <p className="font-bold">{text('Requested role:', 'अनुरोधित भूमिका:')} {roleName(request.accountType)}</p> : <AccountTypeSelect value={accountType} onChange={value => { setAccountType(value); setVerified(false) }} staffOnly />}
        {accountType === 'DOCTOR' && <label className="text-sm font-bold">{text('Verified doctor profile', 'सत्यापित डॉक्टर प्रोफ़ाइल')}<select required disabled={doctorsLoading} value={doctorId} onChange={e => { setDoctorId(e.target.value); setVerified(false) }} className={input}><option value="">{text('Choose an unlinked doctor', 'बिना लिंक वाला डॉक्टर चुनें')}</option>{doctors.map(d => <option key={d.id} value={d.id}>{d.name} · {d.registrationNumber}</option>)}</select></label>}
        {accountType === 'DOCTOR' && doctorsLoading && <p role="status">{text('Loading eligible doctors…', 'योग्य डॉक्टर लोड हो रहे हैं…')}</p>}
        {doctorError && <p role="alert">{doctorError} <button type="button" onClick={() => setRetry(r => r + 1)} className="font-bold text-blue-700">{text('Retry doctors', 'डॉक्टर फिर लोड करें')}</button></p>}
        {accountType === 'DOCTOR' && hospitalId && !doctorsLoading && !doctorError && !doctors.length && <p className="text-sm">{text('No active unlinked doctor profiles. ', 'कोई सक्रिय बिना लिंक वाली डॉक्टर प्रोफ़ाइल नहीं है। ')}<Link to="/admin/setup" className="font-bold text-blue-700">{text('Add a verified doctor in hospital setup', 'अस्पताल सेटअप में सत्यापित डॉक्टर जोड़ें')}</Link></p>}
        <label className="text-sm font-bold">{text('Invited mobile number', 'आमंत्रित मोबाइल नंबर')}<input required readOnly={!!request} type="tel" pattern="\+?[1-9][0-9]{7,14}" value={mobileNumber} onChange={e => { setMobileNumber(e.target.value); setVerified(false) }} className={input} placeholder="+919876543210" /></label>
        <p className="text-sm text-slate-600">{text('Include the country code. Staff must use this exact number and role during activation. Existing users select the existing-account option and confirm their current password; their records are preserved.', 'देश कोड शामिल करें। चालू करते समय यही सटीक नंबर और भूमिका देनी होगी। मौजूदा यूज़र अपने अकाउंट का विकल्प चुनकर मौजूदा पासवर्ड दें; रिकॉर्ड सुरक्षित रहेंगे।')}</p>
        <label className="flex items-start gap-3 text-sm"><input required type="checkbox" checked={verified} onChange={e => setVerified(e.target.checked)} className="mt-1" />{text('I independently verified this person, their hospital role and, for doctors, their medical profile.', 'मैंने इस व्यक्ति, अस्पताल की भूमिका और डॉक्टर होने पर उनकी मेडिकल प्रोफ़ाइल का स्वतंत्र सत्यापन किया है।')}</label>
        <button disabled={busy || !!issued || !verified || !hospitalId || !mobileNumber.trim() || (accountType === 'DOCTOR' && (!doctorId || doctorsLoading)) || !accountTypes.some(t => t[0] === accountType && t[0] !== 'PATIENT')} className="rounded-xl bg-blue-600 px-4 py-3 font-bold text-white disabled:opacity-50">{busy ? text('Issuing…', 'जारी कर रहे हैं…') : text('Issue one-time invitation', 'एक बार उपयोग वाला invitation दें')}</button>
      </fieldset>
    </form>
    {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 p-4 text-rose-800">{error}</p>}
    {issued && <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 p-5">
      <h2 className="font-bold">{text('Invitation ready to share privately', 'invitation निजी रूप से साझा करने के लिए तैयार है')}</h2>
      <p className="my-2">{issued.hospitalName} · {issued.role} · <strong>{issued.mobileNumber}</strong></p>
      <p className="text-sm">{text('Copy the full invitation now. The code is not saved in the browser and cannot be shown again after closing this page.', 'अभी पूरा invitation कॉपी करें। कोड browser में save नहीं होता और पेज बंद होने के बाद दोबारा नहीं दिखाया जा सकता।')}</p>
      <code className="my-3 block break-all select-all rounded-lg bg-white p-3">{issued.invitationCode}</code>
      <p className="text-sm">{text('Expires:', 'समाप्ति:')} {new Date(issued.expiresAt).toLocaleString()}</p>
      <div className="my-3 flex flex-wrap gap-3"><button onClick={() => void copy(true)} className="rounded-lg bg-blue-600 px-3 py-2 font-bold text-white">{text('Copy full invitation', 'पूरा invitation कॉपी करें')}</button><button onClick={() => void copy(false)} className="rounded-lg border border-blue-200 px-3 py-2 font-bold">{text('Copy code only', 'सिर्फ कोड कॉपी करें')}</button></div>
      {copyMessage && <p role="status" className="my-2 text-sm">{copyMessage}</p>}
      <p className="text-sm">{text('Staff open Register, choose the invited role, paste the full code and enter the exact mobile above. New users create a password; existing users select the existing-account option and confirm their current password.', 'स्टाफ रजिस्टर खोले, आमंत्रित भूमिका चुने, पूरा कोड और ऊपर वाला सटीक मोबाइल दे। नए यूज़र पासवर्ड बनाएँ; मौजूदा यूज़र अपना अकाउंट विकल्प चुनकर मौजूदा पासवर्ड दें।')}</p>
      <div className="mt-4 flex flex-wrap gap-3"><button disabled={busy} onClick={() => { setIssued(null); setCopyMessage('') }} className="rounded-lg border border-blue-300 px-3 py-2 font-bold">{text('Done sharing invitation', 'invitation साझा कर दिया')}</button><button disabled={busy} onClick={() => void revoke()} className="rounded-lg border border-rose-300 px-3 py-2 font-bold text-rose-800">{text('Revoke this invitation', 'यह invitation रद्द करें')}</button></div>
    </div>}
    <StaffInvitationHistory key={`invitations-${hospitalId}`} hospitalId={hospitalId} refresh={refresh} onRevoked={id => { if (issued?.id === id) setIssued(null); setRefresh(r => r + 1) }} />
    <StaffAccountControls hospitals={hospitals} />
  </section>
}
