import { Eye, EyeOff, LoaderCircle } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AuthShell } from '../components/AuthShell'
import { AccountTypeSelect } from '../components/AccountTypeSelect'
import { useLanguage } from '../context/LanguageContext'
import { isStaticDemo } from '../config/runtime'
import { useAuth } from '../context/AuthContext'
import { messageFromError } from '../services/api'

export function LoginPage() {
  const { text } = useLanguage()
  const [accountType, setAccountType] = useState('AUTO')
  const { login, session } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [credential, setCredential] = useState('')
  const [password, setPassword] = useState('')
  const [visible, setVisible] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  if (session) return <Navigate to="/dashboard" replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await login(credential, password, accountType)
      const requestedPath = typeof location.state === 'object' && location.state && 'from' in location.state
        ? String(location.state.from)
        : '/dashboard'
      navigate(requestedPath, { replace: true })
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setLoading(false)
    }
  }

  async function openDemo() {
    setLoading(true)
    setError('')
    try {
      await login('demo@smartcare.health', 'github-pages-demo')
      navigate('/dashboard', { replace: true })
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell title={text('Welcome back', 'वापस स्वागत है')} subtitle={text('Use your registered mobile number or email.', 'अपने रजिस्टर्ड मोबाइल नंबर या ईमेल से लॉगिन करें।')}>
      {isStaticDemo && <div className="mb-5 rounded-2xl border border-cyan-200 bg-cyan-50 p-4 text-sm text-cyan-950"><strong>GitHub Pages prototype</strong><p className="mt-1 text-xs leading-5 text-cyan-800">Explore with sample browser-only data. No real account, hospital, payment or medical service is contacted.</p><button type="button" disabled={loading} onClick={() => void openDemo()} className="mt-3 flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-cyan-700 px-4 font-extrabold text-white hover:bg-cyan-800 disabled:opacity-60">{loading && <LoaderCircle className="size-4 animate-spin" />} Explore demo</button></div>}
      {location.state === 'registered' && <div className="mb-4 rounded-2xl bg-care-50 p-4 text-sm font-semibold text-care-900">Account created. You are now signed in.</div>}
      <form onSubmit={submit} className="space-y-5">
        <AccountTypeSelect value={accountType} onChange={setAccountType} login />
        <p className="rounded-xl bg-blue-50 p-3 text-xs leading-5 text-blue-950">{text('Staff: activate your invitation once, then sign in with your own password. Permissions come from the hospital account, not this selection.', 'स्टाफ: invitation से एक बार अकाउंट चालू करें, फिर अपने पासवर्ड से लॉगिन करें। अधिकार अस्पताल के अकाउंट से मिलते हैं, इस चयन से नहीं।')}</p>
        <label className="block text-sm font-bold text-ink-950">{text('Mobile number or email', 'मोबाइल नंबर या ईमेल')}<input autoComplete="username" required value={credential} onChange={(event) => setCredential(event.target.value)} className="mt-2 h-13 w-full rounded-2xl border border-slate-300 bg-white px-4 font-normal" placeholder="+91 98765 43210" /></label>
        <label className="block text-sm font-bold text-ink-950">{text('Password', 'पासवर्ड')}<div className="relative mt-2"><input type={visible ? 'text' : 'password'} autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} className="h-13 w-full rounded-2xl border border-slate-300 bg-white px-4 pr-12 font-normal" /><button type="button" onClick={() => setVisible(!visible)} className="absolute right-2 top-2 grid size-9 place-items-center rounded-xl text-slate-500" aria-label={visible ? text('Hide password', 'पासवर्ड छिपाएँ') : text('Show password', 'पासवर्ड दिखाएँ')}>{visible ? <EyeOff className="size-5" /> : <Eye className="size-5" />}</button></div></label>
        {error && <p role="alert" className="rounded-2xl bg-rose-50 p-3 text-sm font-semibold text-rose-800">{error}</p>}
        <button disabled={loading} className="flex h-13 w-full items-center justify-center gap-2 rounded-2xl bg-care-600 px-5 font-extrabold text-white hover:bg-care-700 disabled:opacity-60">{loading && <LoaderCircle className="size-5 animate-spin" />} {text('Sign in', 'लॉगिन करें')}</button>
      </form>
      <Link to="/recover" className="mt-4 block text-center text-sm font-semibold text-blue-700">{text('Forgot password?', 'पासवर्ड भूल गए?')}</Link>
      <p className="mt-6 text-center text-sm text-slate-600">{text('New to SmartCare?', 'SmartCare पर नए हैं?')} <Link to="/register" className="font-extrabold text-care-700 hover:underline">{text('Create an account', 'अकाउंट बनाएँ')}</Link></p>
    </AuthShell>
  )
}
