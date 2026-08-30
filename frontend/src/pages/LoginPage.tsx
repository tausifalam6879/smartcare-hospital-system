import { Eye, EyeOff, LoaderCircle } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { AuthShell } from '../components/AuthShell'
import { useAuth } from '../context/AuthContext'
import { messageFromError } from '../services/api'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [credential, setCredential] = useState('')
  const [password, setPassword] = useState('')
  const [visible, setVisible] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await login(credential, password)
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

  return (
    <AuthShell title="Welcome back" subtitle="Use your registered mobile number or email.">
      {location.state === 'registered' && <div className="mb-4 rounded-2xl bg-care-50 p-4 text-sm font-semibold text-care-900">Account created. You are now signed in.</div>}
      <form onSubmit={submit} className="space-y-5">
        <label className="block text-sm font-bold text-ink-950">Mobile number or email<input autoComplete="username" required value={credential} onChange={(event) => setCredential(event.target.value)} className="mt-2 h-13 w-full rounded-2xl border border-slate-300 bg-white px-4 font-normal" placeholder="+91 98765 43210" /></label>
        <label className="block text-sm font-bold text-ink-950">Password<div className="relative mt-2"><input type={visible ? 'text' : 'password'} autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} className="h-13 w-full rounded-2xl border border-slate-300 bg-white px-4 pr-12 font-normal" /><button type="button" onClick={() => setVisible(!visible)} className="absolute right-2 top-2 grid size-9 place-items-center rounded-xl text-slate-500" aria-label={visible ? 'Hide password' : 'Show password'}>{visible ? <EyeOff className="size-5" /> : <Eye className="size-5" />}</button></div></label>
        {error && <p role="alert" className="rounded-2xl bg-rose-50 p-3 text-sm font-semibold text-rose-800">{error}</p>}
        <button disabled={loading} className="flex h-13 w-full items-center justify-center gap-2 rounded-2xl bg-care-600 px-5 font-extrabold text-white hover:bg-care-700 disabled:opacity-60">{loading && <LoaderCircle className="size-5 animate-spin" />} Sign in</button>
      </form>
      <p className="mt-6 text-center text-sm text-slate-600">New to RaahMediQ Health? <Link to="/register" className="font-extrabold text-care-700 hover:underline">Create an account</Link></p>
    </AuthShell>
  )
}
