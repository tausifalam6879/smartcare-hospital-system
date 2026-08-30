import {
  ArrowRight,
  Building2,
  CalendarDays,
  CheckCircle2,
  ChevronDown,
  Clock3,
  CreditCard,
  Hospital as HospitalIcon,
  IndianRupee,
  Landmark,
  ListChecks,
  LoaderCircle,
  LockKeyhole,
  Search,
  Smartphone,
  ShieldCheck,
  TicketCheck,
  Users,
  X,
  XCircle,
} from 'lucide-react'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { indiaFacilities, type IndiaFacility } from '../data/indiaFacilities'
import { projectDoctors } from '../data/projectDoctorProfiles'
import { api, messageFromError } from '../services/api'
import {
  appointmentStatusLabel,
  cancelAppointment,
  createAppointment,
  getAvailability,
  type Appointment,
  type Availability,
  type PaymentMethod,
} from '../services/appointments'
import { createPaymentIntent, paymentStatusLabel, type Payment } from '../services/payments'
import { createPrototypeBooking, updatePrototypeBooking, type PrototypeBooking } from '../services/prototypeBookings'

type Hospital = { id: string; name: string; city: string }
type Department = { id: string; name: string }
type Doctor = {
  id: string
  hospitalId: string
  hospitalName: string
  departmentId: string
  departmentName: string
  name: string
  specialization: string
  consultationFee: number
}
type Page<T> = { content: T[] }
type HospitalGroupId = 'raahmediq' | 'national' | 'regional' | 'government' | 'private'
type HospitalPickerItem = {
  value: string
  name: string
  location: string
  meta: string
}
type HospitalPickerGroup = {
  id: HospitalGroupId
  title: string
  shortTitle: string
  subtitle: string
  items: HospitalPickerItem[]
}

const indexedFacilities = indiaFacilities.map((facility, index) => ({ facility, index }))
const nationalInstitutePattern = /\bAIIMS\b|JIPMER|NEIGRIHMS|Postgraduate Institute/i
const regionalInstitutePattern = /\bRIMS\b|Regional Institute/i

function facilityGroup(facility: IndiaFacility): HospitalGroupId {
  if (facility.ownership === 'PRIVATE') return 'private'
  if (nationalInstitutePattern.test(facility.name)) return 'national'
  if (regionalInstitutePattern.test(facility.name)) return 'regional'
  return 'government'
}

function prototypeQueuePosition(doctorId: string, serviceDate: string) {
  const seed = [...`${doctorId}${serviceDate}`]
    .reduce((total, character) => total + character.charCodeAt(0), 0)
  return (seed % 38) + 1
}

const bookingRules = [
  { icon: LockKeyhole, title: 'Protected position', body: 'A queue number is held under one transaction and cannot be issued twice.' },
  { icon: Users, title: 'Fair waitlist', body: 'Full OPDs use first-in-first-out promotion when a protected position opens.' },
  { icon: Clock3, title: 'Clear deadlines', body: 'Unverified online and cash holds expire so capacity is not blocked indefinitely.' },
  { icon: ListChecks, title: 'Patient-controlled', body: 'Track or cancel this booking from My Care without returning to the counter.' },
]

function tomorrow() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

function readableDate(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'full' }).format(new Date(`${value}T12:00:00`))
}

export function QueueBookingPage() {
  const { session } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const requestedFacility = searchParams.get('facility')
  const requestedPrototypeDoctor = searchParams.get('prototypeDoctor')
  const initialReferenceFacilityIndex = requestedFacility !== null && Number.isInteger(Number(requestedFacility))
    && Number(requestedFacility) >= 0 && Number(requestedFacility) < indiaFacilities.length
    ? Number(requestedFacility)
    : -1
  const initialReferenceDoctorIndex = requestedPrototypeDoctor !== null && Number.isInteger(Number(requestedPrototypeDoctor))
    ? Number(requestedPrototypeDoctor)
    : -1
  const initialDoctorId = initialReferenceFacilityIndex >= 0 && initialReferenceDoctorIndex >= 0
    ? `reference:${initialReferenceFacilityIndex}:doctor:${initialReferenceDoctorIndex}`
    : searchParams.get('doctorId') ?? ''
  const [hospitals, setHospitals] = useState<Hospital[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [hospitalSelection, setHospitalSelection] = useState(initialReferenceFacilityIndex >= 0 ? `reference:${initialReferenceFacilityIndex}` : '')
  const [departmentId, setDepartmentId] = useState('')
  const [doctorId, setDoctorId] = useState(initialDoctorId)
  const [serviceDate, setServiceDate] = useState(tomorrow)
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CASH')
  const [availability, setAvailability] = useState<Availability | null>(null)
  const [availabilityState, setAvailabilityState] = useState<'idle' | 'loading' | 'ready' | 'error'>('idle')
  const [availabilityError, setAvailabilityError] = useState('')
  const [result, setResult] = useState<Appointment | null>(null)
  const [payment, setPayment] = useState<Payment | null>(null)
  const [paymentFlow, setPaymentFlow] = useState<'closed' | 'review' | 'provider' | 'authorizing' | 'pending' | 'verified'>('closed')
  const [referenceResult, setReferenceResult] = useState<PrototypeBooking | null>(null)
  const [hospitalPickerOpen, setHospitalPickerOpen] = useState(false)
  const [hospitalGroup, setHospitalGroup] = useState<HospitalGroupId>('raahmediq')
  const [hospitalSearch, setHospitalSearch] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  const hospitalId = hospitalSelection.startsWith('live:') ? hospitalSelection.slice(5) : ''
  const referenceFacilityIndex = hospitalSelection.startsWith('reference:')
    ? Number(hospitalSelection.slice(10))
    : -1
  const selectedReferenceFacility = referenceFacilityIndex >= 0
    ? indiaFacilities[referenceFacilityIndex]
    : undefined
  const hospitalPickerGroups = useMemo<HospitalPickerGroup[]>(() => {
    const referenceItems = (group: HospitalGroupId) => indexedFacilities
      .filter(({ facility }) => facilityGroup(facility) === group)
      .map(({ facility, index }) => ({
        value: `reference:${index}`,
        name: facility.name,
        location: `${facility.city}, ${facility.state}`,
        meta: 'Prototype directory hospital',
      }))
    return [
      {
        id: 'raahmediq',
        title: 'RaahMediQ Network',
        shortTitle: 'RaahMediQ',
        subtitle: 'Connected OPDs with the live capacity ledger',
        items: hospitals.map((hospital) => ({
          value: `live:${hospital.id}`,
          name: hospital.name,
          location: hospital.city,
          meta: 'Live-capacity demo OPD',
        })),
      },
      {
        id: 'national',
        title: 'AIIMS & National Institutes',
        shortTitle: 'AIIMS',
        subtitle: 'AIIMS, JIPMER and national-level public institutes',
        items: referenceItems('national'),
      },
      {
        id: 'regional',
        title: 'RIMS & Regional Institutes',
        shortTitle: 'RIMS',
        subtitle: 'Regional medical institutes and RIMS hospitals',
        items: referenceItems('regional'),
      },
      {
        id: 'government',
        title: 'Government & Sadar Hospitals',
        shortTitle: 'Government',
        subtitle: 'Sadar, civil, medical college and other public hospitals',
        items: referenceItems('government'),
      },
      {
        id: 'private',
        title: 'Private Hospitals',
        shortTitle: 'Private',
        subtitle: 'Apollo, Fortis, Medanta and other prototype private hospitals',
        items: referenceItems('private'),
      },
    ]
  }, [hospitals])
  const activeHospitalGroup = hospitalPickerGroups.find((group) => group.id === hospitalGroup)!
  const filteredHospitalItems = activeHospitalGroup.items.filter((item) =>
    `${item.name} ${item.location}`.toLowerCase().includes(hospitalSearch.trim().toLowerCase()))
  const selectedHospitalItem = hospitalPickerGroups
    .flatMap((group) => group.items)
    .find((item) => item.value === hospitalSelection)

  useEffect(() => {
    Promise.all([
      api.get<Hospital[]>('/api/v1/hospitals'),
      api.get<Page<Doctor>>('/api/v1/doctors', { params: { size: 100 } }),
    ]).then(([hospitalResponse, doctorResponse]) => {
      setHospitals(hospitalResponse.data)
      setDoctors(doctorResponse.data.content)
      const preselected = doctorResponse.data.content.find((doctor) => doctor.id === initialDoctorId)
      if (preselected) setHospitalSelection(`live:${preselected.hospitalId}`)
    }).catch((requestError) => setError(messageFromError(requestError)))
  }, [initialDoctorId])

  useEffect(() => {
    setDepartmentId('')
    if (selectedReferenceFacility) {
      const names = [...new Set(projectDoctors(selectedReferenceFacility).map((doctor) => doctor.department))]
      setDepartments(names.map((name) => ({
        id: `reference:${referenceFacilityIndex}:department:${name}`,
        name,
      })))
      return
    }
    if (!hospitalId) {
      setDepartments([])
      return
    }
    api.get<Department[]>(`/api/v1/hospitals/${hospitalId}/departments`)
      .then(({ data }) => setDepartments(data))
      .catch(() => setDepartments([]))
  }, [hospitalId, referenceFacilityIndex, selectedReferenceFacility])

  const referenceDoctors = useMemo<Doctor[]>(() => selectedReferenceFacility
    ? projectDoctors(selectedReferenceFacility).map((doctor, index) => ({
      id: `reference:${referenceFacilityIndex}:doctor:${index}`,
      hospitalId: `reference:${referenceFacilityIndex}`,
      hospitalName: selectedReferenceFacility.name,
      departmentId: `reference:${referenceFacilityIndex}:department:${doctor.department}`,
      departmentName: doctor.department,
      name: doctor.name,
      specialization: doctor.specialty,
      consultationFee: doctor.fee,
    }))
    : [], [referenceFacilityIndex, selectedReferenceFacility])

  useEffect(() => {
    setAvailability(null)
    setAvailabilityError('')
    if (selectedReferenceFacility) {
      setAvailabilityState('idle')
      return
    }
    if (!doctorId || !serviceDate) {
      setAvailabilityState('idle')
      return
    }
    let active = true
    setAvailabilityState('loading')
    getAvailability(doctorId, serviceDate)
      .then((data) => {
        if (!active) return
        setAvailability(data)
        setAvailabilityState('ready')
      })
      .catch((requestError) => {
        if (!active) return
        setAvailabilityError(messageFromError(requestError))
        setAvailabilityState('error')
      })
    return () => { active = false }
  }, [doctorId, selectedReferenceFacility, serviceDate])

  const visibleDoctors = useMemo(() => {
    if (!selectedReferenceFacility && !hospitalId) return []
    const source = selectedReferenceFacility ? referenceDoctors : doctors
    return source.filter((doctor) =>
      (selectedReferenceFacility || doctor.hospitalId === hospitalId)
      && (!departmentId || doctor.departmentId === departmentId))
  }, [departmentId, doctors, hospitalId, referenceDoctors, selectedReferenceFacility])
  const selectedDoctor = [...doctors, ...referenceDoctors].find((doctor) => doctor.id === doctorId)
  const selectedReferenceProfile = selectedReferenceFacility && selectedDoctor
    ? projectDoctors(selectedReferenceFacility).find((doctor) =>
      doctor.name === selectedDoctor.name && doctor.specialty === selectedDoctor.specialization)
    : undefined
  const canSubmit = Boolean(
    doctorId
    && serviceDate
    && (selectedReferenceFacility || availabilityState === 'ready'),
  )

  function changeHospital(value: string) {
    setHospitalSelection(value)
    setDoctorId('')
    setResult(null)
    setPayment(null)
    setReferenceResult(null)
  }

  function openHospitalPicker() {
    if (hospitalSelection.startsWith('live:')) setHospitalGroup('raahmediq')
    if (selectedReferenceFacility) setHospitalGroup(facilityGroup(selectedReferenceFacility))
    setHospitalSearch('')
    setHospitalPickerOpen(true)
  }

  function selectHospital(value: string) {
    changeHospital(value)
    setHospitalPickerOpen(false)
    setHospitalSearch('')
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!session) {
      navigate('/login', { state: { from: '/booking' } })
      return
    }
    if (selectedReferenceFacility && selectedDoctor) {
      const queuePosition = prototypeQueuePosition(selectedDoctor.id, serviceDate)
      setError('')
      setResult(null)
      setPayment(null)
      const booking = createPrototypeBooking({
        hospitalName: selectedReferenceFacility.name,
        hospitalLocation: `${selectedReferenceFacility.city} · ${selectedReferenceFacility.state}`,
        ownership: selectedReferenceFacility.ownership,
        doctorName: selectedDoctor.name,
        specialization: selectedDoctor.specialization,
        departmentName: selectedDoctor.departmentName,
        serviceDate,
        paymentMethod,
        queuePosition,
        estimatedWaitMinutes: Math.max(10, queuePosition * 4),
        amount: selectedDoctor.consultationFee,
        status: paymentMethod === 'ONLINE' ? 'PAYMENT_PENDING' : 'CASH_PENDING',
        providerReference: paymentMethod === 'ONLINE' ? `RMQ-DEMO-${Date.now().toString(36).toUpperCase()}` : undefined,
      })
      setReferenceResult(booking)
      setPaymentFlow(paymentMethod === 'ONLINE' ? 'review' : 'closed')
      return
    }
    setSubmitting(true)
    setError('')
    setResult(null)
    setPayment(null)
    setReferenceResult(null)
    try {
      const appointment = await createAppointment(doctorId, serviceDate, paymentMethod)
      setResult(appointment)
      if (appointment.paymentMethod === 'ONLINE' && appointment.status === 'RESERVED_PENDING_PAYMENT') {
        setPayment(await createPaymentIntent(appointment.id))
        setPaymentFlow('review')
      }
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  async function cancelResult() {
    if (!result) return
    setSubmitting(true)
    setError('')
    try {
      setResult(await cancelAppointment(result.id))
      setPayment((current) => current ? { ...current, status: current.status === 'PENDING' ? 'CANCELLED' : current.status } : null)
      if (doctorId && serviceDate) setAvailability(await getAvailability(doctorId, serviceDate))
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  function approveProvider() {
    setPaymentFlow('authorizing')
    window.setTimeout(() => {
      if (referenceResult) {
        const receiptNumber = `RMQ-SIM-${Date.now().toString(36).toUpperCase()}`
        const updated = updatePrototypeBooking(referenceResult.id, {
          status: 'CONFIRMED',
          receiptNumber,
        })
        if (updated) setReferenceResult(updated)
        setPaymentFlow('verified')
      } else {
        setPaymentFlow('pending')
      }
    }, 1800)
  }

  const paymentAmount = referenceResult?.amount ?? payment?.amount ?? 0
  const paymentDate = referenceResult?.serviceDate ?? result?.serviceDate ?? serviceDate
  const paymentReference = referenceResult?.providerReference ?? payment?.providerReference ?? 'Awaiting provider reference'
  const paymentDisplayStatus = referenceResult
    ? referenceResult.status === 'CONFIRMED' ? 'Verified demo payment' : 'Provider confirmation pending'
    : payment ? paymentStatusLabel[payment.status] : 'Payment not started'

  return (
    <div className="bg-[#f4f8fc]">
      <section className="border-b border-blue-100 bg-white">
        <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 sm:py-14 lg:px-8">
          <div className="max-w-4xl">
            <span className="inline-flex items-center gap-2 rounded-full bg-care-50 px-3 py-1.5 text-xs font-extrabold uppercase tracking-[.15em] text-care-800"><ShieldCheck className="size-4" /> Capacity checked before issue</span>
            <p className="mt-6 text-xs font-extrabold uppercase tracking-[.22em] text-care-700">Fair OPD access</p>
            <h1 className="mt-3 text-4xl font-black tracking-[-0.04em] text-ink-950 sm:text-5xl">Book a protected OPD number</h1>
            <p className="mt-4 max-w-2xl text-base leading-7 text-slate-600">Choose a hospital, doctor and date. Live RaahMediQ OPDs check the capacity ledger; India directory hospitals demonstrate the same journey as a clearly labelled prototype.</p>
          </div>
        </div>
      </section>

      <section className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 lg:grid-cols-[1.08fr_.92fr] lg:px-8 lg:py-14">
        <form onSubmit={submit} className="rounded-[1.75rem] border border-slate-200 bg-white p-5 shadow-[0_18px_55px_-35px_rgba(9,45,87,.35)] sm:p-8">
          <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-5">
            <div><p className="text-xs font-extrabold uppercase tracking-[.18em] text-care-700">Visit details</p><h2 className="mt-1 text-2xl font-black text-ink-950">Select your OPD</h2></div>
            <span className="grid size-12 place-items-center rounded-xl bg-care-50 text-care-700"><CalendarDays className="size-6" /></span>
          </div>

          <div className="mt-5 flex flex-col gap-3 rounded-2xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-950 sm:flex-row sm:items-center sm:justify-between"><div><strong>70 India directory hospitals are now selectable</strong><p className="mt-1 text-xs leading-5 text-blue-800">RaahMediQ Demo Care Centre uses the live capacity ledger. Government and private directory entries create a clearly labelled prototype OPD preview inside this project.</p></div><Link to="/hospitals" className="shrink-0 font-black text-care-700 underline">Browse full profiles</Link></div>

          <div className="mt-6 grid gap-5 sm:grid-cols-2">
            <div className="text-sm font-bold text-slate-700">Hospital
              <button type="button" onClick={openHospitalPicker} aria-haspopup="dialog" aria-expanded={hospitalPickerOpen} className="mt-2 flex min-h-12 w-full items-center justify-between gap-3 rounded-xl border border-slate-300 bg-white px-3 py-2 text-left text-sm font-semibold text-ink-950 transition hover:border-care-400">
                {selectedHospitalItem ? <span className="min-w-0"><span className="block truncate">{selectedHospitalItem.name}</span><span className="block truncate text-[11px] font-medium text-slate-500">{selectedHospitalItem.location}</span></span> : <span className="font-medium text-slate-500">Choose hospital section</span>}
                <ChevronDown className="size-4 shrink-0 text-slate-500" />
              </button>
            </div>
            <label className="text-sm font-bold text-slate-700">Department
              <select value={departmentId} onChange={(event) => { setDepartmentId(event.target.value); setDoctorId(''); setResult(null); setPayment(null); setReferenceResult(null) }} disabled={!hospitalSelection} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-ink-950 disabled:bg-slate-50">
                <option value="">All departments</option>
                {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
              </select>
            </label>
            <label className="text-sm font-bold text-slate-700">Doctor
              <select required value={doctorId} onChange={(event) => { setDoctorId(event.target.value); setResult(null); setPayment(null); setReferenceResult(null) }} disabled={!hospitalSelection} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-ink-950 disabled:bg-slate-50">
                <option value="">Select doctor</option>
                {visibleDoctors.map((doctor) => <option key={doctor.id} value={doctor.id}>{doctor.name} · {doctor.specialization}</option>)}
              </select>
            </label>
            <label className="text-sm font-bold text-slate-700">Preferred date
              <input required type="date" min={new Date().toISOString().slice(0, 10)} value={serviceDate} onChange={(event) => { setServiceDate(event.target.value); setResult(null); setPayment(null); setReferenceResult(null) }} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-semibold text-ink-950" />
            </label>
          </div>

          <div className="mt-5 min-h-20 rounded-2xl border border-slate-200 bg-slate-50 p-4" aria-live="polite">
            {selectedReferenceFacility ? (
              selectedDoctor ? <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-sm font-black text-ink-950">Project OPD workflow ready</p><p className="mt-1 text-xs leading-5 text-slate-600">{selectedReferenceProfile?.availability ?? 'Sample OPD availability'} · generated inside RaahMediQ</p></div><span className="rounded-full bg-amber-100 px-3 py-1.5 text-xs font-extrabold text-amber-900">Simulation capacity</span><p className="w-full text-xs leading-5 text-slate-500">You can complete OPD booking and demo payment inside this project. No real hospital is contacted.</p></div>
                : <p className="text-sm text-slate-500">Select a project doctor to continue to OPD booking.</p>
            ) : <>
              {availabilityState === 'idle' && <p className="text-sm text-slate-500">Select a doctor and date to check the live capacity ledger.</p>}
              {availabilityState === 'loading' && <p className="flex items-center gap-2 text-sm font-bold text-slate-600"><LoaderCircle className="size-4 animate-spin" /> Checking capacity…</p>}
              {availabilityState === 'error' && <p className="flex gap-2 text-sm font-semibold text-amber-900"><XCircle className="size-5 shrink-0" />{availabilityError}</p>}
              {availabilityState === 'ready' && availability && <div className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-sm font-black text-ink-950">OPD {availability.scheduleStart.slice(0, 5)}–{availability.scheduleEnd.slice(0, 5)}</p><p className="mt-1 text-xs text-slate-500">{availability.reservedCount} of {availability.effectiveCapacity} positions currently held</p></div><span className={`rounded-full px-3 py-1.5 text-xs font-extrabold ${availability.positionsAvailable > 0 ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-900'}`}>{availability.positionsAvailable > 0 ? `${availability.positionsAvailable} available` : `Waitlist · ${availability.waitlistCount} waiting`}</span></div>}
            </>}
          </div>

          <fieldset className="mt-6">
            <legend className="text-sm font-bold text-slate-700">Payment preference</legend>
            <div className="mt-2 grid gap-3 sm:grid-cols-2">
              <label className={`flex cursor-pointer items-center gap-3 rounded-xl border p-4 transition ${paymentMethod === 'CASH' ? 'border-care-500 bg-care-50' : 'border-slate-200'}`}><input checked={paymentMethod === 'CASH'} onChange={() => { setPaymentMethod('CASH'); setResult(null); setPayment(null); setReferenceResult(null) }} type="radio" name="payment" className="accent-care-600" /><IndianRupee className="size-5 text-care-700" /><span><span className="block text-sm font-black text-ink-950">Cash at hospital</span><span className="text-xs text-slate-500">Desk deadline applies</span></span></label>
              <label className={`flex cursor-pointer items-center gap-3 rounded-xl border p-4 transition ${paymentMethod === 'ONLINE' ? 'border-care-500 bg-care-50' : 'border-slate-200'}`}><input checked={paymentMethod === 'ONLINE'} onChange={() => { setPaymentMethod('ONLINE'); setResult(null); setPayment(null); setReferenceResult(null) }} type="radio" name="payment" className="accent-care-600" /><CreditCard className="size-5 text-care-700" /><span><span className="block text-sm font-black text-ink-950">Pay online</span><span className="text-xs text-slate-500">Verification required</span></span></label>
            </div>
          </fieldset>

          {error && <p role="alert" className="mt-5 rounded-xl bg-rose-50 p-3 text-sm font-semibold text-rose-800">{error}</p>}
          {!session && <p className="mt-5 rounded-xl bg-amber-50 p-3 text-sm font-semibold text-amber-900">Sign in is required only when you submit the booking.</p>}
          <button disabled={submitting || !canSubmit} className="mt-6 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-care-600 px-5 py-3 text-sm font-extrabold text-white shadow-lg shadow-blue-700/15 transition hover:bg-care-700 disabled:cursor-not-allowed disabled:opacity-50">{submitting ? <LoaderCircle className="size-4 animate-spin" /> : <TicketCheck className="size-4" />}{session ? (selectedReferenceFacility ? 'Book project OPD number' : 'Protect my OPD position') : 'Sign in to continue'}<ArrowRight className="size-4" /></button>
          <p className="mt-3 text-center text-xs leading-5 text-slate-500">{selectedReferenceFacility ? 'Project booking is saved in this browser session. Demo payment collects no real money.' : 'No card is charged on this screen. Online status changes only after trusted payment verification.'}</p>
        </form>

        <div className="space-y-5">
          <div className="overflow-hidden rounded-[1.75rem] bg-ink-950 p-6 text-white shadow-[0_20px_55px_-32px_rgba(9,45,87,.6)] sm:p-8" aria-live="polite">
            <div className="flex items-start justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-[.18em] text-blue-300">Booking result</p><h2 className="mt-2 text-2xl font-black">Your queue status</h2></div><TicketCheck className="size-8 text-blue-300" /></div>
            {referenceResult ? (
              <div className="mt-7 rounded-2xl border border-amber-300/25 bg-white/10 p-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <p className="text-xs font-bold uppercase tracking-[.16em] text-amber-200">{referenceResult.status === 'CONFIRMED' ? 'Project OPD confirmed' : referenceResult.status === 'PAYMENT_PENDING' ? 'OPD held · payment required' : 'OPD booked · cash confirmation pending'}</p>
                    <p className="mt-2 text-lg font-black">{referenceResult.doctorName}</p>
                    <p className="text-sm text-slate-300">{referenceResult.specialization}</p>
                    <p className="mt-1 text-sm text-slate-300">{referenceResult.hospitalName} · {referenceResult.hospitalLocation}</p>
                  </div>
                  <div className="text-right"><p className="text-xs font-bold text-amber-200">Project OPD number</p><p className="text-4xl font-black">{String(referenceResult.queuePosition).padStart(2, '0')}</p></div>
                </div>
                <div className="mt-5 grid grid-cols-2 gap-3 border-t border-white/10 pt-4 text-sm">
                  <div><p className="text-xs text-slate-400">Preferred date</p><p className="mt-1 font-bold">{readableDate(referenceResult.serviceDate)}</p></div>
                  <div><p className="text-xs text-slate-400">Sample wait estimate</p><p className="mt-1 font-bold">~{referenceResult.estimatedWaitMinutes} min</p></div>
                  <div><p className="text-xs text-slate-400">Hospital group</p><p className="mt-1 font-bold">{referenceResult.ownership === 'GOVERNMENT' ? 'Government' : 'Private'}</p></div>
                  <div><p className="text-xs text-slate-400">Payment preference</p><p className="mt-1 font-bold">{referenceResult.paymentMethod === 'CASH' ? 'Cash at desk' : 'Online'}</p></div>
                </div>
                {referenceResult.status === 'PAYMENT_PENDING' && <p className="mt-4 rounded-xl bg-blue-300/10 p-3 text-xs leading-5 text-blue-100">Your project OPD number is held. Complete the demo provider flow to confirm it.</p>}
                {referenceResult.status === 'CASH_PENDING' && <p className="mt-4 rounded-xl bg-amber-300/10 p-3 text-xs leading-5 text-amber-100">Your project OPD number is saved. Cash confirmation is shown as pending at the simulated hospital desk.</p>}
                {referenceResult.status === 'CONFIRMED' && <p className="mt-4 rounded-xl bg-emerald-300/10 p-3 text-xs leading-5 text-emerald-100">Demo payment verified and project OPD confirmed. Receipt: {referenceResult.receiptNumber}</p>}
                <p className="mt-4 rounded-xl bg-white/5 p-3 text-xs leading-5 text-slate-300">Simulation only: no real hospital is contacted and no money is collected.</p>
                <div className="mt-5 flex flex-wrap gap-3">{referenceResult.status === 'PAYMENT_PENDING' && <button type="button" onClick={() => setPaymentFlow('review')} className="rounded-xl bg-white px-4 py-2.5 text-sm font-extrabold text-ink-950">Continue demo payment</button>}<Link to="/dashboard" className="rounded-xl border border-white/20 px-4 py-2.5 text-sm font-bold text-white hover:bg-white/10">Open My Care</Link><button type="button" onClick={() => setReferenceResult(null)} className="rounded-xl border border-white/20 px-4 py-2.5 text-sm font-bold text-white hover:bg-white/10">Book another</button></div>
              </div>
            ) : result ? (
              <div className="mt-7 rounded-2xl border border-white/10 bg-white/10 p-5">
                <div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-[.16em] text-blue-200">{appointmentStatusLabel[result.status]}</p><p className="mt-2 text-lg font-black">{result.doctorName}</p><p className="text-sm text-slate-300">{result.hospitalName} · {readableDate(result.serviceDate)}</p></div>{result.queuePosition ? <div className="text-right"><p className="text-xs font-bold text-blue-200">OPD number</p><p className="text-4xl font-black">{result.queuePosition}</p></div> : <Users className="size-9 text-amber-300" />}</div>
                <div className="mt-5 grid grid-cols-2 gap-3 border-t border-white/10 pt-4 text-sm"><div><p className="text-xs text-slate-400">Payment</p><p className="mt-1 font-bold">{result.paymentMethod === 'CASH' ? 'Cash at desk' : 'Online verification'}</p></div><div><p className="text-xs text-slate-400">Estimated queue time</p><p className="mt-1 font-bold">{result.queuePosition ? `~${result.estimatedWaitMinutes} min` : 'After promotion'}</p></div></div>
                {result.status === 'WAITLISTED' && <p className="mt-4 rounded-xl bg-amber-300/10 p-3 text-xs leading-5 text-amber-100">The OPD is full. Your place is recorded in the fair waitlist and will be promoted automatically when capacity opens.</p>}
                {result.status === 'CASH_PENDING' && <p className="mt-4 rounded-xl bg-emerald-300/10 p-3 text-xs leading-5 text-emerald-100">Your number is held. Complete cash confirmation at the hospital desk before {new Date(result.cashDeadlineAt!).toLocaleString('en-IN')}.</p>}
                {result.status === 'RESERVED_PENDING_PAYMENT' && <p className="mt-4 rounded-xl bg-blue-300/10 p-3 text-xs leading-5 text-blue-100">Your number is temporarily held. It is not confirmed until a trusted payment provider verifies the transaction.</p>}
                {payment && <div className="mt-4 rounded-xl border border-white/10 bg-white/5 p-3 text-xs"><div className="flex items-center justify-between gap-3"><span className="font-extrabold text-blue-100">{paymentStatusLabel[payment.status]}</span><span className="font-mono text-[10px] text-slate-400">{payment.providerReference}</span></div><p className="mt-2 leading-5 text-slate-300">{payment.verificationMessage}</p>{payment.receiptNumber && <p className="mt-2 font-bold text-emerald-200">Receipt: {payment.receiptNumber}</p>}</div>}
                {result.status === 'CANCELLED' ? <p className="mt-4 flex items-center gap-2 text-sm font-bold text-rose-200"><XCircle className="size-4" />Booking cancelled and capacity released.</p> : <div className="mt-5 flex flex-wrap gap-3"><Link to="/dashboard" className="rounded-xl bg-white px-4 py-2.5 text-sm font-extrabold text-ink-950">Open My Care</Link><button disabled={submitting} type="button" onClick={() => void cancelResult()} className="rounded-xl border border-white/20 px-4 py-2.5 text-sm font-bold text-white hover:bg-white/10">Cancel booking</button></div>}
              </div>
            ) : (
              <div className="mt-8 rounded-2xl border border-dashed border-white/20 p-7 text-center"><CalendarDays className="mx-auto size-8 text-blue-300" /><p className="mt-3 font-bold">No booking submitted yet</p><p className="mt-2 text-sm leading-6 text-slate-300">Select visit details to see verified live capacity or a clearly labelled prototype preview.</p></div>
            )}
          </div>

          {selectedDoctor && <div className="rounded-2xl border border-slate-200 bg-white p-5"><p className="text-xs font-extrabold uppercase tracking-wider text-care-700">{selectedReferenceFacility ? 'Prototype care profile' : 'Selected care team'}</p><h3 className="mt-2 font-black text-ink-950">{selectedDoctor.name}</h3><p className="text-sm text-slate-600">{selectedDoctor.specialization} · {selectedDoctor.departmentName}</p><p className="mt-3 flex items-center gap-1 text-sm font-bold text-slate-700"><IndianRupee className="size-4" />{selectedDoctor.consultationFee.toLocaleString('en-IN')}</p>{selectedReferenceProfile && <><p className="mt-3 text-sm font-semibold text-slate-700">{selectedReferenceProfile.availability}</p><p className="mt-2 rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-900">{selectedReferenceProfile.note}</p></>}</div>}
        </div>
      </section>

      {hospitalPickerOpen && <div className="fixed inset-0 z-[70] grid place-items-center bg-ink-950/70 p-3 backdrop-blur-sm sm:p-6" role="dialog" aria-modal="true" aria-labelledby="booking-hospital-picker-title">
        <div className="flex max-h-[92vh] w-full max-w-6xl flex-col overflow-hidden rounded-[2rem] bg-white shadow-2xl">
          <div className="flex items-start justify-between gap-4 border-b border-slate-200 p-5 sm:p-6">
            <div><p className="text-xs font-black uppercase tracking-[.16em] text-care-700">RaahMediQ hospital picker</p><h2 id="booking-hospital-picker-title" className="mt-1 text-2xl font-black text-ink-950">Choose a hospital section</h2><p className="mt-1 text-xs leading-5 text-slate-500">Pick one category first, then search or scroll only inside that section.</p></div>
            <button type="button" aria-label="Close hospital picker" onClick={() => setHospitalPickerOpen(false)} className="grid size-11 shrink-0 place-items-center rounded-xl border border-slate-200 text-slate-600 transition hover:bg-slate-50"><X className="size-5" /></button>
          </div>

          <div className="flex min-h-0 flex-1 flex-col lg:flex-row">
            <div className="grid shrink-0 grid-cols-2 gap-2 border-b border-slate-200 bg-slate-50 p-4 sm:grid-cols-5 lg:w-72 lg:grid-cols-1 lg:border-b-0 lg:border-r">
              {hospitalPickerGroups.map((group) => <button type="button" key={group.id} onClick={() => { setHospitalGroup(group.id); setHospitalSearch('') }} className={`flex items-center gap-3 rounded-xl border px-3 py-3 text-left transition ${hospitalGroup === group.id ? 'border-care-300 bg-white text-care-800 shadow-sm' : 'border-transparent text-slate-600 hover:bg-white'}`}>
                <span className={`grid size-9 shrink-0 place-items-center rounded-lg ${hospitalGroup === group.id ? 'bg-care-100 text-care-700' : 'bg-white text-slate-500'}`}>{group.id === 'raahmediq' ? <HospitalIcon className="size-4" /> : group.id === 'private' ? <Building2 className="size-4" /> : <Landmark className="size-4" />}</span>
                <span className="min-w-0"><span className="block text-xs font-black sm:text-sm">{group.shortTitle}</span><span className="block text-[10px] font-semibold text-slate-400">{group.items.length} hospital{group.items.length === 1 ? '' : 's'}</span></span>
              </button>)}
            </div>

            <div className="flex min-h-0 flex-1 flex-col p-4 sm:p-6">
              <div className="flex flex-col gap-4 border-b border-slate-100 pb-4 sm:flex-row sm:items-end sm:justify-between">
                <div><p className="text-xs font-black uppercase tracking-[.14em] text-care-700">{activeHospitalGroup.shortTitle} section</p><h3 className="mt-1 text-xl font-black text-ink-950">{activeHospitalGroup.title}</h3><p className="mt-1 text-xs leading-5 text-slate-500">{activeHospitalGroup.subtitle}</p></div>
                <label className="relative block w-full sm:max-w-xs"><span className="sr-only">Search this hospital section</span><Search className="pointer-events-none absolute left-3 top-3.5 size-4 text-slate-400" /><input value={hospitalSearch} onChange={(event) => setHospitalSearch(event.target.value)} placeholder="Search hospital or city" className="h-11 w-full rounded-xl border border-slate-300 bg-white pl-10 pr-3 text-sm outline-none focus:border-care-500" /></label>
              </div>

              <div className="mt-4 grid min-h-0 flex-1 auto-rows-max gap-3 overflow-y-auto pr-1 sm:grid-cols-2">
                {filteredHospitalItems.map((item) => <button type="button" key={item.value} onClick={() => selectHospital(item.value)} className={`flex items-start gap-3 rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 hover:border-care-300 hover:shadow-md ${hospitalSelection === item.value ? 'border-emerald-300 bg-emerald-50' : 'border-slate-200 bg-white'}`}>
                  <span className={`grid size-10 shrink-0 place-items-center rounded-xl ${hospitalGroup === 'private' ? 'bg-violet-50 text-violet-700' : hospitalGroup === 'raahmediq' ? 'bg-care-50 text-care-700' : 'bg-emerald-50 text-emerald-700'}`}>{hospitalGroup === 'private' ? <Building2 className="size-5" /> : hospitalGroup === 'raahmediq' ? <HospitalIcon className="size-5" /> : <Landmark className="size-5" />}</span>
                  <span className="min-w-0"><span className="block font-black text-ink-950">{item.name}</span><span className="mt-1 block text-xs text-slate-500">{item.location}</span><span className="mt-2 block text-[10px] font-black uppercase tracking-wider text-care-700">{hospitalSelection === item.value ? 'Selected' : item.meta}</span></span>
                </button>)}
                {filteredHospitalItems.length === 0 && <div className="col-span-full rounded-2xl border border-dashed border-slate-300 p-8 text-center"><Search className="mx-auto size-7 text-slate-400" /><p className="mt-3 font-black text-ink-950">No hospital found in this section</p><p className="mt-1 text-sm text-slate-500">Try another search or choose a different category.</p></div>}
              </div>
            </div>
          </div>
        </div>
      </div>}

      {paymentFlow !== 'closed' && ((payment && result) || referenceResult) && <div className="fixed inset-0 z-[70] grid place-items-center bg-ink-950/70 p-4 backdrop-blur-sm" role="dialog" aria-modal="true" aria-labelledby="payment-flow-title">
        <div className="w-full max-w-lg overflow-hidden rounded-[2rem] bg-white shadow-2xl">
          <div className="bg-ink-950 p-6 text-white"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-[.16em] text-blue-300">Secure provider flow · demo</p><h2 id="payment-flow-title" className="mt-2 text-2xl font-black">Authorize your OPD payment</h2></div><button onClick={() => setPaymentFlow('closed')} className="rounded-xl border border-white/15 px-3 py-2 text-xs font-black">Close</button></div><div className="mt-5 grid grid-cols-4 gap-2 text-center text-[10px] font-black">{['Review', 'Provider', 'Authorize', 'Verified'].map((label, index) => { const order = { review: 0, provider: 1, authorizing: 2, pending: 3, verified: 3, closed: -1 }[paymentFlow]; return <div key={label}><span className={`mx-auto grid size-8 place-items-center rounded-full ${index <= order ? 'bg-care-500 text-white' : 'bg-white/10 text-blue-100/50'}`}>{index + 1}</span><span className="mt-1 block text-blue-100/70">{label}</span></div> })}</div></div>

          <div className="p-6 sm:p-7">
            {paymentFlow === 'review' && <><p className="text-xs font-black uppercase tracking-wider text-care-700">1 · Review</p><h3 className="mt-2 text-xl font-black text-ink-950">{selectedDoctor?.name}</h3><div className="mt-5 grid grid-cols-2 gap-3 rounded-2xl bg-slate-50 p-4 text-sm"><div><p className="text-xs text-slate-500">Amount</p><p className="mt-1 text-xl font-black">₹{paymentAmount.toLocaleString('en-IN')}</p></div><div><p className="text-xs text-slate-500">OPD date</p><p className="mt-1 font-black">{readableDate(paymentDate)}</p></div></div><p className="mt-4 rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-900">Project payment simulation only. No account is debited.</p><button onClick={() => setPaymentFlow('provider')} className="mt-6 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-care-600 px-5 text-sm font-black text-white">Continue to secure provider <ArrowRight className="size-4" /></button></>}
            {paymentFlow === 'provider' && <><p className="text-xs font-black uppercase tracking-wider text-care-700">2 · Bank or UPI provider</p><div className="mt-4 flex gap-3 rounded-2xl border border-blue-200 bg-blue-50 p-4"><Smartphone className="mt-0.5 size-6 shrink-0 text-care-700" /><div><h3 className="font-black text-blue-950">Approve in your trusted provider app</h3><p className="mt-2 text-sm leading-6 text-blue-900">A real production payment redirects to the bank or UPI app. Enter your UPI PIN only there. <strong>RaahMediQ Health never asks for, sees or stores your PIN.</strong></p></div></div><p className="mt-4 text-xs leading-5 text-slate-500">Demo mode collects no money. This screen demonstrates the hand-off and callback states without imitating a bank PIN screen.</p><button onClick={approveProvider} className="mt-6 inline-flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-care-600 px-5 text-sm font-black text-white">I approved in the provider app <ShieldCheck className="size-4" /></button></>}
            {paymentFlow === 'authorizing' && <div className="py-10 text-center"><span className="mx-auto grid size-20 place-items-center rounded-full bg-care-50 text-care-700"><LoaderCircle className="size-10 animate-spin" /></span><h3 className="mt-5 text-xl font-black text-ink-950">Authorizing securely…</h3><p className="mt-2 text-sm leading-6 text-slate-500">Waiting for the provider’s signed confirmation. Do not refresh or pay again.</p></div>}
            {paymentFlow === 'pending' && <div className="py-5 text-center"><span className="mx-auto grid size-16 place-items-center rounded-full bg-amber-50 text-amber-700"><Clock3 className="size-8" /></span><h3 className="mt-5 text-xl font-black text-ink-950">Provider confirmation pending</h3><p className="mt-2 text-sm leading-6 text-slate-600">Your OPD position remains temporarily held. It becomes confirmed only after RaahMediQ receives a valid signed callback from the provider.</p><div className="mt-5 rounded-xl bg-slate-50 p-3 text-left text-xs text-slate-600"><strong>Reference:</strong> {paymentReference}<br /><strong>Status:</strong> {paymentDisplayStatus}</div><button onClick={() => setPaymentFlow('closed')} className="mt-6 min-h-12 w-full rounded-xl bg-ink-950 px-5 text-sm font-black text-white">Return to booking</button></div>}
            {paymentFlow === 'verified' && referenceResult && <div className="py-5 text-center"><span className="mx-auto grid size-16 place-items-center rounded-full bg-emerald-50 text-emerald-700"><CheckCircle2 className="size-8" /></span><h3 className="mt-5 text-xl font-black text-ink-950">Demo payment verified</h3><p className="mt-2 text-sm leading-6 text-slate-600">Your project OPD number {referenceResult.queuePosition} is confirmed and saved in My Care for this browser session.</p><div className="mt-5 rounded-xl bg-slate-50 p-3 text-left text-xs text-slate-600"><strong>Reference:</strong> {paymentReference}<br /><strong>Receipt:</strong> {referenceResult.receiptNumber}<br /><strong>Status:</strong> {paymentDisplayStatus}</div><button onClick={() => setPaymentFlow('closed')} className="mt-6 min-h-12 w-full rounded-xl bg-ink-950 px-5 text-sm font-black text-white">View confirmed booking</button></div>}
          </div>
        </div>
      </div>}

      <section className="border-t border-blue-100 bg-white py-12">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{bookingRules.map(({ icon: Icon, title, body }) => <article key={title} className="rounded-2xl border border-slate-200 p-5"><span className="grid size-10 place-items-center rounded-xl bg-care-50 text-care-700"><Icon className="size-5" /></span><h3 className="mt-4 font-black text-ink-950">{title}</h3><p className="mt-2 text-sm leading-6 text-slate-600">{body}</p></article>)}</div><p className="mt-8 flex items-start gap-2 text-xs leading-5 text-slate-500"><CheckCircle2 className="mt-0.5 size-4 shrink-0 text-emerald-600" />Queue capacity is assigned server-side under a database lock; re-submitting the same request cannot create a second booking.</p></div>
      </section>
    </div>
  )
}
