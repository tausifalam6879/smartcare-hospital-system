import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { AuthShell } from '../components/AuthShell'
import { useLanguage } from '../context/LanguageContext'
import { api, messageFromError } from '../services/api'
export function RecoverAccountPage() {
  const { text } = useLanguage()
  const [mobile, setMobile] = useState(''), [code, setCode] = useState(''), [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState(''), [error, setError] = useState(''), [done, setDone] = useState(false), [busy, setBusy] = useState(false)
  async function submit(e: FormEvent) {
    e.preventDefault(); setError('')
    if (password !== confirm) { setError(text('Passwords do not match.', 'पासवर्ड मेल नहीं खाते।')); return }
    setBusy(true)
    try { await api.post('/api/v1/auth/recover', { mobileNumber: mobile.trim(), code: code.trim(), newPassword: password }); setCode(''); setPassword(''); setConfirm(''); setDone(true) }
    catch (e) { setError(messageFromError(e)) } finally { setBusy(false) }
  }
  const input = 'mt-2 w-full rounded-xl border border-blue-200 bg-white px-4 py-3'
  return <AuthShell title={text('Recover your account', 'खाता वापस पाएँ')} subtitle={text('Contact the hospital administrator to verify your identity and receive a private, one-time recovery code. It expires in 30 minutes.', 'अपनी पहचान सत्यापित कराने और निजी, एक बार उपयोग वाला कोड लेने के लिए अस्पताल एडमिन से संपर्क करें। कोड 30 मिनट में समाप्त होता है।')}>
    {done ? <p role="status" className="rounded-xl bg-blue-50 p-4">{text('Password reset. Previous sessions are signed out.', 'पासवर्ड बदल गया। पुराने सेशन लॉगआउट हो गए।')}</p> : <form onSubmit={submit}><fieldset disabled={busy} className="space-y-4"><label className="block text-sm font-semibold">{text('Registered mobile number', 'रजिस्टर्ड मोबाइल नंबर')}<input required autoComplete="tel" maxLength={20} value={mobile} onChange={e => setMobile(e.target.value)} className={input} /></label><label className="block text-sm font-semibold">{text('Recovery code', 'रिकवरी कोड')}<input required type="password" autoComplete="off" maxLength={100} value={code} onChange={e => setCode(e.target.value)} className={input} /></label><label className="block text-sm font-semibold">{text('New password', 'नया पासवर्ड')}<input required type="password" minLength={12} maxLength={72} autoComplete="new-password" value={password} onChange={e => setPassword(e.target.value)} className={input} /></label><label className="block text-sm font-semibold">{text('Confirm password', 'पासवर्ड दोहराएँ')}<input required type="password" minLength={12} maxLength={72} autoComplete="new-password" value={confirm} onChange={e => setConfirm(e.target.value)} className={input} /></label>{error && <p role="alert" className="text-sm text-rose-700">{error}</p>}<button className="w-full rounded-xl bg-blue-600 p-3 font-semibold text-white">{text('Reset password', 'पासवर्ड रीसेट करें')}</button></fieldset></form>}
    <Link to="/login" className="mt-5 block text-center text-sm font-semibold text-blue-700">{text('Back to sign in', 'लॉगिन पर वापस जाएँ')}</Link>
  </AuthShell>
}
