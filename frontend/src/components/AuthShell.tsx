import { ShieldCheck } from 'lucide-react'
import type { ReactNode } from 'react'
import { BrandMark } from './BrandMark'

export function AuthShell({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="mx-auto grid min-h-[calc(100vh-4.5rem)] max-w-6xl items-center gap-10 px-4 py-10 sm:px-6 lg:grid-cols-2 lg:px-8">
      <div className="hidden rounded-[2rem] bg-ink-950 p-10 text-white lg:block">
        <BrandMark />
        <h2 className="mt-16 text-4xl font-black leading-tight tracking-tight">Your care journey belongs to you.</h2>
        <p className="mt-5 leading-7 text-slate-300">Sign in to access only your own appointments and care information. Staff permissions stay separated by role.</p>
        <div className="mt-12 flex items-start gap-3 rounded-2xl bg-white/10 p-4"><ShieldCheck className="mt-0.5 size-5 shrink-0 text-care-300" /><p className="text-sm leading-6 text-slate-200">Passwords are BCrypt hashed. Access tokens expire quickly and are kept only for this browser session.</p></div>
      </div>
      <div className="mx-auto w-full max-w-md">
        <p className="text-xs font-extrabold uppercase tracking-[.2em] text-care-700">SmartCare patient access</p>
        <h1 className="mt-2 text-3xl font-black tracking-tight text-ink-950">{title}</h1>
        <p className="mt-2 text-sm leading-6 text-slate-600">{subtitle}</p>
        <div className="mt-7">{children}</div>
      </div>
    </div>
  )
}
