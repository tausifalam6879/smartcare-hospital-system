import { useState } from 'react'
import { useLanguage } from '../context/LanguageContext'
import { worklistHindi, statusHindi } from '../i18n/worklist'
import { IdentityAvatar } from './IdentityAvatar'
import { appointmentStatusLabel } from '../services/appointments'
import type { OperationsQueueRow } from '../services/operations'

type Props = {
  rows: OperationsQueueRow[]; date: string; today: string; saving: boolean
  canManage: boolean; canConfirmCash: boolean
  onAction: (action: 'cash' | 'check-in', id: string) => void
  onNoShow: (id: string) => void
}

export function OperationsWorklist({ rows, date, today, saving, canManage, canConfirmCash, onAction, onNoShow }: Props) {
  const { text } = useLanguage()
  const t = (english: string) => text(english, worklistHindi[english] ?? english)
  const [search, setSearch] = useState('')
  const [doctor, setDoctor] = useState('')
  const [payment, setPayment] = useState('')
  const [status, setStatus] = useState('')
  const doctors = [...new Map(rows.map(row => [row.doctorId, row.doctorName])).entries()]
  const query = search.trim().toLowerCase()
  const visible = rows.filter(row => (!doctor || row.doctorId === doctor)
    && (!payment || row.paymentMethod === payment) && (!status || row.status === status)
    && (!query || row.patientNumber.toLowerCase().includes(query) || String(row.queuePosition ?? '') === query || row.doctorName.toLowerCase().includes(query)))
  const field = 'mt-1 w-full rounded-xl border border-blue-100 bg-white px-3 py-2 text-sm text-slate-700'
  function nextStep(row: OperationsQueueRow) {
    if (row.status === 'CASH_PENDING') return t("Awaiting cashier confirmation")
    if (row.status === 'RESERVED_PENDING_PAYMENT') return t("Awaiting online payment verification")
    if (row.status === 'CONFIRMED') return date > today ? text(`Check-in opens on ${date}`, `चेक-इन ${date} को खुलेगा`) : date === today ? t("Reception can check in this patient") : t("Reception can review missed visit")
    if (row.status === 'CHECKED_IN') return t("Waiting for doctor to call")
    if (row.status === 'IN_CONSULTATION') return t("With doctor — consultation in progress")
    if (row.status === 'WAITLISTED') return t("Waiting for an available place")
    return t("No desk action required")
  }
  return <section id="patient-queue" className="mt-8 overflow-hidden rounded-3xl border border-blue-100 bg-white shadow-sm">
    <div className="border-b border-slate-100 p-5">
      <p className="text-xs font-extrabold uppercase tracking-wider text-blue-700">{t("Live worklist")}</p>
      <h2 className="mt-1 text-xl font-black text-ink-950">{t("Patient queue")}</h2>
      <p className="mt-1 text-xs text-slate-500">{t("Patient IDs protect identities in this shared view. Payment method is not a payment receipt.")}</p>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <label className="text-xs font-bold">{t("Search patient / OPD")}<input className={field} value={search} onChange={e => setSearch(e.target.value)} placeholder={t("Patient ID, OPD number or doctor")} /></label>
        <label className="text-xs font-bold">{t("Filter doctor")}<select className={field} value={doctor} onChange={e => setDoctor(e.target.value)}><option value="">{t("All doctors")}</option>{doctors.map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select></label>
        <label className="text-xs font-bold">{t("Payment method")}<select className={field} value={payment} onChange={e => setPayment(e.target.value)}><option value="">{t("All methods")}</option><option value="CASH">{t("Cash")}</option><option value="ONLINE">{t("Online")}</option></select></label>
        <label className="text-xs font-bold">{t("Booking status")}<select className={field} value={status} onChange={e => setStatus(e.target.value)}><option value="">{t("All statuses")}</option>{Object.entries(appointmentStatusLabel).map(([value, label]) => <option key={value} value={value}>{text(label, statusHindi[value as keyof typeof statusHindi])}</option>)}</select></label>
      </div>
      <div className="mt-3 flex items-center justify-between text-xs"><span aria-live="polite">{text(`Showing ${visible.length} of ${rows.length} records`, `${rows.length} में से ${visible.length} रिकॉर्ड`)}</span><button className="font-bold text-blue-700" onClick={() => { setSearch(''); setDoctor(''); setPayment(''); setStatus('') }}>{t("Clear filters")}</button></div>
    </div>
    <div className="overflow-x-auto"><table className="w-full min-w-[760px] text-left text-sm">
      <thead className="bg-blue-50 text-xs text-slate-600"><tr>{['OPD', t("Patient"), t("Doctor"), t("Status"), t("Payment method"), t("Next step")].map(title => <th key={title} className="px-4 py-3">{title}</th>)}</tr></thead>
      <tbody className="divide-y divide-slate-100">{visible.map(row => <tr key={row.appointmentId} className="hover:bg-blue-50/40">
        <td className="px-4 py-4 font-black text-blue-700">{row.queuePosition ?? '—'}</td>
        <td className="px-4 py-4"><div className="flex items-center gap-2"><IdentityAvatar size="sm" /><span className="font-mono text-xs">{row.patientNumber}</span></div></td>
        <td className="px-4 py-4 font-bold">{row.doctorName}</td>
        <td className="px-4 py-4"><span className="rounded-lg bg-blue-50 px-2 py-1 text-xs font-bold text-blue-800">{text(appointmentStatusLabel[row.status], statusHindi[row.status])}</span></td>
        <td className="px-4 py-4">{row.paymentMethod === 'CASH' ? t("Cash") : t("Online")}</td>
        <td className="px-4 py-4 text-xs">{row.status === 'CASH_PENDING' && canConfirmCash
          ? <button disabled={saving} className="rounded-lg bg-blue-700 px-3 py-2 font-bold text-white disabled:opacity-50" onClick={() => onAction('cash', row.appointmentId)}>{t("Confirm cash")}</button>
          : row.status === 'CONFIRMED' && canManage && date <= today
            ? <button disabled={saving} className="rounded-lg border border-blue-200 px-3 py-2 font-bold text-blue-700 disabled:opacity-50" onClick={() => date === today ? onAction('check-in', row.appointmentId) : onNoShow(row.appointmentId)}>{date === today ? t("Check in") : t("Mark no-show")}</button>
            : <span className="text-slate-500">{nextStep(row)}</span>}</td>
      </tr>)}{visible.length === 0 && <tr><td colSpan={6} className="px-4 py-10 text-center text-slate-500">{rows.length ? t("No matching appointments. Clear filters to see the full queue.") : t("No appointments for this view.")}</td></tr>}</tbody>
    </table></div>
  </section>
}
