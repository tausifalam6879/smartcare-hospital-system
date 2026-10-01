import { CalendarCheck2, CheckCircle2, Clock3, LoaderCircle, Pill, ShieldCheck, Stethoscope } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useLanguage } from '../context/LanguageContext'
import { isStaticDemo } from '../config/runtime'
import { messageFromError } from '../services/api'
import { getMyFollowUps, updateFollowUpStatus, type CareFollowUp, type FollowUpStatus } from '../services/followUps'

const labels: Record<FollowUpStatus, string> = { SCHEDULED: 'Scheduled', CONFIRMED: 'Confirmed', COMPLETED: 'Completed', MISSED: 'Missed' }
const styles: Record<FollowUpStatus, string> = { SCHEDULED: 'bg-blue-50 text-blue-800', CONFIRMED: 'bg-violet-50 text-violet-800', COMPLETED: 'bg-emerald-50 text-emerald-800', MISSED: 'bg-rose-50 text-rose-800' }
function displayDate(value: string, locale: string) { return new Intl.DateTimeFormat(locale, { dateStyle: 'long' }).format(new Date(`${value}T12:00:00`)) }

export function CareFollowUpsPage() {
  const { text, language } = useLanguage()
  const hindi: Record<string, string> = {
  "Continuity of care": "इलाज की निरंतरता",
  "Follow-ups & medication reminders": "फ़ॉलो-अप और दवा रिमाइंडर",
  "See doctor-scheduled follow-ups, confirm attendance and open the doctor-issued prescription schedule after discharge.": "डॉक्टर द्वारा निर्धारित फ़ॉलो-अप देखें, आने की पुष्टि करें और दवा का पर्चा खोलें।",
  "Loading follow-ups…": "फ़ॉलो-अप लोड हो रहे हैं…",
  "No follow-up scheduled": "कोई फ़ॉलो-अप निर्धारित नहीं है",
  "A follow-up appears here only after your treating doctor finalizes a visit and selects a date.": "डॉक्टर द्वारा विज़िट पूरी करके तारीख चुनने के बाद फ़ॉलो-अप यहाँ दिखाई देगा।",
  "Open health record →": "स्वास्थ्य रिकॉर्ड खोलें →",
  "Medication reminder active": "दवा रिमाइंडर चालू है",
  "Follow only the doctor-issued prescription. Do not change dose, frequency or duration without clinical advice.": "डॉक्टर के पर्चे का पालन करें। चिकित्सकीय सलाह के बिना दवा की मात्रा, आवृत्ति या अवधि न बदलें।",
  "Open prescription schedule": "दवा का पर्चा खोलें",
  "Confirm visit": "आने की पुष्टि करें",
  "Mark completed": "पूरा हुआ दर्ज करें",
  "Mark missed": "छूटा हुआ दर्ज करें",
  "Reminder only:": "केवल रिमाइंडर:",
  "SmartCare supports continuity and adherence; it does not prescribe medicine, change a treatment plan or guarantee prevention of readmission.": "SmartCare इलाज जारी रखने में सहायता करता है; यह दवा नहीं लिखता, इलाज नहीं बदलता और दोबारा भर्ती से बचाव की गारंटी नहीं देता।",
  "Scheduled": "निर्धारित",
  "Confirmed": "पुष्ट",
  "Completed": "पूरा हुआ",
  "Missed": "छूटा हुआ"
}
  const t = (value: string) => text(value, hindi[value] ?? value)
  const date = (value: string) => displayDate(value, language === 'hi' ? 'hi-IN' : 'en-IN')
  const [items, setItems] = useState<CareFollowUp[]>([]), [loading, setLoading] = useState(true)
  const [updating, setUpdating] = useState(''), [error, setError] = useState('')
  const [loadFailed, setLoadFailed] = useState(false)
  async function load() {
    setLoading(true); setLoadFailed(false); setError('')
    try { setItems(await getMyFollowUps()) }
    catch (cause) { setLoadFailed(true); setError(messageFromError(cause)) }
    finally { setLoading(false) }
  }
  useEffect(() => { if (isStaticDemo) { setLoading(false); return }; void load() }, [])
  async function change(item: CareFollowUp, status: FollowUpStatus) { setUpdating(item.id); setError(''); try { const updated = await updateFollowUpStatus(item.id, status); setItems((current) => current.map((entry) => entry.id === updated.id ? updated : entry)) } catch (cause) { setError(messageFromError(cause)) } finally { setUpdating('') } }
  return <div className="min-h-[70vh] bg-slate-50"><section className="border-b border-blue-100 bg-gradient-to-r from-blue-50 to-indigo-50 text-ink-950"><div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8"><p className="text-xs font-black uppercase tracking-[.2em] text-blue-700">{t("Continuity of care")}</p><h1 className="mt-3 text-4xl font-black tracking-tight">{t("Follow-ups & medication reminders")}</h1><p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600">{t("See doctor-scheduled follow-ups, confirm attendance and open the doctor-issued prescription schedule after discharge.")}</p></div></section><div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">{error && <p role="alert" className="mb-5 rounded-2xl bg-rose-50 p-4 text-sm font-bold text-rose-800">{error}</p>}{loading ? <div className="flex min-h-52 items-center justify-center gap-2 font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin" />{t("Loading follow-ups…")}</div> : loadFailed ? <div className="rounded-3xl border border-rose-200 bg-white p-8 text-center"><h2 className="text-xl font-bold">{text('Unable to load follow-ups', 'फ़ॉलो-अप लोड नहीं हो सके')}</h2><p className="mt-2 text-sm text-slate-600">{text('Your records may still be available. Please try again.', 'आपके रिकॉर्ड उपलब्ध हो सकते हैं। कृपया दोबारा कोशिश करें।')}</p><button onClick={() => void load()} className="mt-4 rounded-xl bg-blue-600 px-5 py-3 font-bold text-white">{text('Try again', 'दोबारा कोशिश करें')}</button></div> : items.length === 0 ? <div className="rounded-[2rem] border border-dashed border-slate-300 bg-white p-10 text-center"><CalendarCheck2 className="mx-auto size-10 text-slate-300" /><h2 className="mt-4 text-xl font-black text-ink-950">{t("No follow-up scheduled")}</h2><p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-slate-500">{t("A follow-up appears here only after your treating doctor finalizes a visit and selects a date.")}</p><Link to="/records" className="mt-5 inline-flex text-sm font-black text-care-700">{t("Open health record →")}</Link></div> : <div className="grid gap-5 lg:grid-cols-2">{items.map((item) => { const actionableDate = item.canClose === true; const terminal = item.status === 'COMPLETED' || item.status === 'MISSED'; return <article key={item.id} className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm"><div className="flex items-start justify-between gap-4"><div className="flex gap-3"><span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-violet-50 text-violet-700"><Stethoscope className="size-6" /></span><div><h2 className="font-black text-ink-950">{item.doctorName}</h2><p className="text-sm font-semibold text-care-700">{item.specialization} · {item.hospitalName}</p></div></div><span className={`rounded-full px-3 py-2 text-[11px] font-black ${styles[item.status]}`}>{item.overdue && !terminal ? text('Overdue · ', 'तारीख बीत गई · ') : ''}{t(labels[item.status])}</span></div><div className="mt-5 rounded-2xl bg-slate-50 p-4"><p className="flex items-center gap-2 font-black text-ink-950"><CalendarCheck2 className="size-5 text-violet-700" />{date(item.followUpDate)}</p><p className="mt-2 text-xs text-slate-500">{text('Original visit:', 'पिछली विज़िट:')} {date(item.visitDate)}</p>{item.instructions && <p className="mt-3 text-sm font-semibold leading-6 text-slate-700">{item.instructions}</p>}</div>{item.medicationReminderEnabled && <div className="mt-4 flex gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4"><Pill className="mt-0.5 size-5 shrink-0 text-emerald-700" /><div><p className="font-black text-emerald-950">{t("Medication reminder active")}</p><p className="mt-1 text-xs leading-5 text-emerald-800">{t("Follow only the doctor-issued prescription. Do not change dose, frequency or duration without clinical advice.")}</p><Link to="/records" className="mt-2 inline-flex text-xs font-black text-emerald-900 underline">{t("Open prescription schedule")}</Link></div></div>}{!terminal && <div className="mt-5 flex flex-wrap gap-2">{item.status !== 'CONFIRMED' && <button disabled={updating === item.id} onClick={() => void change(item, 'CONFIRMED')} className="rounded-xl bg-care-600 px-4 py-2.5 text-xs font-black text-white disabled:opacity-50">{t("Confirm visit")}</button>}{actionableDate && <button disabled={updating === item.id} onClick={() => void change(item, 'COMPLETED')} className="rounded-xl border border-emerald-300 px-4 py-2.5 text-xs font-black text-emerald-800 disabled:opacity-50"><CheckCircle2 className="mr-1.5 inline size-4" />{t("Mark completed")}</button>}{actionableDate && <button disabled={updating === item.id} onClick={() => void change(item, 'MISSED')} className="rounded-xl border border-slate-300 px-4 py-2.5 text-xs font-black text-slate-600 disabled:opacity-50"><Clock3 className="mr-1.5 inline size-4" />{t("Mark missed")}</button>}</div>}</article> })}</div>}<div className="mt-8 flex gap-3 rounded-3xl bg-amber-50 p-5 text-sm leading-6 text-amber-950"><ShieldCheck className="mt-0.5 size-5 shrink-0" /><p><strong>{t("Reminder only:")}</strong> {t("SmartCare supports continuity and adherence; it does not prescribe medicine, change a treatment plan or guarantee prevention of readmission.")}</p></div></div></div>
}
