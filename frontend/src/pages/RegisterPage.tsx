import { LoaderCircle } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthShell } from '../components/AuthShell'
import { AccountTypeSelect } from '../components/AccountTypeSelect'
import { useLanguage } from '../context/LanguageContext'
import { useAuth } from '../context/AuthContext'
import { messageFromError } from '../services/api'
import { localDateString } from '../utils/appointmentLifecycle'

const initial = { name: '', mobileNumber: '', email: '', password: '', dateOfBirth: '', emergencyContact: '', preferredLanguage: 'en' }
const mobileNumberPattern = /^\+?[1-9][0-9]{7,14}$/
const htmlMobileNumberPattern = '\\+?[1-9][0-9]{7,14}'

function normaliseMobileNumber(value: string) {
  return value.replace(/[\s()-]/g, '')
}

export function RegisterPage() {
  const { text } = useLanguage()
  const [accountType, setAccountType] = useState('PATIENT')
  const [invitationCode, setInvitationCode] = useState('')
  const [showInvitation, setShowInvitation] = useState(false)
  const [existingAccount, setExistingAccount] = useState(false)
  const staff = accountType !== 'PATIENT'
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(initial)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const update = (field: keyof typeof initial, value: string) => setForm((current) => ({ ...current, [field]: value }))

  async function submit(event: FormEvent) {
    event.preventDefault()
    const mobileNumber = normaliseMobileNumber(form.mobileNumber)
    const emergencyContact = staff ? '' : normaliseMobileNumber(form.emergencyContact)
    if (!mobileNumberPattern.test(mobileNumber)) {
      setError('Enter a valid mobile number with country code, for example +919876543210.')
      return
    }
    if (emergencyContact && !mobileNumberPattern.test(emergencyContact)) {
      setError('Emergency contact must be a mobile number, for example +919876543210 — not a person\'s name.')
      return
    }
    setLoading(true)
    setError('')
    try {
      await register({
        ...form,
        ...(staff ? { accountType, invitationCode: invitationCode.trim(), ...(existingAccount ? { existingAccount: true } : {}) } : {}),
        name: form.name.trim(),
        mobileNumber,
        email: form.email.trim() || undefined,
        dateOfBirth: !staff && form.dateOfBirth ? form.dateOfBirth : undefined,
        emergencyContact: emergencyContact || undefined,
      })
      navigate('/dashboard', { replace: true, state: 'registered' })
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setLoading(false)
    }
  }

  const inputClass = 'mt-2 h-13 w-full rounded-2xl border border-slate-300 bg-white px-4 font-normal'
  return (
    <AuthShell title={staff ? text('Activate authorized staff account', 'अधिकृत स्टाफ अकाउंट चालू करें') : text('Create your patient account', 'मरीज़ का अकाउंट बनाएँ')} subtitle={text('Patient registration is open. Staff accounts require an administrator-issued invitation.', 'मरीज़ अपना अकाउंट बना सकते हैं। स्टाफ के लिए एडमिन का invitation आवश्यक है।')}>
      <AccountTypeSelect value={accountType} onChange={value => { setAccountType(value); setInvitationCode(''); setError('') }} />
      <form onSubmit={submit} className="space-y-4">
        {staff && <div className="rounded-2xl border border-blue-200 bg-blue-50 p-4"><label className="block text-sm font-bold">{text('Staff invitation code', 'स्टाफ invitation code')}<input required type={showInvitation ? 'text' : 'password'} maxLength={100} autoComplete="off" autoCapitalize="none" spellCheck={false} value={invitationCode} onChange={e => setInvitationCode(e.target.value)} className={inputClass} /></label><button type="button" onClick={() => setShowInvitation(!showInvitation)} className="mt-2 text-sm font-bold text-blue-700">{showInvitation ? text('Hide invitation code', 'invitation कोड छिपाएँ') : text('Show invitation code', 'invitation कोड दिखाएँ')}</button><p className="mt-2 text-xs leading-5 text-slate-600">{text('Paste the complete code, including any hyphens or underscores. Use the exact mobile number and role in the invitation. It works once and expires after 24 hours.', 'पूरा कोड पेस्ट करें, hyphen और underscore सहित। invitation वाला सटीक मोबाइल नंबर और भूमिका चुनें। कोड एक बार चलता है और 24 घंटे में समाप्त होता है।')}</p><Link to="/staff/request-access" className="mt-3 block text-sm font-bold text-blue-700">{text('No invitation yet? Request staff access', 'invitation नहीं है? स्टाफ एक्सेस अनुरोध करें')}</Link></div>}
        {staff && <label className="flex items-start gap-3 text-sm"><input type="checkbox" checked={existingAccount} onChange={e => { setExistingAccount(e.target.checked); update('password', ''); setError('') }} />{text('I already have a SmartCare account for this mobile number. Activate staff access using my existing password.', 'इस मोबाइल नंबर से मेरा SmartCare अकाउंट पहले से है। मौजूदा पासवर्ड से स्टाफ एक्सेस चालू करें।')}</label>}
        {(!staff || !existingAccount) && <label className="block text-sm font-bold text-ink-950">{text('Full name', 'पूरा नाम')}<input required maxLength={120} autoComplete="name" value={form.name} onChange={(e) => update('name', e.target.value)} className={inputClass} /></label>}
        <label className="block text-sm font-bold text-ink-950">{text('Mobile number', 'मोबाइल नंबर')}<input required inputMode="tel" autoComplete="tel" pattern={htmlMobileNumberPattern} title="Use 8 to 15 digits, optionally starting with +" value={form.mobileNumber} onChange={(e) => update('mobileNumber', e.target.value)} className={inputClass} placeholder="+919876543210" /></label>
        {(!staff || !existingAccount) && <label className="block text-sm font-bold text-ink-950">{text('Email', 'ईमेल')} <span className="font-normal text-slate-400">{text('(optional)', '(वैकल्पिक)')}</span><input type="email" autoComplete="email" value={form.email} onChange={(e) => update('email', e.target.value)} className={inputClass} /></label>}
        {!staff && <div className="grid gap-4 sm:grid-cols-2"><label className="block text-sm font-bold text-ink-950">{text('Date of birth', 'जन्म तिथि')}<input type="date" max={localDateString()} value={form.dateOfBirth} onChange={(e) => update('dateOfBirth', e.target.value)} className={inputClass} /></label><label className="block text-sm font-bold text-ink-950">{text('Language', 'भाषा')}<select value={form.preferredLanguage} onChange={(e) => update('preferredLanguage', e.target.value)} className={inputClass}><option value="en">English</option><option value="hi">हिन्दी</option></select></label></div>}
        {!staff && <label className="block text-sm font-bold text-ink-950">{text('Emergency contact mobile number', 'आपातकालीन संपर्क का मोबाइल नंबर')} <span className="font-normal text-slate-400">{text('(optional)', '(वैकल्पिक)')}</span><input inputMode="tel" autoComplete="tel" pattern={htmlMobileNumberPattern} title="Enter a mobile number, not the contact person's name" value={form.emergencyContact} onChange={(e) => update('emergencyContact', e.target.value)} className={inputClass} placeholder="+919876543210" /><span className="mt-1.5 block text-xs font-normal text-slate-500">{text("Enter the number SmartCare should call in an emergency, not the person's name.", 'आपातकाल में संपर्क करने का मोबाइल नंबर दें, व्यक्ति का नाम नहीं।')}</span></label>}
        <label className="block text-sm font-bold text-ink-950">{staff && existingAccount ? text('Current password', 'मौजूदा पासवर्ड') : text('Password', 'पासवर्ड')}<input required type="password" minLength={staff && existingAccount ? 1 : 10} maxLength={72} autoComplete={staff && existingAccount ? 'current-password' : 'new-password'} value={form.password} onChange={(e) => update('password', e.target.value)} className={inputClass} /><span className="mt-1.5 block text-xs font-normal text-slate-500">{staff && existingAccount ? text('Enter your current SmartCare password. Your patient records and existing password are preserved.', 'मौजूदा SmartCare पासवर्ड दें। मरीज़ के रिकॉर्ड और पासवर्ड सुरक्षित रहेंगे।') : text('Use 10–72 characters. Do not reuse a banking or email password.', '10–72 अक्षर रखें। बैंक या ईमेल वाला पासवर्ड दोबारा इस्तेमाल न करें।')}</span></label>
        {error && <p role="alert" className="rounded-2xl bg-rose-50 p-3 text-sm font-semibold text-rose-800">{error}</p>}
        <button disabled={loading} className="flex h-13 w-full items-center justify-center gap-2 rounded-2xl bg-care-600 px-5 font-extrabold text-white hover:bg-care-700 disabled:opacity-60">{loading && <LoaderCircle className="size-5 animate-spin" />} {staff ? text('Activate staff account', 'स्टाफ अकाउंट चालू करें') : text('Create account', 'अकाउंट बनाएँ')}</button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-600">{text('Already registered?', 'पहले से रजिस्टर्ड हैं?')} <Link to="/login" className="font-extrabold text-care-700 hover:underline">{text('Sign in', 'लॉगिन करें')}</Link></p>
    </AuthShell>
  )
}
