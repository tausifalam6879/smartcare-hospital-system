import { ArrowUpRight, type LucideIcon } from 'lucide-react'
import { Link } from 'react-router-dom'

type Props = {
  title: string
  description: string
  to?: string
  icon: LucideIcon
  tone?: 'care' | 'blue' | 'amber' | 'rose'
  status?: string
}

const tones = {
  care: 'bg-emerald-50 text-emerald-700',
  blue: 'bg-blue-50 text-blue-700',
  amber: 'bg-amber-50 text-amber-700',
  rose: 'bg-rose-50 text-rose-700',
}

export function ActionCard({ title, description, to, icon: Icon, tone = 'care', status }: Props) {
  const content = (
    <>
      <div className="flex items-start justify-between gap-4">
        <span className={`grid size-12 place-items-center rounded-2xl ${tones[tone]}`}><Icon className="size-6" /></span>
        {status ? <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-bold text-slate-500">{status}</span> : <ArrowUpRight className="size-5 text-slate-300 transition group-hover:text-care-600" />}
      </div>
      <h3 className="mt-5 text-lg font-black tracking-tight text-ink-950">{title}</h3>
      <p className="mt-1.5 text-sm leading-6 text-slate-600">{description}</p>
    </>
  )
  const className = "group min-h-48 rounded-2xl border border-slate-200 bg-[#fbfdff] p-5 text-left shadow-[0_8px_30px_-24px_rgba(9,45,87,.4)] transition hover:-translate-y-1 hover:border-care-200 hover:bg-white hover:shadow-soft"
  return to ? <Link to={to} className={className}>{content}</Link> : <div className={className}>{content}</div>
}
