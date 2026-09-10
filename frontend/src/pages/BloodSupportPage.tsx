import {
  AlertOctagon, BadgeCheck, Building2, CheckCircle2, Clock3, Droplets, HeartHandshake,
  Info, LoaderCircle, MapPin, Phone, RefreshCw, ShieldCheck, TriangleAlert, Users,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { messageFromError } from '../services/api'
import {
  createDonorConsent, getBloodAvailability, getMyBloodRequests, getMyDonorConsent,
  withdrawDonorConsent, type AvailabilityStatus, type BloodAvailability,
  type BloodComponent, type BloodGroup, type BloodRequest, type BloodRequestStatus,
  type DonorOptIn,
} from '../services/bloodBank'
import { getNavigationHospitals, type HospitalSummary } from '../services/navigation'

const groupLabels: Record<BloodGroup, string> = {
  A_POSITIVE: 'A+', A_NEGATIVE: 'A−', B_POSITIVE: 'B+', B_NEGATIVE: 'B−',
  AB_POSITIVE: 'AB+', AB_NEGATIVE: 'AB−', O_POSITIVE: 'O+', O_NEGATIVE: 'O−',
}

const componentLabels: Record<BloodComponent, string> = {
  WHOLE_BLOOD: 'Whole blood', PACKED_RED_CELLS: 'Packed red cells',
  PLATELETS: 'Platelets', FRESH_FROZEN_PLASMA: 'Fresh frozen plasma',
}

const availabilityLabels: Record<AvailabilityStatus, string> = {
  AVAILABLE: 'Verified availability', LIMITED: 'Limited verified stock',
  UNAVAILABLE: 'No verified units available', STALE_OR_UNVERIFIED: 'Verification expired or unavailable',
}

const availabilityStyles: Record<AvailabilityStatus, string> = {
  AVAILABLE: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  LIMITED: 'border-amber-200 bg-amber-50 text-amber-950',
  UNAVAILABLE: 'border-rose-200 bg-rose-50 text-rose-900',
  STALE_OR_UNVERIFIED: 'border-slate-300 bg-slate-100 text-slate-700',
}

const requestLabels: Record<BloodRequestStatus, string> = {
  SEARCHING: 'Authorized search in progress', PARTIALLY_RESERVED: 'Partially reserved',
  RESERVED: 'Verified units reserved', UNAVAILABLE: 'Staff review required',
  FULFILLED: 'Fulfilled', CANCELLED: 'Cancelled',
}

const requestStyles: Record<BloodRequestStatus, string> = {
  SEARCHING: 'bg-blue-50 text-blue-800 border-blue-200',
  PARTIALLY_RESERVED: 'bg-amber-50 text-amber-900 border-amber-200',
  RESERVED: 'bg-emerald-50 text-emerald-900 border-emerald-200',
  UNAVAILABLE: 'bg-rose-50 text-rose-900 border-rose-200',
  FULFILLED: 'bg-violet-50 text-violet-900 border-violet-200',
  CANCELLED: 'bg-slate-100 text-slate-600 border-slate-200',
}

function displayDateTime(value?: string) {
  if (!value) return 'No current verification timestamp'
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function displayDate(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`))
}

export function BloodSupportPage() {
  const [hospitals, setHospitals] = useState<HospitalSummary[]>([])
  const [requests, setRequests] = useState<BloodRequest[]>([])
  const [availability, setAvailability] = useState<BloodAvailability[]>([])
  const [donor, setDonor] = useState<DonorOptIn | null>(null)
  const [hospitalId, setHospitalId] = useState('')
  const [bloodGroup, setBloodGroup] = useState<BloodGroup>('O_POSITIVE')
  const [component, setComponent] = useState<BloodComponent>('PACKED_RED_CELLS')
  const [units, setUnits] = useState(1)
  const [contactPreference, setContactPreference] = useState<DonorOptIn['contactPreference']>('MOBILE')
  const [loading, setLoading] = useState(true)
  const [searching, setSearching] = useState(false)
  const [donorWorking, setDonorWorking] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    Promise.all([getNavigationHospitals(), getMyBloodRequests(), getMyDonorConsent()])
      .then(([hospitalData, requestData, donorData]) => {
        setHospitals(hospitalData)
        setRequests(requestData)
        setDonor(donorData)
        setHospitalId(hospitalData[0]?.id ?? '')
      })
      .catch((requestError) => setError(messageFromError(requestError)))
      .finally(() => setLoading(false))
  }, [])

  const activeRequests = useMemo(() => requests.filter((item) =>
    !['FULFILLED', 'CANCELLED'].includes(item.status)).length, [requests])

  async function search() {
    if (!hospitalId) return
    setSearching(true)
    setError('')
    setSuccess('')
    try {
      setAvailability(await getBloodAvailability(hospitalId, bloodGroup, component, units))
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setSearching(false)
    }
  }

  async function consent() {
    setDonorWorking(true)
    setError('')
    setSuccess('')
    try {
      const updated = await createDonorConsent(contactPreference)
      setDonor(updated)
      setSuccess('Your voluntary donor consent is recorded. Blood group and eligibility still require staff verification.')
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setDonorWorking(false)
    }
  }

  async function withdraw() {
    setDonorWorking(true)
    setError('')
    setSuccess('')
    try {
      const updated = await withdrawDonorConsent()
      setDonor(updated)
      setSuccess('Your voluntary donor consent has been withdrawn.')
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setDonorWorking(false)
    }
  }

  if (loading) return <div className="grid min-h-[65vh] place-items-center bg-slate-50"><div className="flex items-center gap-3 text-sm font-bold text-slate-500"><LoaderCircle className="size-6 animate-spin text-rose-600" />Opening verified blood support...</div></div>

  return (
    <div className="min-h-screen bg-[#f7f8fb]">
      <section className="overflow-hidden bg-[linear-gradient(120deg,#3b0b18_0%,#7f1d32_58%,#b42348_100%)] text-white">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-11 sm:px-6 lg:grid-cols-[1fr_.7fr] lg:px-8 lg:py-16">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-rose-100/20 bg-white/10 px-3 py-2 text-xs font-black uppercase tracking-[.18em] text-rose-50"><ShieldCheck className="size-4" />Recently verified inventory only</div>
            <h1 className="mt-5 text-4xl font-black tracking-tight sm:text-5xl">Blood support without false assurance</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-rose-100/80 sm:text-base">Check authorized blood-bank records, see exactly when inventory was verified, and follow a clinician-created emergency request. Compatibility and transfusion decisions always remain with qualified staff.</p>
          </div>
          <div className="grid grid-cols-2 gap-3 self-end">
            <div className="rounded-2xl border border-white/10 bg-white/10 p-5 backdrop-blur"><p className="text-3xl font-black">{activeRequests}</p><p className="mt-1 text-xs font-bold text-rose-100/70">Active requests</p></div>
            <div className="rounded-2xl border border-white/10 bg-white/10 p-5 backdrop-blur"><p className="text-2xl font-black">Fresh</p><p className="mt-1 text-xs font-bold text-rose-100/70">Timestamp required</p></div>
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        <div className="mb-6 flex gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm leading-6 text-rose-950"><AlertOctagon className="mt-0.5 size-5 shrink-0" /><p><strong>Medical emergency?</strong> Do not wait for this page or travel based only on displayed inventory. Contact local emergency services or your hospital emergency desk immediately.</p></div>
        {error && <div role="alert" className="mb-5 flex gap-3 rounded-2xl border border-rose-200 bg-white p-4 text-sm font-semibold text-rose-800"><TriangleAlert className="size-5 shrink-0" />{error}</div>}
        {success && <div className="mb-5 flex gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-semibold text-emerald-800"><CheckCircle2 className="size-5 shrink-0" />{success}</div>}

        <section className="grid gap-6 lg:grid-cols-[.72fr_1.28fr]">
          <div className="h-fit rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <p className="text-xs font-black uppercase tracking-[.18em] text-rose-700">Authorized network search</p>
            <h2 className="mt-2 text-2xl font-black text-ink-950">Check verified availability</h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">This lookup does not reserve blood. Only an authorized clinician or blood-bank team can create and fulfil a request.</p>
            <div className="mt-6 space-y-4">
              <label className="block text-xs font-black text-slate-700">Hospital<select value={hospitalId} onChange={(event) => { setHospitalId(event.target.value); setAvailability([]) }} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold"><option value="">Choose hospital</option>{hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}</select></label>
              <div className="grid grid-cols-2 gap-3"><label className="block text-xs font-black text-slate-700">Blood group<select value={bloodGroup} onChange={(event) => setBloodGroup(event.target.value as BloodGroup)} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold">{Object.entries(groupLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><label className="block text-xs font-black text-slate-700">Units<input type="number" min={1} max={20} value={units} onChange={(event) => setUnits(Math.max(1, Math.min(20, Number(event.target.value) || 1)))} className="mt-2 h-12 w-full rounded-xl border border-slate-300 px-3 text-sm font-bold" /></label></div>
              <label className="block text-xs font-black text-slate-700">Component<select value={component} onChange={(event) => setComponent(event.target.value as BloodComponent)} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold">{Object.entries(componentLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
              <button onClick={() => void search()} disabled={!hospitalId || searching} className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-rose-700 text-sm font-black text-white shadow-md shadow-rose-900/15 disabled:opacity-50">{searching ? <LoaderCircle className="size-5 animate-spin" /> : <RefreshCw className="size-4" />}Check current verification</button>
            </div>
            <div className="mt-5 flex gap-3 rounded-xl bg-slate-50 p-3 text-xs leading-5 text-slate-600"><Info className="size-5 shrink-0 text-slate-500" /><p>Search is exact group + component only. SmartCare does not calculate transfusion compatibility or approve a donation.</p></div>
          </div>

          <div>
            <div><p className="text-xs font-black uppercase tracking-[.18em] text-rose-700">Ranked by verified status and distance</p><h2 className="mt-2 text-2xl font-black text-ink-950">Blood-bank results</h2></div>
            {availability.length === 0 ? <div className="mt-5 grid min-h-72 place-items-center rounded-[2rem] border border-dashed border-slate-300 bg-white p-8 text-center"><div><Droplets className="mx-auto size-9 text-rose-200" /><h3 className="mt-4 font-black text-ink-950">Run an authorized inventory check</h3><p className="mx-auto mt-2 max-w-lg text-sm leading-6 text-slate-500">Results will show a freshness timestamp. Missing or old verification is never presented as available stock.</p></div></div> : <div className="mt-5 space-y-4">{availability.map((item) => { const demo = item.bloodBankName.toLowerCase().includes('demo'); return <article key={item.bloodBankId} className="rounded-[1.75rem] border border-slate-200 bg-white p-5 shadow-sm"><div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between"><div className="flex gap-3"><span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-rose-50 text-rose-700"><Building2 className="size-5" /></span><div><div className="flex flex-wrap items-center gap-2"><h3 className="font-black text-ink-950">{item.bloodBankName}</h3>{demo && <span className="rounded-full bg-violet-100 px-2 py-1 text-[10px] font-black text-violet-800">Synthetic demo data</span>}</div><p className="mt-1 text-xs font-bold text-slate-500">{item.sourceType.replaceAll('_', ' ')}</p></div></div><span className={`w-fit rounded-full border px-3 py-2 text-[11px] font-black ${availabilityStyles[item.status]}`}>{availabilityLabels[item.status]}</span></div><div className="mt-5 grid gap-3 text-xs text-slate-600 sm:grid-cols-2"><p className="flex gap-2"><Droplets className="size-4 shrink-0 text-rose-600" /><strong>{item.availableUnits} recently verified unit{item.availableUnits === 1 ? '' : 's'}</strong></p><p className="flex gap-2"><Clock3 className="size-4 shrink-0 text-slate-400" />Last checked {displayDateTime(item.lastVerifiedAt)}</p><p className="flex gap-2"><MapPin className="size-4 shrink-0 text-slate-400" />{item.distanceKm.toFixed(1)} km · ~{item.estimatedTransferMinutes} min transfer</p><p className="flex gap-2"><Building2 className="size-4 shrink-0 text-slate-400" />{item.addressLine}</p></div><div className="mt-5 flex flex-wrap items-center gap-3 border-t border-slate-100 pt-4">{demo ? <span className="inline-flex h-10 items-center gap-2 rounded-xl border border-violet-200 bg-violet-50 px-4 text-xs font-black text-violet-800"><Phone className="size-4" />Calling disabled for demo</span> : <a href={`tel:${item.contactNumber}`} className="inline-flex h-10 items-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 text-xs font-black text-rose-800"><Phone className="size-4" />Call blood-bank desk</a>}<p className="text-[11px] text-slate-500">{demo ? 'Synthetic inventory is for workflow testing only.' : 'Reconfirm availability before any transfer or travel.'}</p></div></article> })}</div>}
          </div>
        </section>

        <section className="mt-12 border-t border-slate-200 pt-10">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-black uppercase tracking-[.18em] text-rose-700">My protected status</p><h2 className="mt-2 text-2xl font-black text-ink-950">Emergency blood requests</h2><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">Requests appear only after an authorized clinician or blood-bank staff member records the need.</p></div><Link to="/notifications" className="text-sm font-black text-rose-700 hover:underline">Open request notifications</Link></div>
          {requests.length === 0 ? <div className="mt-5 rounded-[2rem] border border-dashed border-slate-300 bg-white p-8 text-center"><ShieldCheck className="mx-auto size-8 text-slate-300" /><h3 className="mt-4 font-black text-ink-950">No blood-support request</h3><p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-slate-500">Patients cannot self-issue a blood request. This protects against unsafe group, component or unit selection.</p></div> : <div className="mt-5 grid gap-5 lg:grid-cols-2">{requests.map((request) => <article key={request.id} className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm"><div className="p-5 sm:p-6"><div className="flex items-start justify-between gap-3"><div><div className="flex items-center gap-2"><span className="grid size-11 place-items-center rounded-2xl bg-rose-50 text-lg font-black text-rose-800">{groupLabels[request.bloodGroup]}</span><div><h3 className="font-black text-ink-950">{componentLabels[request.component]}</h3><p className="text-xs font-bold text-rose-700">{request.urgency} · {request.requestedUnits} unit{request.requestedUnits === 1 ? '' : 's'}</p></div></div></div><span className={`rounded-full border px-3 py-2 text-[10px] font-black ${requestStyles[request.status]}`}>{requestLabels[request.status]}</span></div><div className="mt-5 h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-rose-600" style={{ width: `${Math.min(100, request.requestedUnits ? (request.matchedUnits / request.requestedUnits) * 100 : 0)}%` }} /></div><div className="mt-2 flex justify-between text-[11px] font-bold text-slate-500"><span>{request.matchedUnits} matched</span><span>{request.requestedUnits} requested</span></div><div className="mt-5 space-y-2 text-xs leading-5 text-slate-600"><p><strong>Hospital:</strong> {request.hospitalName}</p><p><strong>Created by:</strong> {request.createdBy}</p><p><strong>Recorded:</strong> {displayDateTime(request.createdAt)}</p><p><strong>Clinical reason:</strong> {request.clinicalReason}</p></div></div>{request.allocations.length > 0 && <div className="border-t border-slate-100 bg-slate-50/70 p-5 sm:px-6"><p className="text-[10px] font-black uppercase tracking-[.16em] text-slate-500">Authorized allocations</p><div className="mt-3 space-y-3">{request.allocations.map((allocation) => <div key={allocation.id} className="rounded-xl border border-slate-200 bg-white p-3 text-xs"><div className="flex items-start justify-between gap-3"><div><p className="font-black text-ink-950">{allocation.bloodBankName}</p><p className="mt-1 text-slate-500">{allocation.units} unit{allocation.units === 1 ? '' : 's'} · expires {displayDate(allocation.expiresOn)}</p></div><span className="font-black text-slate-600">{allocation.status}</span></div></div>)}</div></div>}</article>)}</div>}
        </section>

        <section className="mt-12 grid gap-6 rounded-[2rem] border border-rose-100 bg-[linear-gradient(120deg,#fff7f8,#ffffff)] p-5 sm:p-7 lg:grid-cols-[1fr_.8fr]">
          <div><span className="grid size-12 place-items-center rounded-2xl bg-rose-100 text-rose-700"><HeartHandshake className="size-6" /></span><p className="mt-5 text-xs font-black uppercase tracking-[.18em] text-rose-700">Optional voluntary registry</p><h2 className="mt-2 text-2xl font-black text-ink-950">Donor consent stays private</h2><p className="mt-3 text-sm leading-6 text-slate-600">Consent only allows authorized staff to consider contacting you when verified inventory is insufficient. It does not confirm your blood group, eligibility, compatibility or approval to donate.</p><div className="mt-4 flex gap-3 rounded-xl bg-white p-3 text-xs leading-5 text-slate-600"><Users className="size-5 shrink-0 text-rose-600" /><p>Your contact and verified blood group are never shown in the public availability search.</p></div></div>
          <div className="rounded-2xl border border-slate-200 bg-white p-5">{donor?.consentActive ? <><div className="flex items-start justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-wider text-emerald-700">Consent active</p><h3 className="mt-1 font-black text-ink-950">{donor.eligibilityStatus.replaceAll('_', ' ')}</h3></div><BadgeCheck className="size-6 text-emerald-600" /></div><div className="mt-4 space-y-2 text-xs text-slate-600"><p>Contact preference: <strong>{donor.contactPreference}</strong></p><p>Verified blood group: <strong>{donor.verifiedBloodGroup ? groupLabels[donor.verifiedBloodGroup] : 'Pending staff verification'}</strong></p><p>Eligibility checked: <strong>{displayDateTime(donor.eligibilityVerifiedAt)}</strong></p></div><button disabled={donorWorking} onClick={() => void withdraw()} className="mt-5 h-11 w-full rounded-xl border border-rose-200 text-sm font-black text-rose-700 disabled:opacity-50">Withdraw consent</button></> : <><label className="text-xs font-black text-slate-700">How may authorized staff contact you?<select value={contactPreference} onChange={(event) => setContactPreference(event.target.value as DonorOptIn['contactPreference'])} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm font-bold"><option value="MOBILE">Mobile call</option><option value="SMS">SMS</option><option value="WHATSAPP">WhatsApp</option><option value="EMAIL">Email</option></select></label><button disabled={donorWorking} onClick={() => void consent()} className="mt-4 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-rose-700 text-sm font-black text-white disabled:opacity-50">{donorWorking && <LoaderCircle className="size-4 animate-spin" />}Record my explicit consent</button><p className="mt-3 text-[11px] leading-5 text-slate-500">You can withdraw at any time. A qualified team must independently verify eligibility before contact.</p></>}</div>
        </section>
      </main>
    </div>
  )
}
