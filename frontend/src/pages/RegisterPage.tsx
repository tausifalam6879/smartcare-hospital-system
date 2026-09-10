import { LoaderCircle } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthShell } from '../components/AuthShell'
import { useAuth } from '../context/AuthContext'
import { messageFromError } from '../services/api'

const initial = { name: '', mobileNumber: '', email: '', password: '', dateOfBirth: '', emergencyContact: '', preferredLanguage: 'en' }
const mobileNumberPattern = /^\+?[1-9][0-9]{7,14}$/
const htmlMobileNumberPattern = '\\+?[1-9][0-9]{7,14}'

function normaliseMobileNumber(value: string) {
  return value.replace(/[\s()-]/g, '')
}

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(initial)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const update = (field: keyof typeof initial, value: string) => setForm((current) => ({ ...current, [field]: value }))

  async function submit(event: FormEvent) {
    event.preventDefault()
    const mobileNumber = normaliseMobileNumber(form.mobileNumber)
    const emergencyContact = normaliseMobileNumber(form.emergencyContact)
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
        name: form.name.trim(),
        mobileNumber,
        email: form.email.trim() || undefined,
        dateOfBirth: form.dateOfBirth || undefined,
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
    <AuthShell title="Create your patient account" subtitle="Only a mobile number, name, and secure password are required. Add other details when available.">
      <form onSubmit={submit} className="space-y-4">
        <label className="block text-sm font-bold text-ink-950">Full name<input required maxLength={120} autoComplete="name" value={form.name} onChange={(e) => update('name', e.target.value)} className={inputClass} /></label>
        <label className="block text-sm font-bold text-ink-950">Mobile number<input required inputMode="tel" autoComplete="tel" pattern={htmlMobileNumberPattern} title="Use 8 to 15 digits, optionally starting with +" value={form.mobileNumber} onChange={(e) => update('mobileNumber', e.target.value)} className={inputClass} placeholder="+919876543210" /></label>
        <label className="block text-sm font-bold text-ink-950">Email <span className="font-normal text-slate-400">(optional)</span><input type="email" autoComplete="email" value={form.email} onChange={(e) => update('email', e.target.value)} className={inputClass} /></label>
        <div className="grid gap-4 sm:grid-cols-2"><label className="block text-sm font-bold text-ink-950">Date of birth<input type="date" max={new Date().toISOString().slice(0, 10)} value={form.dateOfBirth} onChange={(e) => update('dateOfBirth', e.target.value)} className={inputClass} /></label><label className="block text-sm font-bold text-ink-950">Language<select value={form.preferredLanguage} onChange={(e) => update('preferredLanguage', e.target.value)} className={inputClass}><option value="en">English</option><option value="hi">हिन्दी</option></select></label></div>
        <label className="block text-sm font-bold text-ink-950">Emergency contact mobile number <span className="font-normal text-slate-400">(optional)</span><input inputMode="tel" autoComplete="tel" pattern={htmlMobileNumberPattern} title="Enter a mobile number, not the contact person's name" value={form.emergencyContact} onChange={(e) => update('emergencyContact', e.target.value)} className={inputClass} placeholder="+919876543210" /><span className="mt-1.5 block text-xs font-normal text-slate-500">Enter the number SmartCare should call in an emergency, not the person's name.</span></label>
        <label className="block text-sm font-bold text-ink-950">Password<input required type="password" minLength={10} maxLength={72} autoComplete="new-password" value={form.password} onChange={(e) => update('password', e.target.value)} className={inputClass} /><span className="mt-1.5 block text-xs font-normal text-slate-500">Use 10–72 characters. Do not reuse a banking or email password.</span></label>
        {error && <p role="alert" className="rounded-2xl bg-rose-50 p-3 text-sm font-semibold text-rose-800">{error}</p>}
        <button disabled={loading} className="flex h-13 w-full items-center justify-center gap-2 rounded-2xl bg-care-600 px-5 font-extrabold text-white hover:bg-care-700 disabled:opacity-60">{loading && <LoaderCircle className="size-5 animate-spin" />} Create account</button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-600">Already registered? <Link to="/login" className="font-extrabold text-care-700 hover:underline">Sign in</Link></p>
    </AuthShell>
  )
}
