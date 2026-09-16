import { Bell, BellRing, CheckCheck, Clock3, ExternalLink, LoaderCircle, TicketCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { messageFromError } from '../services/api'
import { getNotifications, markAllNotificationsRead, markNotificationRead, type CareNotification } from '../services/notifications'

function displayTime(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function relatedAction(item: CareNotification) {
  if (item.type.startsWith('DIAGNOSTIC_')) return { to: '/diagnostics', label: 'Open diagnostic results' }
  if (item.type.startsWith('BLOOD_GROUP_')) return { to: '/blood-group-analysis', label: 'Open blood-slide review' }
  if (item.type.startsWith('BLOOD_REQUEST_')) return { to: '/blood-support', label: 'Open blood support' }
  if (item.type.startsWith('AMBULANCE_')) return { to: '/ambulance', label: 'Track ambulance request' }
  if (item.type === 'FOLLOW_UP_SCHEDULED' || item.type === 'MEDICATION_REMINDER_CREATED') {
    return { to: '/follow-ups', label: 'Open follow-ups & reminders' }
  }
  if (item.type === 'QUEUE_POSITION_UPDATED' || item.type === 'NOW_SERVING' || item.type === 'CHECK_IN_CONFIRMED') {
    return item.appointmentId
      ? { to: `/queue/${item.appointmentId}`, label: 'Open live visit' }
      : { to: '/dashboard', label: 'Open My care' }
  }
  return { to: '/dashboard', label: 'Open My care' }
}

export function NotificationsPage() {
  const [items, setItems] = useState<CareNotification[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    getNotifications().then(setItems).catch((requestError) => setError(messageFromError(requestError))).finally(() => setLoading(false))
  }, [])

  async function markRead(item: CareNotification) {
    if (item.read) return
    try {
      const updated = await markNotificationRead(item.id)
      setItems((current) => current.map((value) => value.id === item.id ? updated : value))
    } catch (requestError) { setError(messageFromError(requestError)) }
  }

  async function markAll() {
    try {
      await markAllNotificationsRead()
      setItems((current) => current.map((item) => ({ ...item, read: true })))
    } catch (requestError) { setError(messageFromError(requestError)) }
  }

  return (
    <div className="mx-auto min-h-[65vh] max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-extrabold uppercase tracking-[.2em] text-care-700">Care updates</p><h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">Notifications</h1><p className="mt-2 text-slate-600">Booking, queue, diagnostics and continuity-of-care updates from SmartCare.</p></div>{items.some((item) => !item.read) && <button onClick={() => void markAll()} className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-extrabold text-slate-700"><CheckCheck className="size-4"/>Mark all read</button>}</div>
      {error && <p role="alert" className="mt-6 rounded-2xl bg-rose-50 p-4 text-sm font-semibold text-rose-800">{error}</p>}
      {loading ? <div className="mt-8 flex min-h-40 items-center justify-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin"/>Loading updates...</div> : items.length === 0 ? <div className="mt-8 rounded-3xl border border-dashed border-slate-300 bg-white p-12 text-center"><Bell className="mx-auto size-7 text-slate-300"/><h2 className="mt-4 font-black text-ink-950">No notifications yet</h2><p className="mt-2 text-sm text-slate-500">Your booking and care-workflow updates will appear here.</p></div> : <div className="mt-8 space-y-3">{items.map((item) => {
        const action = relatedAction(item)
        return <article key={item.id} onClick={() => void markRead(item)} className={`cursor-pointer rounded-3xl border p-5 transition sm:p-6 ${item.read ? 'border-slate-200 bg-white' : 'border-care-200 bg-care-50/70 shadow-sm'}`}><div className="flex gap-4"><span className={`grid size-11 shrink-0 place-items-center rounded-2xl ${item.type === 'NOW_SERVING' ? 'bg-emerald-500 text-white' : 'bg-white text-care-700 shadow-sm'}`}>{item.type.includes('QUEUE') || item.type === 'NOW_SERVING' ? <BellRing className="size-5"/> : <TicketCheck className="size-5"/>}</span><div className="min-w-0 flex-1"><div className="flex flex-wrap items-start justify-between gap-2"><h2 className="font-black text-ink-950">{item.title}</h2>{!item.read && <span className="rounded-full bg-care-600 px-2.5 py-1 text-[10px] font-black uppercase text-white">New</span>}</div><p className="mt-2 text-sm leading-6 text-slate-600">{item.message}</p><div className="mt-3 flex flex-wrap items-center gap-4 text-xs font-semibold text-slate-400"><span className="inline-flex items-center gap-1.5"><Clock3 className="size-3.5"/>{displayTime(item.createdAt)}</span><Link onClick={(event) => { event.stopPropagation(); void markRead(item) }} to={action.to} className="inline-flex items-center gap-1.5 font-extrabold text-care-700 hover:underline">{action.label}<ExternalLink className="size-3.5"/></Link></div></div></div></article>
      })}</div>}
    </div>
  )
}
