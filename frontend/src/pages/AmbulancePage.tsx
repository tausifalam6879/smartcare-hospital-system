import {
  AlertOctagon, Ambulance, BadgeCheck, Building2, CheckCircle2, ChevronRight, CircleDot,
  Clock3, Headphones, LoaderCircle, LocateFixed, MapPin, Navigation, Phone, RefreshCw,
  Route, ShieldAlert, ShieldCheck, Siren, Truck, Users,
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { messageFromError } from '../services/api'
import { recommendAvailableAmbulance } from '../services/ambulanceRecommendation'
import {
  acknowledgeAmbulance, advanceAmbulanceRequest, assignAmbulance, cancelAmbulanceRequest,
  createAmbulanceRequest, getAmbulanceAvailability, getAmbulanceFleet,
  getAmbulanceWorklist, getMyAmbulanceRequests, type Ambulance as AmbulanceVehicle,
  type AmbulanceAvailability, type AmbulancePriority, type AmbulanceRequest,
  type AmbulanceRequestStatus,
} from '../services/ambulances'
import { getNavigationHospitals, type HospitalSummary } from '../services/navigation'

const dispatcherRoles = ['AMBULANCE_DISPATCHER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']

const statusLabel: Record<AmbulanceRequestStatus, string> = {
  REQUESTED: 'Awaiting dispatcher', ASSIGNED: 'Vehicle assigned', ACKNOWLEDGED: 'Crew acknowledged',
  EN_ROUTE_TO_PATIENT: 'En route to pickup', PATIENT_PICKED_UP: 'Patient picked up',
  EN_ROUTE_TO_HOSPITAL: 'En route to hospital', ARRIVED: 'Arrived at hospital',
  COMPLETED: 'Transport completed', CANCELLED: 'Cancelled',
}

const statusTone: Record<AmbulanceRequestStatus, string> = {
  REQUESTED: 'border-amber-200 bg-amber-50 text-amber-900',
  ASSIGNED: 'border-blue-200 bg-blue-50 text-blue-900',
  ACKNOWLEDGED: 'border-cyan-200 bg-cyan-50 text-cyan-900',
  EN_ROUTE_TO_PATIENT: 'border-violet-200 bg-violet-50 text-violet-900',
  PATIENT_PICKED_UP: 'border-fuchsia-200 bg-fuchsia-50 text-fuchsia-900',
  EN_ROUTE_TO_HOSPITAL: 'border-indigo-200 bg-indigo-50 text-indigo-900',
  ARRIVED: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  COMPLETED: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  CANCELLED: 'border-slate-200 bg-slate-100 text-slate-600',
}

const nextStatus: Partial<Record<AmbulanceRequestStatus, AmbulanceRequestStatus>> = {
  ACKNOWLEDGED: 'EN_ROUTE_TO_PATIENT', EN_ROUTE_TO_PATIENT: 'PATIENT_PICKED_UP',
  PATIENT_PICKED_UP: 'EN_ROUTE_TO_HOSPITAL', EN_ROUTE_TO_HOSPITAL: 'ARRIVED', ARRIVED: 'COMPLETED',
}

function dateTime(value?: string) {
  if (!value) return 'Not recorded'
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function priorityLabel(priority: AmbulancePriority) {
  return priority.charAt(0) + priority.slice(1).toLowerCase()
}

export function AmbulancePage() {
  const { session } = useAuth()
  const isDispatcher = Boolean(session?.user.roles.some((role) => dispatcherRoles.includes(role)))
  const [hospitals, setHospitals] = useState<HospitalSummary[]>([])
  const [hospitalId, setHospitalId] = useState('')
  const [availability, setAvailability] = useState<AmbulanceAvailability | null>(null)
  const [requests, setRequests] = useState<AmbulanceRequest[]>([])
  const [fleet, setFleet] = useState<AmbulanceVehicle[]>([])
  const [assignments, setAssignments] = useState<Record<string, string>>({})
  const [pickupAddress, setPickupAddress] = useState('')
  const [pickupLandmark, setPickupLandmark] = useState('')
  const [contactNumber, setContactNumber] = useState(session?.user.mobileNumber ?? '')
  const [assistanceNotes, setAssistanceNotes] = useState('')
  const [priority, setPriority] = useState<AmbulancePriority>('EMERGENCY')
  const [understood, setUnderstood] = useState(false)
  const [loading, setLoading] = useState(true)
  const [working, setWorking] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    getNavigationHospitals().then((data) => {
      setHospitals(data)
      setHospitalId(data[0]?.id ?? '')
    }).catch((requestError) => setError(messageFromError(requestError)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (!hospitalId) return
    void refresh(hospitalId)
    const timer = window.setInterval(() => void refresh(hospitalId), 10_000)
    return () => window.clearInterval(timer)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hospitalId, isDispatcher])

  async function refresh(selectedHospital = hospitalId) {
    if (!selectedHospital) return
    setError('')
    try {
      if (isDispatcher) {
        const [availabilityData, worklistData, fleetData] = await Promise.all([
          getAmbulanceAvailability(selectedHospital), getAmbulanceWorklist(selectedHospital),
          getAmbulanceFleet(selectedHospital),
        ])
        setAvailability(availabilityData)
        setRequests(worklistData)
        setFleet(fleetData)
      } else {
        const [availabilityData, ownRequests] = await Promise.all([
          getAmbulanceAvailability(selectedHospital), getMyAmbulanceRequests(),
        ])
        setAvailability(availabilityData)
        setRequests(ownRequests)
      }
    } catch (requestError) {
      setError(messageFromError(requestError))
    }
  }

  async function submitRequest(event: React.FormEvent) {
    event.preventDefault()
    if (!understood) return
    setWorking('create')
    setError('')
    setSuccess('')
    try {
      const created = await createAmbulanceRequest({
        hospitalId, transportType: 'PATIENT_TRANSPORT', priority,
        pickupAddress, pickupLandmark: pickupLandmark || undefined,
        contactNumber, assistanceNotes: assistanceNotes || undefined,
        idempotencyKey: crypto.randomUUID(),
      })
      setRequests((current) => [created, ...current])
      setPickupAddress('')
      setPickupLandmark('')
      setAssistanceNotes('')
      setUnderstood(false)
      setSuccess('Request recorded. It is waiting for authorized dispatcher review; no vehicle is assigned yet.')
      await refresh()
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setWorking('')
    }
  }

  async function perform(key: string, action: () => Promise<AmbulanceRequest>) {
    setWorking(key)
    setError('')
    setSuccess('')
    try {
      await action()
      await refresh()
      setSuccess('Operational status saved and audited.')
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setWorking('')
    }
  }

  const availableFleet = useMemo(() => fleet.filter((item) => item.status === 'AVAILABLE'), [fleet])
  const activeCount = useMemo(() => requests.filter((item) => !['COMPLETED', 'CANCELLED'].includes(item.status)).length, [requests])

  if (loading) return <div className="grid min-h-[65vh] place-items-center bg-slate-50"><div className="flex items-center gap-3 text-sm font-bold text-slate-500"><LoaderCircle className="size-6 animate-spin text-red-600" />Opening ambulance coordination...</div></div>

  return (
    <div className="min-h-screen bg-[#f6f8fb]">
      <section className="overflow-hidden bg-[linear-gradient(118deg,#071b35_0%,#123d64_56%,#0f6471_100%)] text-white">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-11 sm:px-6 lg:grid-cols-[1fr_.72fr] lg:px-8 lg:py-16">
          <div>
            <div className="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-2 text-xs font-black uppercase tracking-[.16em] text-cyan-100"><ShieldCheck className="size-4" />Human-authorized dispatch</div>
            <h1 className="mt-5 text-4xl font-black tracking-tight sm:text-5xl">Ambulance coordination with accountable handoffs</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-blue-100/80 sm:text-base">Request hospital transport, follow dispatcher-confirmed stages, and see the latest shared area. SmartCare never auto-dispatches a vehicle or replaces emergency services.</p>
          </div>
          <div className="grid grid-cols-2 gap-3 self-end">
            <div className="rounded-2xl border border-white/10 bg-white/10 p-5 backdrop-blur"><p className="text-3xl font-black">{availability?.availableVehicles ?? '—'}</p><p className="mt-1 text-xs font-bold text-blue-100/70">Currently marked available</p></div>
            <div className="rounded-2xl border border-white/10 bg-white/10 p-5 backdrop-blur"><p className="text-3xl font-black">{activeCount}</p><p className="mt-1 text-xs font-bold text-blue-100/70">Active {isDispatcher ? 'worklist items' : 'requests'}</p></div>
          </div>
        </div>
      </section>

      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        <div className="mb-6 grid gap-3 lg:grid-cols-[1fr_auto]">
          <div className="flex gap-3 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm leading-6 text-red-950"><AlertOctagon className="mt-0.5 size-5 shrink-0" /><p><strong>Life-threatening emergency?</strong> Contact your local official emergency service immediately. Do not wait for a SmartCare request, availability count, or page update.</p></div>
          <button onClick={() => void refresh()} className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-5 text-sm font-black text-slate-700 hover:border-care-300 hover:bg-care-50"><RefreshCw className="size-4" />Refresh</button>
        </div>
        {availability?.syntheticData && <div className="mb-6 flex gap-3 rounded-2xl border border-violet-200 bg-violet-50 p-4 text-sm text-violet-950"><ShieldAlert className="mt-0.5 size-5 shrink-0" /><p><strong>Synthetic demo fleet:</strong> vehicle availability on this development hospital is test data, not a real ambulance service. Do not call or rely on it for care.</p></div>}
        {error && <div role="alert" className="mb-5 rounded-2xl border border-red-200 bg-white p-4 text-sm font-bold text-red-800">{error}</div>}
        {success && <div role="status" className="mb-5 flex items-center gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-bold text-emerald-900"><CheckCircle2 className="size-5" />{success}</div>}

        <section className="mb-7 rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
          <div className="grid gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
            <label className="text-sm font-black text-ink-950">Coordinating hospital
              <select value={hospitalId} onChange={(event) => setHospitalId(event.target.value)} className="mt-2 min-h-12 w-full rounded-xl border border-slate-300 bg-white px-4 font-semibold outline-none focus:border-care-500">
                {hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}
              </select>
            </label>
            <div className="rounded-xl bg-slate-50 px-4 py-3 text-xs leading-5 text-slate-600"><Clock3 className="mr-2 inline size-4 text-care-700" />Checked {dateTime(availability?.checkedAt)}<br /><strong>Availability is indicative until assigned.</strong></div>
          </div>
        </section>

        {!isDispatcher ? (
          <div className="grid gap-7 lg:grid-cols-[.88fr_1.12fr]">
            <form onSubmit={submitRequest} className="h-fit rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
              <div className="flex items-start gap-3"><span className="grid size-11 shrink-0 place-items-center rounded-xl bg-red-50 text-red-700"><Siren className="size-5" /></span><div><h2 className="text-xl font-black text-ink-950">Request patient transport</h2><p className="mt-1 text-sm leading-6 text-slate-600">Creates a dispatch request—not a confirmed ambulance.</p></div></div>
              <div className="mt-6 space-y-5">
                <label className="block text-sm font-black text-ink-950">Priority
                  <select value={priority} onChange={(event) => setPriority(event.target.value as AmbulancePriority)} className="mt-2 min-h-12 w-full rounded-xl border border-slate-300 px-4 font-semibold outline-none focus:border-care-500"><option value="EMERGENCY">Emergency</option><option value="URGENT">Urgent</option><option value="SCHEDULED">Scheduled transport</option></select>
                </label>
                <label className="block text-sm font-black text-ink-950">Pickup address
                  <textarea required maxLength={500} value={pickupAddress} onChange={(event) => setPickupAddress(event.target.value)} rows={3} placeholder="Full address with area and postal code" className="mt-2 w-full rounded-xl border border-slate-300 p-4 font-medium outline-none focus:border-care-500" />
                </label>
                <label className="block text-sm font-black text-ink-950">Nearby landmark <span className="font-medium text-slate-400">(optional)</span>
                  <input maxLength={180} value={pickupLandmark} onChange={(event) => setPickupLandmark(event.target.value)} placeholder="Gate, building or landmark" className="mt-2 min-h-12 w-full rounded-xl border border-slate-300 px-4 outline-none focus:border-care-500" />
                </label>
                <label className="block text-sm font-black text-ink-950">Contact mobile number
                  <input required pattern="^\+?[1-9][0-9]{7,14}$" value={contactNumber} onChange={(event) => setContactNumber(event.target.value)} className="mt-2 min-h-12 w-full rounded-xl border border-slate-300 px-4 outline-none focus:border-care-500" />
                </label>
                <label className="block text-sm font-black text-ink-950">Access or assistance notes <span className="font-medium text-slate-400">(optional)</span>
                  <textarea maxLength={500} value={assistanceNotes} onChange={(event) => setAssistanceNotes(event.target.value)} rows={2} placeholder="Stairs, gate access or mobility support—do not enter detailed diagnosis" className="mt-2 w-full rounded-xl border border-slate-300 p-4 outline-none focus:border-care-500" />
                </label>
                <label className="flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950"><input type="checkbox" checked={understood} onChange={(event) => setUnderstood(event.target.checked)} className="mt-1 size-4" /><span>I understand that submitting does not assign a vehicle. I will use official emergency services if immediate help is needed.</span></label>
                <button disabled={!understood || working === 'create'} className="inline-flex min-h-13 w-full items-center justify-center gap-2 rounded-xl bg-red-700 px-5 text-sm font-black text-white shadow-lg shadow-red-800/15 hover:bg-red-800 disabled:cursor-not-allowed disabled:opacity-50">{working === 'create' ? <LoaderCircle className="size-5 animate-spin" /> : <Ambulance className="size-5" />}Submit for dispatcher review</button>
              </div>
            </form>
            <RequestList requests={requests} working={working} onCancel={(request) => perform(`cancel:${request.id}`, () => cancelAmbulanceRequest(request.id, 'Cancelled by patient before dispatch.'))} />
          </div>
        ) : (
          <div className="grid gap-7 lg:grid-cols-[.72fr_1.28fr]">
            <FleetPanel fleet={fleet} />
            <DispatcherWorklist requests={requests} fleet={availableFleet} assignments={assignments} setAssignments={setAssignments} working={working} perform={perform} />
          </div>
        )}
      </main>
    </div>
  )
}

function patientNextStep(request: AmbulanceRequest) {
  const steps: Record<AmbulanceRequestStatus, string> = {
    REQUESTED: 'Keep your phone reachable. A dispatcher must review the request and select an available vehicle.',
    ASSIGNED: 'The vehicle is selected. Wait for crew acknowledgement before relying on its movement status.',
    ACKNOWLEDGED: 'The crew has accepted the task and will update when it starts toward the pickup.',
    EN_ROUTE_TO_PATIENT: 'The vehicle is moving toward the pickup. Keep access clear and your phone reachable.',
    PATIENT_PICKED_UP: 'The crew has recorded pickup and will update the hospital travel stage.',
    EN_ROUTE_TO_HOSPITAL: 'Transport to the hospital is in progress. Follow crew safety instructions.',
    ARRIVED: 'Arrival is recorded. Hospital handoff is the next operational step.',
    COMPLETED: 'The dispatcher has closed the transport workflow.', CANCELLED: 'No further dispatch action will occur for this request.',
  }
  return steps[request.status]
}

function dispatcherNextStep(request: AmbulanceRequest) {
  const steps: Record<AmbulanceRequestStatus, string> = {
    REQUESTED: 'Call requester if clarification is needed, select an available vehicle and assign it.',
    ASSIGNED: 'Confirm the crew received the task, then record acknowledgement.',
    ACKNOWLEDGED: 'Record departure when the crew starts toward the pickup.',
    EN_ROUTE_TO_PATIENT: 'Monitor the handoff and record patient pickup.',
    PATIENT_PICKED_UP: 'Record departure toward the receiving hospital.',
    EN_ROUTE_TO_HOSPITAL: 'Record arrival and receiving-desk handoff.', ARRIVED: 'Complete the request after handoff.',
    COMPLETED: 'No action pending.', CANCELLED: 'No action pending.',
  }
  return steps[request.status]
}

function RequestList({ requests, working, onCancel }: { requests: AmbulanceRequest[]; working: string; onCancel: (request: AmbulanceRequest) => void }) {
  return <section><div className="flex items-center justify-between"><div><p className="text-xs font-black uppercase tracking-[.18em] text-care-700">My transport requests</p><h2 className="mt-2 text-2xl font-black text-ink-950">Dispatcher-confirmed timeline</h2></div><Headphones className="size-7 text-care-600" /></div>
    <div className="mt-5 space-y-5">{requests.length === 0 && <div className="rounded-3xl border border-dashed border-slate-300 bg-white p-10 text-center"><Ambulance className="mx-auto size-9 text-slate-300" /><p className="mt-3 font-black text-slate-700">No transport requests yet</p><p className="mt-1 text-sm text-slate-500">New requests will appear here after submission.</p></div>}
      {requests.map((request) => <article key={request.id} className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm"><div className="border-b border-slate-100 p-5 sm:p-6"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-[.13em] text-slate-400">{priorityLabel(request.priority)} · {request.transportType === 'PATIENT_TRANSPORT' ? 'Patient transport' : 'Blood transport'}</p><h3 className="mt-2 text-lg font-black text-ink-950">{request.hospitalName}</h3><p className="mt-1 text-sm text-slate-600"><MapPin className="mr-1 inline size-4" />{request.pickupAddress}</p></div><span className={`rounded-full border px-3 py-1.5 text-xs font-black ${statusTone[request.status]}`}>{statusLabel[request.status]}</span></div>
        {request.ambulance ? <div className="mt-5 grid gap-3 rounded-2xl border border-blue-100 bg-blue-50 p-4 sm:grid-cols-2"><div><p className="text-[10px] font-black uppercase tracking-[.14em] text-blue-500">Assigned vehicle</p><p className="mt-1 font-black text-blue-950">{request.ambulance.callSign} · {request.ambulance.registrationNumber}</p></div><div><p className="text-[10px] font-black uppercase tracking-[.14em] text-blue-500">Last shared area</p><p className="mt-1 font-bold text-blue-950">{request.ambulance.currentArea ?? 'Not shared'} <span className="block text-xs font-medium text-blue-700">{dateTime(request.ambulance.locationUpdatedAt)}</span></p></div>{request.ambulance.synthetic && <p className="sm:col-span-2 text-xs font-bold text-violet-800">Synthetic demo vehicle—not a real ambulance.</p>}</div> : <div className="mt-5 flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950"><Clock3 className="mt-0.5 size-5 shrink-0" /><p><strong>No vehicle assigned.</strong> Authorized dispatcher review is pending.</p></div>}
        <div className="mt-4 rounded-xl border border-blue-100 bg-blue-50 p-3 text-xs leading-5 text-blue-950"><strong>What happens next:</strong> {patientNextStep(request)}</div>
        {request.status === 'REQUESTED' && <button disabled={working === `cancel:${request.id}`} onClick={() => onCancel(request)} className="mt-4 text-sm font-black text-red-700 hover:underline">Cancel before dispatch</button>}
      </div><div className="p-5 sm:p-6"><h4 className="text-sm font-black text-ink-950">Status timeline</h4><ol className="mt-4 space-y-4">{request.timeline.map((event, index) => <li key={`${event.toStatus}-${event.eventAt}-${index}`} className="grid grid-cols-[1.25rem_1fr] gap-3"><span className="relative mt-0.5 grid size-5 place-items-center rounded-full bg-care-100 text-care-700"><CircleDot className="size-3" />{index < request.timeline.length - 1 && <span className="absolute top-5 h-8 w-px bg-slate-200" />}</span><div><p className="text-sm font-black text-slate-800">{statusLabel[event.toStatus]}</p><p className="text-xs text-slate-500">{event.actorLabel} · {dateTime(event.eventAt)}</p>{event.note && <p className="mt-1 text-xs leading-5 text-slate-600">{event.note}</p>}</div></li>)}</ol></div></article>)}
    </div></section>
}

function FleetPanel({ fleet }: { fleet: AmbulanceVehicle[] }) {
  return <section className="h-fit rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"><div className="flex items-center justify-between"><div><p className="text-xs font-black uppercase tracking-[.18em] text-care-700">Dispatcher fleet</p><h2 className="mt-2 text-xl font-black text-ink-950">Vehicle readiness</h2></div><Truck className="size-6 text-care-700" /></div><div className="mt-5 space-y-3">{fleet.map((vehicle) => <div key={vehicle.id} className="rounded-2xl border border-slate-200 p-4"><div className="flex items-start justify-between gap-3"><div><p className="font-black text-ink-950">{vehicle.callSign}</p><p className="text-xs text-slate-500">{vehicle.registrationNumber}</p></div><span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${vehicle.status === 'AVAILABLE' ? 'bg-emerald-100 text-emerald-800' : vehicle.status === 'OUT_OF_SERVICE' ? 'bg-slate-200 text-slate-700' : 'bg-blue-100 text-blue-800'}`}>{vehicle.status.replaceAll('_', ' ')}</span></div><p className="mt-3 text-sm font-bold text-slate-700"><Users className="mr-1.5 inline size-4" />{vehicle.crewLabel}</p><p className="mt-1 text-xs text-slate-500"><LocateFixed className="mr-1.5 inline size-4" />{vehicle.currentArea ?? 'Location not updated'}</p>{vehicle.crewContact && <a href={`tel:${vehicle.crewContact}`} className="mt-3 inline-flex items-center gap-2 text-xs font-black text-care-700 hover:underline"><Phone className="size-4" />Call crew {vehicle.crewContact}</a>}{vehicle.synthetic && <p className="mt-2 text-xs font-black text-violet-700">Synthetic demo</p>}</div>)}</div></section>
}

function DispatcherWorklist({ requests, fleet, assignments, setAssignments, working, perform }: { requests: AmbulanceRequest[]; fleet: AmbulanceVehicle[]; assignments: Record<string, string>; setAssignments: React.Dispatch<React.SetStateAction<Record<string, string>>>; working: string; perform: (key: string, action: () => Promise<AmbulanceRequest>) => Promise<void> }) {
  return <section><div><p className="text-xs font-black uppercase tracking-[.18em] text-care-700">Authorized operations</p><h2 className="mt-2 text-2xl font-black text-ink-950">Dispatch worklist</h2><p className="mt-2 text-sm text-slate-600">Every assignment and stage change is explicit, locked and audited. New requests refresh every 10 seconds.</p></div><div className="mt-5 space-y-4">{requests.map((request) => { const next = nextStatus[request.status]; const recommendation = request.status === 'REQUESTED' ? recommendAvailableAmbulance(request, fleet) : null; return <article key={request.id} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6"><div className="flex flex-wrap justify-between gap-3"><div><p className="text-xs font-black uppercase tracking-[.13em] text-red-600">{priorityLabel(request.priority)} · {request.transportType.replaceAll('_', ' ')}</p><h3 className="mt-2 text-lg font-black text-ink-950">{request.pickupAddress}</h3><p className="mt-1 text-sm text-slate-500">To {request.hospitalName} · contact {request.contactNumber}</p></div><span className={`h-fit rounded-full border px-3 py-1.5 text-xs font-black ${statusTone[request.status]}`}>{statusLabel[request.status]}</span></div><div className="mt-4 flex flex-wrap items-center gap-3 rounded-2xl border border-blue-100 bg-blue-50 p-3"><p className="mr-auto text-xs text-blue-950"><strong>Assigned task:</strong> {dispatcherNextStep(request)}</p><a href={`tel:${request.contactNumber}`} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-care-700 px-4 text-xs font-black text-white"><Phone className="size-4" />Call requester</a></div>{request.assistanceNotes && <p className="mt-4 rounded-xl bg-slate-50 p-3 text-sm text-slate-700"><ShieldAlert className="mr-2 inline size-4 text-amber-600" />{request.assistanceNotes}</p>}{request.status === 'REQUESTED' && <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-950"><strong>Decision-support suggestion:</strong> {recommendation ? `${recommendation.vehicle.callSign} — ${recommendation.reason}.` : 'No available vehicle.'} <span className="block mt-1 font-semibold">This is not GPS routing; dispatcher must verify equipment, crew and actual distance before assignment.</span></div>}
      <div className="mt-5 flex flex-col gap-3 border-t border-slate-100 pt-5 sm:flex-row">{request.status === 'REQUESTED' && <><select value={assignments[request.id] ?? ''} onChange={(event) => setAssignments((current) => ({ ...current, [request.id]: event.target.value }))} className="min-h-11 flex-1 rounded-xl border border-slate-300 px-3 text-sm font-bold"><option value="">Select available vehicle</option>{fleet.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.callSign} · {vehicle.registrationNumber}</option>)}</select><button disabled={!assignments[request.id] || working === `assign:${request.id}`} onClick={() => void perform(`assign:${request.id}`, () => assignAmbulance(request.id, assignments[request.id]))} className="min-h-11 rounded-xl bg-care-700 px-5 text-sm font-black text-white disabled:opacity-50">Assign vehicle</button></>}{request.status === 'ASSIGNED' && <>{request.ambulance?.crewContact && <a href={`tel:${request.ambulance.crewContact}`} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-care-300 bg-care-50 px-5 text-sm font-black text-care-800"><Phone className="size-4" />Call assigned crew</a>}<button onClick={() => void perform(`ack:${request.id}`, () => acknowledgeAmbulance(request.id))} className="min-h-11 rounded-xl bg-care-700 px-5 text-sm font-black text-white"><BadgeCheck className="mr-2 inline size-4" />Record crew acknowledgement</button></>}{next && <button onClick={() => void perform(`next:${request.id}`, () => advanceAmbulanceRequest(request.id, next))} className="min-h-11 rounded-xl bg-care-700 px-5 text-sm font-black text-white">Move to {statusLabel[next]} <ChevronRight className="ml-1 inline size-4" /></button>}</div>
      {request.ambulance && <div className="mt-4 flex flex-wrap gap-4 text-xs font-bold text-slate-600"><span><Ambulance className="mr-1 inline size-4" />{request.ambulance.callSign}</span><span><Navigation className="mr-1 inline size-4" />{request.ambulance.currentArea ?? 'Area not shared'}</span><span><Route className="mr-1 inline size-4" />Updated {dateTime(request.statusUpdatedAt)}</span></div>}</article>})}</div></section>
}
