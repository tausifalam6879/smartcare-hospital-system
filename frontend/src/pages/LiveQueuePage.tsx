import { ArrowLeft, BellRing, CheckCircle2, Clock3, LoaderCircle, MapPin, Navigation, RefreshCw, ShieldCheck, Stethoscope, TicketCheck, Users } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useLanguage } from '../context/LanguageContext'
import { statusHindi } from '../i18n/worklist'
import { appointmentStatusLabel } from '../services/appointments'
import { messageFromError } from '../services/api'
import { checkInAppointment, getPatientQueue, type PatientQueue } from '../services/queues'

function displayDate(value: string, language: string) {
  return new Intl.DateTimeFormat(language === 'hi' ? 'hi-IN' : 'en-IN', { dateStyle: 'full' }).format(new Date(`${value}T12:00:00`))
}

function queueHeadline(queue: PatientQueue) {
  if (queue.status === 'IN_CONSULTATION') return 'It is your turn'
  if (queue.status === 'COMPLETED') return 'Visit completed'
  if (queue.status === 'CHECKED_IN') return 'You are checked in'
  if (queue.status === 'CONFIRMED') return 'Appointment confirmed'
  return appointmentStatusLabel[queue.status]
}

export function LiveQueuePage() {
  const { appointmentId = '' } = useParams()
  const { language, text: t } = useLanguage()
  const [queue, setQueue] = useState<PatientQueue | null>(null)
  const [loading, setLoading] = useState(true)
  const [checkingIn, setCheckingIn] = useState(false)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [checkInError, setCheckInError] = useState('')

  const refresh = useCallback(async (quiet = false) => {
    if (!appointmentId) return
    if (!quiet) setRefreshing(true)
    try {
      setQueue(await getPatientQueue(appointmentId))
      setError('')
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [appointmentId])

  useEffect(() => {
    void refresh(true)
    const timer = window.setInterval(() => void refresh(true), 15_000)
    return () => window.clearInterval(timer)
  }, [refresh])

  async function checkIn() {
    setCheckingIn(true)
    setCheckInError('')
    try {
      await checkInAppointment(appointmentId)
      await refresh(true)
    } catch (requestError) {
      setCheckInError(messageFromError(requestError))
    } finally {
      setCheckingIn(false)
    }
  }

  const progress = useMemo(() => {
    if (!queue?.yourPosition) return 0
    if (queue.status === 'IN_CONSULTATION' || queue.status === 'COMPLETED') return 100
    if (!queue.currentlyServingPosition) return 8
    return Math.max(8, Math.min(96, (queue.currentlyServingPosition / queue.yourPosition) * 100))
  }, [queue])

  if (loading) return <div className="grid min-h-[60vh] place-items-center"><div className="flex items-center gap-3 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin" />{t("Loading live queue...", "लाइव कतार लोड हो रही है…")}</div></div>

  return (
    <div className="bg-[linear-gradient(180deg,#eef6ff_0%,#f8fafc_38%,#fff_100%)]">
      <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <Link to="/dashboard" className="inline-flex items-center gap-2 text-sm font-extrabold text-slate-600 hover:text-care-700"><ArrowLeft className="size-4" />{t("Back to My care", "मेरी देखभाल पर वापस जाएँ")}</Link>
          <button onClick={() => void refresh()} disabled={refreshing} className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-extrabold text-slate-700 shadow-sm disabled:opacity-60"><RefreshCw className={`size-4 ${refreshing ? 'animate-spin' : ''}`} />{t("Refresh now", "अभी अपडेट करें")}</button>
        </div>

        {error && <p role="alert" className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-800">{error}</p>}
        {checkInError && <p role="alert" className="mt-5 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-800">{checkInError}</p>}

        {queue && <>
          <section className="mt-6 overflow-hidden rounded-[2rem] bg-ink-950 text-white shadow-2xl shadow-blue-950/15">
            <div className="grid lg:grid-cols-[1.15fr_.85fr]">
              <div className="p-6 sm:p-9 lg:p-12">
                <div className="flex items-center gap-2 text-xs font-extrabold uppercase tracking-[.2em] text-care-300"><span className="relative flex size-2"><span className="absolute inline-flex size-full animate-ping rounded-full bg-emerald-400 opacity-75"/><span className="relative inline-flex size-2 rounded-full bg-emerald-400"/></span>{t("Live OPD queue", "लाइव ओपीडी कतार")}</div>
                <h1 className="mt-5 text-4xl font-black tracking-tight sm:text-5xl">{language === 'hi' ? ({ IN_CONSULTATION: 'अब आपकी बारी है', COMPLETED: 'विज़िट पूरी हुई', CHECKED_IN: 'आपका चेक-इन हो गया है', CONFIRMED: 'अपॉइंटमेंट की पुष्टि हो गई है' } as Record<string, string>)[queue.status] ?? statusHindi[queue.status] ?? queueHeadline(queue) : queueHeadline(queue)}</h1>
                <p className="mt-4 max-w-xl text-sm leading-6 text-blue-100/75">{queue.doctorName} · {queue.hospitalName}<br/>{displayDate(queue.serviceDate, language)}</p>
                {queue.status === 'CONFIRMED' && <button onClick={() => void checkIn()} disabled={checkingIn} className="mt-7 inline-flex min-h-12 items-center gap-2 rounded-xl bg-emerald-500 px-5 text-sm font-black text-white shadow-lg shadow-emerald-950/20 transition hover:bg-emerald-400 disabled:opacity-60">{checkingIn ? <LoaderCircle className="size-5 animate-spin" /> : <CheckCircle2 className="size-5" />}{t("Check in with mobile", "मोबाइल से चेक-इन करें")}</button>}
                {queue.privacyToken && <div className="mt-7 inline-flex items-center gap-3 rounded-2xl border border-white/10 bg-white/8 px-4 py-3"><ShieldCheck className="size-5 text-emerald-300" /><div><p className="text-[10px] font-bold uppercase tracking-wider text-blue-100/55">{t("Private display token", "निजी डिस्प्ले टोकन")}</p><p className="font-mono text-lg font-black">{queue.privacyToken}</p></div></div>}
              </div>
              <div className="border-t border-white/10 bg-care-700/25 p-6 sm:p-9 lg:border-l lg:border-t-0 lg:p-12">
                <p className="text-xs font-bold uppercase tracking-widest text-blue-100/60">{t("Your OPD number", "आपका ओपीडी नंबर")}</p>
                <p className="mt-2 text-8xl font-black tracking-tighter text-white">{queue.yourPosition ?? '—'}</p>
                <div className="mt-8 h-2 overflow-hidden rounded-full bg-white/10"><div className="h-full rounded-full bg-emerald-400 transition-all duration-700" style={{ width: `${progress}%` }}/></div>
                <p className="mt-3 text-xs font-semibold text-blue-100/60">{t("Queue movement indicator · automatically refreshes every 15 seconds", "कतार की प्रगति · हर 15 सेकंड में अपने आप अपडेट")}</p>
              </div>
            </div>
          </section>

          <section className="mt-5 grid gap-4 sm:grid-cols-3">
            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><TicketCheck className="size-5 text-care-700"/><p className="mt-5 text-xs font-bold uppercase tracking-wider text-slate-400">{t("Currently serving", "अभी किसकी बारी है")}</p><p className="mt-1 text-3xl font-black text-ink-950">{queue.currentlyServingPosition ?? t('Not started', 'अभी शुरू नहीं हुआ')}</p></div>
            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><Users className="size-5 text-violet-600"/><p className="mt-5 text-xs font-bold uppercase tracking-wider text-slate-400">{t("Approximately ahead", "आपसे पहले लगभग")}</p><p className="mt-1 text-3xl font-black text-ink-950">{queue.patientsAhead}</p></div>
            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><Clock3 className="size-5 text-emerald-600"/><p className="mt-5 text-xs font-bold uppercase tracking-wider text-slate-400">{t("Estimated wait", "अनुमानित प्रतीक्षा")}</p><p className="mt-1 text-3xl font-black text-ink-950">~{queue.estimatedWaitMinutes} <span className="text-base text-slate-400">{t("min", "मिनट")}</span></p></div>
          </section>

          <section className="mt-5 grid gap-4 lg:grid-cols-[1fr_.7fr]">
            <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"><div className="flex items-center gap-3"><span className="grid size-11 place-items-center rounded-2xl bg-care-50 text-care-700"><MapPin className="size-5"/></span><div><h2 className="font-black text-ink-950">{t("Where to go", "कहाँ जाना है")}</h2><p className="text-sm text-slate-500">{t("Confirmed hospital location", "अस्पताल में निर्धारित स्थान")}</p></div></div><p className="mt-5 text-sm font-bold text-slate-700">{[queue.building, queue.floorLabel, queue.roomNumber].filter(Boolean).join(' · ') || t('Room details will be provided by the hospital.', 'कमरे की जानकारी अस्पताल से मिलेगी।')}</p><Link to={`/navigate?appointment=${appointmentId}`} className="mt-5 inline-flex min-h-11 items-center gap-2 rounded-xl bg-care-600 px-4 text-sm font-black text-white"><Navigation className="size-4" />{t("Navigate to this room", "इस कमरे का रास्ता देखें")}</Link></div>
            <div className="rounded-3xl border border-amber-200 bg-amber-50 p-6"><BellRing className="size-5 text-amber-700"/><h2 className="mt-4 font-black text-amber-950">{t("Estimate, not a promise", "अनुमान है, गारंटी नहीं")}</h2><p className="mt-2 text-sm leading-6 text-amber-900/75">{queue.estimateNotice}</p></div>
          </section>

          <div className="mt-5 flex items-center gap-2 text-xs font-semibold text-slate-400"><Stethoscope className="size-4"/>{t("Patient names are never exposed on the public queue display.", "सार्वजनिक कतार डिस्प्ले पर मरीज़ों के नाम नहीं दिखाए जाते।")}</div>
        </>}
      </div>
    </div>
  )
}
