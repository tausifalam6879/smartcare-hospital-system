import { Bell, CalendarDays, Clock3, IndianRupee, LoaderCircle, LogOut, MapPin, Navigation, ShieldCheck, Stethoscope, TicketCheck, Users, XCircle } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { isStaticDemo, publicAsset } from '../config/runtime'
import { useAuth } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
import { messageFromError } from '../services/api'
import {
  appointmentStatusLabel,
  cancelAppointment,
  getMyAppointments,
  type Appointment,
} from '../services/appointments'
import { getMyPayments, paymentStatusLabel, type Payment } from '../services/payments'
import { cancelPrototypeBooking, getPrototypeBookings, type PrototypeBooking } from '../services/prototypeBookings'
import { canOpenAppointmentQueue, canPatientCancelAppointment, findNextAppointment, isCurrentAppointment } from '../utils/appointmentLifecycle'

function displayDate(value: string, language = 'en') {
  return new Intl.DateTimeFormat(language === 'hi' ? 'hi-IN' : 'en-IN', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`))
}

const careGroups = [
  {
    title: 'Appointments & hospital visit',
    titleHi: 'अपॉइंटमेंट और अस्पताल यात्रा',
    description: 'Book a doctor, check your queue, and find the right room.',
    descriptionHi: 'डॉक्टर बुक करें, अपनी कतार देखें और सही कमरा खोजें।',
    image: 'images/smartcare-queue-workstation.png',
    imageAlt: 'OPD queue workstation showing a confirmed appointment',
    links: [['/booking', 'Book OPD', 'OPD बुक करें'], ['/doctors', 'Find a doctor', 'डॉक्टर खोजें'], ['/navigate', 'Find my way', 'रास्ता खोजें'], ['/operations', 'Booking changes', 'बुकिंग बदलाव']],
  },
  {
    title: 'Tests, reports & records',
    titleHi: 'जाँच, रिपोर्ट और रिकॉर्ड',
    description: 'See clinician-ordered tests and keep your health documents together.',
    descriptionHi: 'डॉक्टर द्वारा लिखी जाँच देखें और स्वास्थ्य दस्तावेज़ एक जगह रखें।',
    image: 'images/doctor-consultation.jpg',
    imageAlt: 'Doctor reviewing care information with a patient',
    links: [['/diagnostics', 'My tests', 'मेरी जाँच'], ['/records', 'Health records', 'स्वास्थ्य रिकॉर्ड'], ['/blood-group-analysis', 'Blood-slide review', 'ब्लड-स्लाइड जाँच']],
  },
  {
    title: 'Help & emergency',
    titleHi: 'सहायता और आपातकाल',
    description: 'Find blood support, request an ambulance, or ask for care guidance.',
    descriptionHi: 'रक्त सहायता खोजें, एम्बुलेंस माँगें या देखभाल संबंधी मार्गदर्शन लें।',
    image: 'images/hospital-lobby.jpg',
    imageAlt: 'Hospital reception and patient guidance area',
    links: [['/blood-support', 'Blood support', 'रक्त सहायता'], ['/ambulance', 'Ambulance', 'एम्बुलेंस'], ['/assistant', 'Care assistant', 'देखभाल सहायक']],
  },
  {
    title: 'After your visit',
    titleHi: 'आपकी यात्रा के बाद',
    description: 'Check doctor-planned follow-ups, reminders, and your visit history.',
    descriptionHi: 'डॉक्टर द्वारा तय फॉलो-अप, रिमाइंडर और यात्रा इतिहास देखें।',
    image: 'images/medical-centre.jpg',
    imageAlt: 'Medical centre for ongoing follow-up care',
    links: [['/follow-ups', 'Follow-ups & reminders', 'फॉलो-अप और रिमाइंडर'], ['/records', 'Visit notes', 'विज़िट नोट्स'], ['/notifications', 'Notifications', 'सूचनाएँ']],
  },
] as const

export function DashboardPage() {
  const { session, logout } = useAuth()
  const { language, text } = useLanguage()
  const location = useLocation()
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [paymentRecords, setPaymentRecords] = useState<Payment[]>([])
  const [prototypeBookings, setPrototypeBookings] = useState<PrototypeBooking[]>(() => getPrototypeBookings())
  const [loading, setLoading] = useState(true)
  const [cancellingId, setCancellingId] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (isStaticDemo) {
      setLoading(false)
      return
    }
    Promise.all([getMyAppointments(), getMyPayments()])
      .then(([appointmentData, paymentData]) => { setAppointments(appointmentData); setPaymentRecords(paymentData) })
      .catch((requestError) => setError(messageFromError(requestError)))
      .finally(() => setLoading(false))
  }, [])

  const nextAppointment = useMemo(() => findNextAppointment(appointments), [appointments])
  if (!session) return null

  async function cancel(id: string) {
    setCancellingId(id)
    setError('')
    try {
      const updated = await cancelAppointment(id)
      setAppointments((items) => items.map((item) => item.id === id ? updated : item))
      setPaymentRecords(await getMyPayments())
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setCancellingId('')
    }
  }

  function cancelPrototype(id: string) {
    cancelPrototypeBooking(id)
    setPrototypeBookings(getPrototypeBookings())
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      {location.state === 'registered' && <div className="mb-6 flex gap-3 rounded-2xl border border-care-200 bg-care-50 p-4 text-sm text-care-900"><ShieldCheck className="size-5 shrink-0" /><span><strong>Your patient account is ready.</strong> Your private identifier is {session.user.patientNumber ?? 'being prepared'}.</span></div>}
      <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div><p className="text-xs font-extrabold uppercase tracking-[.2em] text-care-700">{text('My care', 'मेरी देखभाल')}</p><h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">{text('Namaste', 'नमस्ते')}, {session.user.displayName.split(' ')[0]}</h1><p className="mt-2 text-slate-600">{text('Your next step and all your care services, in one place.', 'आपका अगला कदम और सभी देखभाल सेवाएँ, एक ही जगह।')}</p></div>
        <button type="button" onClick={logout} className="flex h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 text-sm font-bold text-slate-600 hover:bg-white"><LogOut className="size-4" /> {text('Sign out', 'साइन आउट')}</button>
      </div>

      {error && <p role="alert" className="mt-6 rounded-2xl bg-rose-50 p-4 text-sm font-semibold text-rose-800">{error}</p>}

      <div className="mt-8 grid gap-4 md:grid-cols-[1.4fr_.6fr]">
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          {loading ? <div className="flex min-h-36 items-center justify-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin" />Loading your care…</div> : nextAppointment ? (
            <>
              <div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-wider text-care-700">{text('Next active booking', 'अगली सक्रिय बुकिंग')}</p><h2 className="mt-1 text-xl font-black text-ink-950">{nextAppointment.doctorName}</h2><p className="text-sm font-semibold text-slate-500">{nextAppointment.specialization}</p></div>{nextAppointment.queuePosition ? <div className="rounded-2xl bg-care-50 px-4 py-3 text-center"><p className="text-[10px] font-extrabold uppercase text-care-700">{text('OPD no.', 'OPD नं.')}</p><p className="text-3xl font-black text-care-800">{nextAppointment.queuePosition}</p></div> : <span className="grid size-12 place-items-center rounded-2xl bg-amber-50 text-amber-700"><Users className="size-6" /></span>}</div>
              <div className="mt-5 grid gap-3 text-sm text-slate-600 sm:grid-cols-2"><p className="flex gap-2"><CalendarDays className="size-4 shrink-0 text-slate-400" />{displayDate(nextAppointment.serviceDate, language)}</p><p className="flex gap-2"><TicketCheck className="size-4 shrink-0 text-slate-400" />{appointmentStatusLabel[nextAppointment.status]}</p><p className="flex gap-2"><MapPin className="size-4 shrink-0 text-slate-400" />{nextAppointment.hospitalName}</p><p className="flex gap-2"><Clock3 className="size-4 shrink-0 text-slate-400" />{nextAppointment.queuePosition ? text(`~${nextAppointment.estimatedWaitMinutes} min from queue start`, `कतार शुरू होने से लगभग ${nextAppointment.estimatedWaitMinutes} मिनट`) : text('Promoted when capacity opens', 'जगह उपलब्ध होने पर पुष्टि होगी')}</p></div>
              <div className="mt-5 flex flex-wrap gap-3 border-t border-slate-100 pt-4">{canOpenAppointmentQueue(nextAppointment) ? <Link to={`/queue/${nextAppointment.id}`} className="rounded-xl bg-care-600 px-4 py-2.5 text-sm font-extrabold text-white">{nextAppointment.status === 'CONFIRMED' ? text('Check in & track queue', 'चेक-इन और कतार देखें') : text('Open live queue', 'लाइव कतार खोलें')}</Link> : <Link to="/booking" className="rounded-xl bg-care-600 px-4 py-2.5 text-sm font-extrabold text-white">{text('Book another visit', 'दूसरी विज़िट बुक करें')}</Link>}<Link to={`/navigate?appointment=${nextAppointment.id}`} className="inline-flex items-center gap-2 rounded-xl border border-care-300 bg-care-50 px-4 py-2.5 text-sm font-extrabold text-care-800"><Navigation className="size-4" />{text('Navigate to room', 'कमरे का रास्ता देखें')}</Link>{canPatientCancelAppointment(nextAppointment) && <button disabled={cancellingId === nextAppointment.id} onClick={() => void cancel(nextAppointment.id)} className="flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-600 hover:bg-slate-50 disabled:opacity-50">{cancellingId === nextAppointment.id && <LoaderCircle className="size-4 animate-spin" />}{text('Cancel', 'रद्द करें')}</button>}</div>
            </>
          ) : (
            <><div className="flex items-center justify-between"><div><p className="text-xs font-bold uppercase tracking-wider text-slate-500">{text('Next appointment', 'अगला अपॉइंटमेंट')}</p><h2 className="mt-1 text-xl font-black text-ink-950">{text('No active booking', 'कोई सक्रिय बुकिंग नहीं')}</h2></div><span className="grid size-12 place-items-center rounded-2xl bg-blue-50 text-blue-700"><CalendarDays className="size-6" /></span></div><p className="mt-4 text-sm leading-6 text-slate-600">{text('Choose a doctor and SmartCare will check real day capacity before issuing a position or waitlist entry.', 'डॉक्टर चुनें। SmartCare OPD नंबर या प्रतीक्षा सूची देने से पहले उस दिन की उपलब्धता जाँचेगा।')}</p><Link to="/booking" className="mt-5 inline-flex items-center gap-2 rounded-xl bg-care-600 px-4 py-2.5 text-sm font-extrabold text-white"><Stethoscope className="size-4" /> {text('Book OPD number', 'OPD नंबर बुक करें')}</Link></>
          )}
        </section>
        <section className="rounded-3xl bg-ink-950 p-6 text-white"><Bell className="size-6 text-care-300" /><p className="mt-5 text-xs font-bold uppercase tracking-wider text-care-300">{text('Stay informed', 'जानकारी पाते रहें')}</p><h2 className="mt-1 text-xl font-black">{text('Your care updates', 'आपकी देखभाल की सूचनाएँ')}</h2><p className="mt-3 text-sm leading-6 text-slate-300">{text('See private queue and appointment updates without sharing patient names.', 'रोगी का नाम साझा किए बिना निजी कतार और अपॉइंटमेंट अपडेट देखें।')}</p><Link to="/notifications" className="mt-5 inline-flex text-sm font-extrabold text-care-300 hover:text-white">{text('Open notifications', 'सूचनाएँ खोलें')} →</Link></section>
      </div>

      <section aria-labelledby="care-services-heading" className="mt-10">
        <div><p className="text-xs font-extrabold uppercase tracking-[.18em] text-care-700">{text('Explore my care', 'मेरी देखभाल देखें')}</p><h2 id="care-services-heading" className="mt-1 text-2xl font-black text-ink-950">{text('What would you like to do?', 'आप क्या करना चाहेंगे?')}</h2><p className="mt-2 text-sm text-slate-600">{text('Choose a service group. Every existing care feature is still available.', 'सेवा समूह चुनें। सभी मौजूदा सुविधाएँ उपलब्ध हैं।')}</p></div>
        <div className="mt-5 grid gap-5 md:grid-cols-2">
          {careGroups.map((group) => <article key={group.title} className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
            <img src={publicAsset(group.image)} alt={group.imageAlt} loading="lazy" className="h-36 w-full object-cover sm:h-40" />
            <div className="p-5 sm:p-6"><h3 className="text-xl font-black text-ink-950">{language === 'hi' ? group.titleHi : group.title}</h3><p className="mt-1 text-sm leading-6 text-slate-600">{language === 'hi' ? group.descriptionHi : group.description}</p><div className="mt-4 flex flex-wrap gap-2">{group.links.map(([to, label, labelHi]) => <Link key={`${group.title}-${label}`} to={to} className="inline-flex min-h-11 items-center rounded-xl border border-care-200 bg-care-50 px-3 text-sm font-bold text-care-800 transition hover:border-care-400 hover:bg-care-100">{language === 'hi' ? labelHi : label}</Link>)}</div></div>
          </article>)}
        </div>
      </section>

      {prototypeBookings.length > 0 && <section className="mt-8 rounded-3xl border border-amber-200 bg-amber-50/60 p-5 sm:p-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-extrabold uppercase tracking-[.18em] text-amber-800">Project simulation bookings</p><h2 className="mt-1 text-2xl font-black text-ink-950">Directory OPD numbers</h2><p className="mt-1 text-xs leading-5 text-amber-900">Saved in this browser session only; no real hospital or payment provider is contacted.</p></div><Link to="/hospitals" className="text-sm font-extrabold text-care-700 hover:underline">Browse hospitals</Link></div>
        <div className="mt-5 grid gap-4 md:grid-cols-2">{prototypeBookings.map((booking) => <article key={booking.id} className="rounded-2xl border border-amber-200 bg-white p-5 shadow-sm">
          <div className="flex items-start justify-between gap-4"><div><h3 className="font-black text-ink-950">{booking.doctorName}</h3><p className="text-sm font-semibold text-care-700">{booking.departmentName}</p><p className="mt-1 text-xs text-slate-500">{booking.hospitalName} · {booking.hospitalLocation}</p></div><div className="rounded-xl bg-care-50 px-3 py-2 text-center"><p className="text-[9px] font-black uppercase text-care-700">OPD</p><p className="text-2xl font-black text-care-800">{booking.queuePosition}</p></div></div>
          <div className="mt-4 grid grid-cols-2 gap-3 text-xs text-slate-600"><p><span className="block text-slate-400">Visit date</span><strong>{displayDate(booking.serviceDate)}</strong></p><p><span className="block text-slate-400">Status</span><strong>{booking.status === 'CONFIRMED' ? 'Demo payment verified' : booking.status === 'PAYMENT_PENDING' ? 'Payment required' : booking.status === 'CASH_PENDING' ? 'Cash desk pending' : 'Cancelled'}</strong></p><p><span className="block text-slate-400">Payment</span><strong>{booking.paymentMethod === 'ONLINE' ? 'Online demo' : 'Cash at desk'}</strong></p><p><span className="block text-slate-400">Fee</span><strong>₹{booking.amount.toLocaleString('en-IN')}</strong></p></div>
          {booking.status === 'CANCELLED' && booking.refundStatus === 'DEMO_REFUND_RECORDED' && <p className="mt-3 rounded-xl bg-blue-50 p-3 text-xs font-semibold leading-5 text-blue-900">Demo refund recorded · {booking.refundReference}. No real money was collected or transferred.</p>}
          {booking.status === 'CANCELLED' && booking.paymentMethod === 'CASH' && <p className="mt-3 rounded-xl bg-slate-50 p-3 text-xs font-semibold leading-5 text-slate-700">No payment was collected, so no refund is due.</p>}
          {booking.status === 'CANCELLED' && booking.paymentMethod === 'ONLINE' && booking.refundStatus !== 'DEMO_REFUND_RECORDED' && <p className="mt-3 rounded-xl bg-slate-50 p-3 text-xs font-semibold leading-5 text-slate-700">The demo payment was not verified, so no refund is due.</p>}
          {booking.receiptNumber && <p className="mt-3 rounded-xl bg-emerald-50 p-3 font-mono text-[11px] text-emerald-800">Receipt {booking.receiptNumber}</p>}
          {!['CANCELLED'].includes(booking.status) && <div className="mt-4 flex flex-wrap gap-4"><Link to={`/navigate?prototypeBooking=${booking.id}`} className="inline-flex items-center gap-2 text-xs font-extrabold text-violet-700 hover:underline"><Navigation className="size-4" />Open demo indoor route</Link><button type="button" onClick={() => cancelPrototype(booking.id)} className="text-xs font-extrabold text-rose-700 hover:underline">Cancel project booking</button></div>}
        </article>)}</div>
      </section>}

      <details className="group mt-10 rounded-3xl border border-slate-200 bg-slate-50/60 p-5 sm:p-6">
        <summary className="cursor-pointer list-none text-lg font-black text-ink-950 marker:hidden">{text('View past bookings', 'पुरानी बुकिंग देखें')} <span className="ml-2 text-sm font-semibold text-slate-500">({appointments.length})</span><span className="ml-2 text-sm text-care-700 group-open:hidden">{text('Show', 'दिखाएँ')} ↓</span><span className="ml-2 hidden text-sm text-care-700 group-open:inline">{text('Hide', 'छिपाएँ')} ↑</span></summary>
      <section className="mt-6">
        <div className="flex items-center justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-[.18em] text-care-700">Booking history</p><h2 className="mt-1 text-2xl font-black text-ink-950">All OPD requests</h2></div><Link to="/doctors" className="text-sm font-extrabold text-care-700 hover:underline">Find a doctor</Link></div>
        {!loading && appointments.length === 0 ? <div className="mt-4 rounded-3xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-500">Your first booking will appear here.</div> : <div className="mt-4 grid gap-4 md:grid-cols-2">{appointments.map((appointment) => {
          const payment = paymentRecords.find((item) => item.appointmentId === appointment.id)
          return (
          <article key={appointment.id} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between gap-4"><div><h3 className="font-black text-ink-950">{appointment.doctorName}</h3><p className="text-sm font-semibold text-care-700">{appointment.departmentName}</p></div><span className={`rounded-full px-3 py-1.5 text-[11px] font-extrabold ${isCurrentAppointment(appointment) ? 'bg-care-50 text-care-800' : 'bg-slate-100 text-slate-600'}`}>{appointmentStatusLabel[appointment.status]}</span></div>
            <div className="mt-4 grid grid-cols-2 gap-3 text-xs text-slate-600"><p><span className="block text-slate-400">Visit date</span><strong>{displayDate(appointment.serviceDate)}</strong></p><p><span className="block text-slate-400">Queue</span><strong>{appointment.queuePosition ? `OPD ${appointment.queuePosition}` : 'Waitlist'}</strong></p><p><span className="block text-slate-400">Payment</span><strong>{appointment.paymentMethod === 'CASH' ? 'Cash at desk' : 'Online'}</strong></p><p><span className="block text-slate-400">Fee</span><strong className="inline-flex items-center"><IndianRupee className="size-3" />{appointment.amount.toLocaleString('en-IN')}</strong></p></div>
            {payment && <div className="mt-4 rounded-xl bg-slate-50 p-3 text-xs"><div className="flex items-center justify-between gap-2"><span className="font-extrabold text-slate-700">{paymentStatusLabel[payment.status]}</span>{payment.refundStatus && <span className="rounded-full bg-amber-100 px-2 py-1 font-bold text-amber-900">Refund {payment.refundStatus.toLowerCase()}</span>}</div>{payment.receiptNumber && <p className="mt-1 font-mono text-[11px] text-slate-500">Receipt {payment.receiptNumber}</p>}</div>}
            {canOpenAppointmentQueue(appointment) && <div className="mt-4 flex flex-wrap gap-4"><Link to={`/queue/${appointment.id}`} className="inline-flex items-center gap-2 text-xs font-extrabold text-care-700 hover:underline"><TicketCheck className="size-4" />Open live queue</Link><Link to={`/navigate?appointment=${appointment.id}`} className="inline-flex items-center gap-2 text-xs font-extrabold text-emerald-700 hover:underline"><Navigation className="size-4" />Navigate to room</Link></div>}
            {canPatientCancelAppointment(appointment) && <button disabled={cancellingId === appointment.id} onClick={() => void cancel(appointment.id)} className="mt-4 flex items-center gap-2 text-xs font-extrabold text-rose-700 hover:underline"><XCircle className="size-4" />Cancel this booking</button>}
          </article>
        )})}</div>}
      </section>
      </details>
    </div>
  )
}
