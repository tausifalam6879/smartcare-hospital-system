import { Activity, Ambulance, Bell, CalendarDays, Clock3, Droplets, FileHeart, FlaskConical, IndianRupee, LoaderCircle, LogOut, MapPin, Microscope, Navigation, ShieldCheck, Sparkles, Stethoscope, TicketCheck, Users, XCircle } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { isStaticDemo } from '../config/runtime'
import { useAuth } from '../context/AuthContext'
import { messageFromError } from '../services/api'
import {
  activeAppointmentStatuses,
  appointmentStatusLabel,
  cancellableAppointmentStatuses,
  cancelAppointment,
  getMyAppointments,
  type Appointment,
} from '../services/appointments'
import { getMyPayments, paymentStatusLabel, type Payment } from '../services/payments'
import { getPrototypeBookings, updatePrototypeBooking, type PrototypeBooking } from '../services/prototypeBookings'

function displayDate(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`))
}

export function DashboardPage() {
  const { session, logout } = useAuth()
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

  const nextAppointment = useMemo(() => appointments.find((appointment) =>
    activeAppointmentStatuses.includes(appointment.status)), [appointments])
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
    updatePrototypeBooking(id, { status: 'CANCELLED' })
    setPrototypeBookings(getPrototypeBookings())
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      {location.state === 'registered' && <div className="mb-6 flex gap-3 rounded-2xl border border-care-200 bg-care-50 p-4 text-sm text-care-900"><ShieldCheck className="size-5 shrink-0" /><span><strong>Your patient account is ready.</strong> Your private identifier is {session.user.patientNumber ?? 'being prepared'}.</span></div>}
      <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div><p className="text-xs font-extrabold uppercase tracking-[.2em] text-care-700">My care</p><h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">Namaste, {session.user.displayName.split(' ')[0]}</h1><p className="mt-2 text-slate-600">Your protected OPD numbers, waitlist status, and booking history in one place.</p></div>
        <button onClick={logout} className="flex h-11 items-center justify-center gap-2 rounded-xl border border-slate-300 px-4 text-sm font-bold text-slate-600 hover:bg-white"><LogOut className="size-4" /> Sign out</button>
      </div>

      {error && <p role="alert" className="mt-6 rounded-2xl bg-rose-50 p-4 text-sm font-semibold text-rose-800">{error}</p>}

      <div className="mt-8 grid gap-4 md:grid-cols-[1.4fr_.6fr]">
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          {loading ? <div className="flex min-h-36 items-center justify-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin" />Loading your care…</div> : nextAppointment ? (
            <>
              <div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-wider text-care-700">Next active booking</p><h2 className="mt-1 text-xl font-black text-ink-950">{nextAppointment.doctorName}</h2><p className="text-sm font-semibold text-slate-500">{nextAppointment.specialization}</p></div>{nextAppointment.queuePosition ? <div className="rounded-2xl bg-care-50 px-4 py-3 text-center"><p className="text-[10px] font-extrabold uppercase text-care-700">OPD no.</p><p className="text-3xl font-black text-care-800">{nextAppointment.queuePosition}</p></div> : <span className="grid size-12 place-items-center rounded-2xl bg-amber-50 text-amber-700"><Users className="size-6" /></span>}</div>
              <div className="mt-5 grid gap-3 text-sm text-slate-600 sm:grid-cols-2"><p className="flex gap-2"><CalendarDays className="size-4 shrink-0 text-slate-400" />{displayDate(nextAppointment.serviceDate)}</p><p className="flex gap-2"><TicketCheck className="size-4 shrink-0 text-slate-400" />{appointmentStatusLabel[nextAppointment.status]}</p><p className="flex gap-2"><MapPin className="size-4 shrink-0 text-slate-400" />{nextAppointment.hospitalName}</p><p className="flex gap-2"><Clock3 className="size-4 shrink-0 text-slate-400" />{nextAppointment.queuePosition ? `~${nextAppointment.estimatedWaitMinutes} min from queue start` : 'Promoted when capacity opens'}</p></div>
              <div className="mt-5 flex flex-wrap gap-3 border-t border-slate-100 pt-4">{['CONFIRMED', 'CHECKED_IN', 'IN_CONSULTATION'].includes(nextAppointment.status) ? <Link to={`/queue/${nextAppointment.id}`} className="rounded-xl bg-care-600 px-4 py-2.5 text-sm font-extrabold text-white">{nextAppointment.status === 'CONFIRMED' ? 'Check in & track queue' : 'Open live queue'}</Link> : <Link to="/booking" className="rounded-xl bg-care-600 px-4 py-2.5 text-sm font-extrabold text-white">Book another visit</Link>}<Link to={`/navigate?appointment=${nextAppointment.id}`} className="inline-flex items-center gap-2 rounded-xl border border-care-300 bg-care-50 px-4 py-2.5 text-sm font-extrabold text-care-800"><Navigation className="size-4" />Navigate to room</Link>{cancellableAppointmentStatuses.includes(nextAppointment.status) && <button disabled={cancellingId === nextAppointment.id} onClick={() => void cancel(nextAppointment.id)} className="flex items-center gap-2 rounded-xl border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-600 hover:bg-slate-50 disabled:opacity-50">{cancellingId === nextAppointment.id && <LoaderCircle className="size-4 animate-spin" />}Cancel</button>}</div>
            </>
          ) : (
            <><div className="flex items-center justify-between"><div><p className="text-xs font-bold uppercase tracking-wider text-slate-500">Next appointment</p><h2 className="mt-1 text-xl font-black text-ink-950">No active booking</h2></div><span className="grid size-12 place-items-center rounded-2xl bg-blue-50 text-blue-700"><CalendarDays className="size-6" /></span></div><p className="mt-4 text-sm leading-6 text-slate-600">Choose a doctor and SmartCare will check real day capacity before issuing a position or waitlist entry.</p><Link to="/booking" className="mt-5 inline-flex items-center gap-2 rounded-xl bg-care-600 px-4 py-2.5 text-sm font-extrabold text-white"><Stethoscope className="size-4" /> Book OPD number</Link></>
          )}
        </section>
        <section className="rounded-3xl bg-ink-950 p-6 text-white"><Bell className="size-6 text-care-300" /><p className="mt-5 text-xs font-bold uppercase tracking-wider text-care-300">Phase 4 live</p><h2 className="mt-1 text-xl font-black">Queue updates without names</h2><p className="mt-3 text-sm leading-6 text-slate-300">Check in securely, track the currently serving OPD number, and receive private care notifications.</p><Link to="/notifications" className="mt-5 inline-flex text-sm font-extrabold text-care-300 hover:text-white">Open notifications →</Link></section>
      </div>

      {prototypeBookings.length > 0 && <section className="mt-8 rounded-3xl border border-amber-200 bg-amber-50/60 p-5 sm:p-6">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-extrabold uppercase tracking-[.18em] text-amber-800">Project simulation bookings</p><h2 className="mt-1 text-2xl font-black text-ink-950">Directory OPD numbers</h2><p className="mt-1 text-xs leading-5 text-amber-900">Saved in this browser session only; no real hospital or payment provider is contacted.</p></div><Link to="/hospitals" className="text-sm font-extrabold text-care-700 hover:underline">Browse hospitals</Link></div>
        <div className="mt-5 grid gap-4 md:grid-cols-2">{prototypeBookings.map((booking) => <article key={booking.id} className="rounded-2xl border border-amber-200 bg-white p-5 shadow-sm">
          <div className="flex items-start justify-between gap-4"><div><h3 className="font-black text-ink-950">{booking.doctorName}</h3><p className="text-sm font-semibold text-care-700">{booking.departmentName}</p><p className="mt-1 text-xs text-slate-500">{booking.hospitalName} · {booking.hospitalLocation}</p></div><div className="rounded-xl bg-care-50 px-3 py-2 text-center"><p className="text-[9px] font-black uppercase text-care-700">OPD</p><p className="text-2xl font-black text-care-800">{booking.queuePosition}</p></div></div>
          <div className="mt-4 grid grid-cols-2 gap-3 text-xs text-slate-600"><p><span className="block text-slate-400">Visit date</span><strong>{displayDate(booking.serviceDate)}</strong></p><p><span className="block text-slate-400">Status</span><strong>{booking.status === 'CONFIRMED' ? 'Demo payment verified' : booking.status === 'PAYMENT_PENDING' ? 'Payment required' : booking.status === 'CASH_PENDING' ? 'Cash desk pending' : 'Cancelled'}</strong></p><p><span className="block text-slate-400">Payment</span><strong>{booking.paymentMethod === 'ONLINE' ? 'Online demo' : 'Cash at desk'}</strong></p><p><span className="block text-slate-400">Fee</span><strong>₹{booking.amount.toLocaleString('en-IN')}</strong></p></div>
          {booking.status === 'CANCELLED' && booking.paymentMethod === 'CASH' && <p className="mt-3 rounded-xl bg-slate-50 p-3 text-xs font-semibold leading-5 text-slate-700">No payment was collected, so no refund is due.</p>}
          {booking.status === 'CANCELLED' && booking.paymentMethod === 'ONLINE' && !booking.receiptNumber && <p className="mt-3 rounded-xl bg-slate-50 p-3 text-xs font-semibold leading-5 text-slate-700">The demo payment was not verified, so no refund is due.</p>}
          {booking.receiptNumber && <p className="mt-3 rounded-xl bg-emerald-50 p-3 font-mono text-[11px] text-emerald-800">Receipt {booking.receiptNumber}</p>}
          {!['CANCELLED'].includes(booking.status) && <div className="mt-4 flex flex-wrap gap-4"><Link to={`/navigate?prototypeBooking=${booking.id}`} className="inline-flex items-center gap-2 text-xs font-extrabold text-violet-700 hover:underline"><Navigation className="size-4" />Open demo indoor route</Link><button type="button" onClick={() => cancelPrototype(booking.id)} className="text-xs font-extrabold text-rose-700 hover:underline">Cancel project booking</button></div>}
        </article>)}</div>
      </section>}

      <section className="mt-8">
        <div className="flex items-center justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-[.18em] text-care-700">Booking history</p><h2 className="mt-1 text-2xl font-black text-ink-950">All OPD requests</h2></div><Link to="/doctors" className="text-sm font-extrabold text-care-700 hover:underline">Find a doctor</Link></div>
        {!loading && appointments.length === 0 ? <div className="mt-4 rounded-3xl border border-dashed border-slate-300 bg-white p-8 text-center text-sm text-slate-500">Your first booking will appear here.</div> : <div className="mt-4 grid gap-4 md:grid-cols-2">{appointments.map((appointment) => {
          const payment = paymentRecords.find((item) => item.appointmentId === appointment.id)
          return (
          <article key={appointment.id} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-start justify-between gap-4"><div><h3 className="font-black text-ink-950">{appointment.doctorName}</h3><p className="text-sm font-semibold text-care-700">{appointment.departmentName}</p></div><span className={`rounded-full px-3 py-1.5 text-[11px] font-extrabold ${activeAppointmentStatuses.includes(appointment.status) ? 'bg-care-50 text-care-800' : 'bg-slate-100 text-slate-600'}`}>{appointmentStatusLabel[appointment.status]}</span></div>
            <div className="mt-4 grid grid-cols-2 gap-3 text-xs text-slate-600"><p><span className="block text-slate-400">Visit date</span><strong>{displayDate(appointment.serviceDate)}</strong></p><p><span className="block text-slate-400">Queue</span><strong>{appointment.queuePosition ? `OPD ${appointment.queuePosition}` : 'Waitlist'}</strong></p><p><span className="block text-slate-400">Payment</span><strong>{appointment.paymentMethod === 'CASH' ? 'Cash at desk' : 'Online'}</strong></p><p><span className="block text-slate-400">Fee</span><strong className="inline-flex items-center"><IndianRupee className="size-3" />{appointment.amount.toLocaleString('en-IN')}</strong></p></div>
            {payment && <div className="mt-4 rounded-xl bg-slate-50 p-3 text-xs"><div className="flex items-center justify-between gap-2"><span className="font-extrabold text-slate-700">{paymentStatusLabel[payment.status]}</span>{payment.refundStatus && <span className="rounded-full bg-amber-100 px-2 py-1 font-bold text-amber-900">Refund {payment.refundStatus.toLowerCase()}</span>}</div>{payment.receiptNumber && <p className="mt-1 font-mono text-[11px] text-slate-500">Receipt {payment.receiptNumber}</p>}</div>}
            {['CONFIRMED', 'CHECKED_IN', 'IN_CONSULTATION'].includes(appointment.status) && <div className="mt-4 flex flex-wrap gap-4"><Link to={`/queue/${appointment.id}`} className="inline-flex items-center gap-2 text-xs font-extrabold text-care-700 hover:underline"><TicketCheck className="size-4" />Open live queue</Link><Link to={`/navigate?appointment=${appointment.id}`} className="inline-flex items-center gap-2 text-xs font-extrabold text-emerald-700 hover:underline"><Navigation className="size-4" />Navigate to room</Link></div>}
            {cancellableAppointmentStatuses.includes(appointment.status) && <button disabled={cancellingId === appointment.id} onClick={() => void cancel(appointment.id)} className="mt-4 flex items-center gap-2 text-xs font-extrabold text-rose-700 hover:underline"><XCircle className="size-4" />Cancel this booking</button>}
          </article>
        )})}</div>}
      </section>

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <Link to="/navigate" className="rounded-3xl border border-care-200 bg-care-50 p-5 transition hover:border-care-400"><MapPin className="size-5 text-care-700" /><h3 className="mt-4 font-extrabold text-ink-950">Hospital navigation</h3><p className="mt-1 text-xs font-bold text-care-700">Phase 5 · Available now</p></Link>
        <Link to="/records" className="rounded-3xl border border-emerald-200 bg-emerald-50 p-5 transition hover:border-emerald-400"><FileHeart className="size-5 text-emerald-700" /><h3 className="mt-4 font-extrabold text-ink-950">Private health record</h3><p className="mt-1 text-xs font-bold text-emerald-700">Phase 6 · Available now</p></Link>
        <Link to="/assistant" className="rounded-3xl border border-violet-200 bg-violet-50 p-5 transition hover:border-violet-400"><Sparkles className="size-5 text-violet-700" /><h3 className="mt-4 font-extrabold text-ink-950">Cited care assistant</h3><p className="mt-1 text-xs font-bold text-violet-700">Phase 7 · Available now</p></Link>
        <Link to="/diagnostics" className="rounded-3xl border border-cyan-200 bg-cyan-50 p-5 transition hover:border-cyan-400"><FlaskConical className="size-5 text-cyan-700" /><h3 className="mt-4 font-extrabold text-ink-950">Diagnostics & results</h3><p className="mt-1 text-xs font-bold text-cyan-700">Phase 8 · Available now</p></Link>
        <Link to="/blood-support" className="rounded-3xl border border-rose-200 bg-rose-50 p-5 transition hover:border-rose-400"><Droplets className="size-5 text-rose-700" /><h3 className="mt-4 font-extrabold text-ink-950">Verified blood support</h3><p className="mt-1 text-xs font-bold text-rose-700">Phase 9 · Available now</p></Link>
        <Link to="/ambulance" className="rounded-3xl border border-red-200 bg-red-50 p-5 transition hover:border-red-400"><Ambulance className="size-5 text-red-700" /><h3 className="mt-4 font-extrabold text-ink-950">Ambulance coordination</h3><p className="mt-1 text-xs font-bold text-red-700">Phase 10 · Available now</p></Link>
        <Link to="/operations" className="rounded-3xl border border-amber-200 bg-amber-50 p-5 transition hover:border-amber-400"><Activity className="size-5 text-amber-700" /><h3 className="mt-4 font-extrabold text-ink-950">Appointment recovery</h3><p className="mt-1 text-xs font-bold text-amber-700">Phase 11 · Patient approval required</p></Link>
        <Link to="/blood-group-analysis" className="rounded-3xl border border-violet-200 bg-violet-50 p-5 transition hover:border-violet-400"><Microscope className="size-5 text-violet-700" /><h3 className="mt-4 font-extrabold text-ink-950">Blood-slide review</h3><p className="mt-1 text-xs font-bold text-violet-700">Phase 12 · Experimental · Lab verification</p></Link>
      </div>
    </div>
  )
}
