import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AccountTypeSelect } from '../components/AccountTypeSelect'
import { AuthShell } from '../components/AuthShell'
import { useLanguage } from '../context/LanguageContext'
import { api, messageFromError } from '../services/api'

export function StaffAccessRequestPage() {
  const { text } = useLanguage()
  const [hospitals, setHospitals] = useState<{ id: string; name: string }[]>([])
  const [loading, setLoading] = useState(true), [busy, setBusy] = useState(false), [sent, setSent] = useState(false)
  const [error, setError] = useState(''), [retry, setRetry] = useState(0)
  const [accountType, setAccountType] = useState('DOCTOR')
  const [form, setForm] = useState({ name: '', mobileNumber: '', email: '', hospitalId: '', message: '' })
  useEffect(() => {
    let active = true
    setLoading(true); setError('')
    api.get('/api/v1/hospitals').then(({ data }) => { if (active) setHospitals(data) })
      .catch(e => { if (active) setError(messageFromError(e)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [retry])
  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('')
    try {
      await api.post('/api/v1/auth/staff-access-requests', {
        ...form, name: form.name.trim(), mobileNumber: form.mobileNumber.trim(),
        email: form.email.trim() || undefined, message: form.message.trim(), accountType,
      })
      setSent(true)
    } catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  const input = 'mt-2 block w-full rounded-xl border border-blue-200 bg-white p-3 font-normal'
  return <AuthShell title={text('Request staff access', 'स्टाफ एक्सेस का अनुरोध करें')} subtitle={text('Send your details to the system administrator for independent verification.', 'स्वतंत्र सत्यापन के लिए अपनी जानकारी सिस्टम एडमिन को भेजें।')}>
    {sent ? <div role="status" className="space-y-4">
      <h2 className="text-xl font-bold">{text('Request submitted for review', 'अनुरोध समीक्षा के लिए भेजा गया')}</h2>
      <p>{text('The administrator will verify your identity and hospital role, then share a one-time invitation privately using your contact details. No code is sent automatically, and this request does not create an account or grant staff access.', 'एडमिन आपकी पहचान और अस्पताल की भूमिका सत्यापित करके आपके संपर्क विवरण पर निजी रूप से invitation देगा। कोड अपने आप नहीं भेजा जाता। इस अनुरोध से अकाउंट या स्टाफ अधिकार नहीं मिलते।')}</p>
      <p>{text('Contact your hospital administrator if you need an update. Once you receive a code, activate your staff account with the same mobile number and role.', 'अपडेट के लिए अस्पताल के एडमिन से संपर्क करें। कोड मिलने पर उसी मोबाइल नंबर और भूमिका से स्टाफ अकाउंट चालू करें।')}</p>
    </div> : <form onSubmit={submit} className="space-y-4">
      <fieldset disabled={busy} className="space-y-4">
        <label className="block text-sm font-bold">{text('Full name', 'पूरा नाम')}<input required maxLength={120} autoComplete="name" className={input} value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} /></label>
        <label className="block text-sm font-bold">{text('Hospital', 'अस्पताल')}<select required disabled={loading} className={input} value={form.hospitalId} onChange={e => setForm({ ...form, hospitalId: e.target.value })}><option value="">{text('Choose hospital', 'अस्पताल चुनें')}</option>{hospitals.map(h => <option key={h.id} value={h.id}>{h.name}</option>)}</select></label>
        {loading && <p role="status">{text('Loading hospitals…', 'अस्पताल लोड हो रहे हैं…')}</p>}
        {!loading && !error && !hospitals.length && <p>{text('No hospitals are available yet. Contact the system administrator to complete hospital setup.', 'अभी अस्पताल उपलब्ध नहीं हैं। सेटअप के लिए सिस्टम एडमिन से संपर्क करें।')}</p>}
        <AccountTypeSelect staffOnly value={accountType} onChange={setAccountType} />
        <label className="block text-sm font-bold">{text('Mobile number', 'मोबाइल नंबर')}<input required type="tel" autoComplete="tel" pattern="\+?[1-9][0-9]{7,14}" placeholder="+919876543210" className={input} value={form.mobileNumber} onChange={e => setForm({ ...form, mobileNumber: e.target.value })} /></label>
        <p className="text-sm text-slate-600">{text('Include your country code. Use this exact number again when activating your invitation.', 'देश का कोड शामिल करें। invitation से अकाउंट चालू करते समय यही नंबर दें।')}</p>
        <label className="block text-sm font-bold">{text('Email for contact optional', 'संपर्क ईमेल वैकल्पिक')}<input type="email" maxLength={254} autoComplete="email" className={input} value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} /></label>
        <label className="block text-sm font-bold">{text('Hospital affiliation and role details', 'अस्पताल से संबंध और भूमिका की जानकारी')}<textarea required maxLength={500} rows={3} className={input} value={form.message} onChange={e => setForm({ ...form, message: e.target.value })} /></label>
        <p className="text-sm text-slate-600">{text('Give your department or staff reference so the administrator can verify you. Do not include passwords, identity documents or patient information.', 'विभाग या स्टाफ संदर्भ दें जिससे एडमिन सत्यापन कर सके। पासवर्ड, पहचान दस्तावेज़ या मरीज़ की जानकारी न दें।')}</p>
        <button disabled={busy || loading || !form.hospitalId} className="w-full rounded-xl bg-blue-600 p-3 font-bold text-white disabled:opacity-50">{busy ? text('Submitting…', 'भेज रहे हैं…') : text('Submit access request', 'एक्सेस अनुरोध भेजें')}</button>
      </fieldset>
    </form>}
    {error && <div role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-rose-800">{error}{!hospitals.length && <button type="button" onClick={() => setRetry(retry + 1)} className="ml-3 underline">{text('Retry hospitals', 'अस्पताल फिर लोड करें')}</button>}</div>}
    <Link to="/register" className="mt-6 block font-bold text-blue-700">{text('Already have an invitation? Activate staff account', 'invitation मिल गया? स्टाफ अकाउंट चालू करें')}</Link>
    <Link to="/login" className="mt-3 block text-blue-700">{text('Already activated? Sign in', 'अकाउंट चालू है? लॉगिन करें')}</Link>
  </AuthShell>
}
