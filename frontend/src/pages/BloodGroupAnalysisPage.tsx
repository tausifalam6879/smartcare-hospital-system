import {
  AlertTriangle, BadgeCheck, Beaker, Camera, CheckCircle2, Eye, FileImage, FlaskConical,
  LoaderCircle, Microscope, RefreshCw, ShieldAlert, ShieldCheck, Upload, XCircle,
} from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api, messageFromError } from '../services/api'
import type { BloodGroup } from '../services/bloodBank'
import {
  getBloodGroupAnalysisWorklist, getMyBloodGroupAnalyses, loadBloodGroupImage,
  recordBloodGroupObservations, rejectBloodGroupAnalysis, submitBloodGroupImage,
  verifyBloodGroupAnalysis, type BloodGroupAnalysis, type BloodGroupAnalysisStatus,
} from '../services/bloodGroupAnalysis'

type Hospital = { id: string; name: string; city: string }

const staffRoles = ['LAB_TECHNICIAN', 'BLOOD_BANK_STAFF', 'HOSPITAL_ADMIN', 'SUPER_ADMIN']
const observationRoles = ['LAB_TECHNICIAN', 'BLOOD_BANK_STAFF']

const groupLabels: Record<BloodGroup, string> = {
  A_POSITIVE: 'A+', A_NEGATIVE: 'A−', B_POSITIVE: 'B+', B_NEGATIVE: 'B−',
  AB_POSITIVE: 'AB+', AB_NEGATIVE: 'AB−', O_POSITIVE: 'O+', O_NEGATIVE: 'O−',
}

const statusLabels: Record<BloodGroupAnalysisStatus, string> = {
  SUBMITTED: 'Awaiting authorized review',
  OBSERVATIONS_RECORDED: 'Independent verification required',
  LAB_VERIFIED: 'Laboratory verified',
  REJECTED: 'Image rejected',
}

const statusStyles: Record<BloodGroupAnalysisStatus, string> = {
  SUBMITTED: 'border-blue-200 bg-blue-50 text-blue-800',
  OBSERVATIONS_RECORDED: 'border-amber-200 bg-amber-50 text-amber-900',
  LAB_VERIFIED: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  REJECTED: 'border-rose-200 bg-rose-50 text-rose-800',
}

function dateTime(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

function ResultBoundary({ item }: { item: BloodGroupAnalysis }) {
  return (
    <div className="mt-4 space-y-3">
      {item.modelInferenceStatus === 'NOT_CONFIGURED' && <p className="flex gap-2 rounded-xl bg-slate-100 p-3 text-xs font-semibold leading-5 text-slate-700"><ShieldAlert className="mt-0.5 size-4 shrink-0" />Automated image model is not configured. RaahMediQ Health produced no AI blood-group result.</p>}
      {item.modelInferenceStatus === 'FAILED' && <p className="rounded-xl bg-amber-50 p-3 text-xs font-semibold text-amber-900">Automated analysis failed safely; laboratory review remains available.</p>}
      {item.modelSuggestedGroup && <p className="rounded-xl border border-violet-200 bg-violet-50 p-3 text-xs text-violet-900"><strong>Experimental model suggestion:</strong> {groupLabels[item.modelSuggestedGroup]}{item.modelConfidence != null ? ` · ${(item.modelConfidence * 100).toFixed(1)}%` : ''}. This is not a confirmed result.</p>}
      {item.verifiedGroup ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4"><p className="text-xs font-extrabold uppercase tracking-wider text-emerald-700">Independently lab-verified</p><p className="mt-1 text-3xl font-black text-emerald-950">{groupLabels[item.verifiedGroup]}</p><p className="mt-1 text-xs leading-5 text-emerald-800">Reconfirm with the treating laboratory before transfusion, donation, or clinical use.</p></div> : <p className="flex gap-2 text-sm font-bold text-slate-700"><AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-600" />No verified blood group is available from this submission.</p>}
    </div>
  )
}

function PatientView() {
  const [hospitals, setHospitals] = useState<Hospital[]>([])
  const [hospitalId, setHospitalId] = useState('')
  const [items, setItems] = useState<BloodGroupAnalysis[]>([])
  const [file, setFile] = useState<File | null>(null)
  const [acknowledged, setAcknowledged] = useState(false)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [hospitalResponse, analyses] = await Promise.all([
        api.get<Hospital[]>('/api/v1/hospitals'), getMyBloodGroupAnalyses(),
      ])
      setHospitals(hospitalResponse.data)
      setHospitalId((current) => current || hospitalResponse.data[0]?.id || '')
      setItems(analyses)
    } catch (requestError) { setError(messageFromError(requestError)) }
    finally { setLoading(false) }
  }

  useEffect(() => { void load() }, [])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!file || !hospitalId || !acknowledged) return
    setUploading(true)
    setError('')
    setSuccess('')
    try {
      const submitted = await submitBloodGroupImage({ hospitalId, safetyAcknowledged: true, file })
      setItems((all) => [submitted, ...all.filter((item) => item.id !== submitted.id)])
      setFile(null)
      setAcknowledged(false)
      setSuccess('Image stored privately. Authorized laboratory review is now pending.')
    } catch (requestError) { setError(messageFromError(requestError)) }
    finally { setUploading(false) }
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="max-w-3xl"><p className="text-xs font-extrabold uppercase tracking-[.2em] text-violet-700">Experimental decision support</p><h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">Blood-slide image review, with humans in control</h1><p className="mt-3 leading-7 text-slate-600">Upload a clear Anti-A, Anti-B and Anti-D slide image for an authorized review workflow. The image alone is never treated as a validated blood-group test.</p></div>
      <div className="mt-8 grid gap-6 lg:grid-cols-[.9fr_1.1fr]">
        <form onSubmit={submit} className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-violet-50 text-violet-700"><Camera className="size-6" /></span><div><h2 className="font-black text-ink-950">Submit slide image</h2><p className="text-xs text-slate-500">Private JPG or PNG · maximum 10 MB</p></div></div>
          <label className="mt-6 block text-xs font-bold text-slate-600">Review hospital<select value={hospitalId} onChange={(event) => setHospitalId(event.target.value)} required className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm">{hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name} · {hospital.city}</option>)}</select></label>
          <label className="mt-4 block text-xs font-bold text-slate-600">Blood-slide image<input type="file" accept="image/png,image/jpeg" onChange={(event) => setFile(event.target.files?.[0] ?? null)} required className="mt-1.5 block w-full rounded-xl border border-slate-300 p-3 text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-violet-50 file:px-3 file:py-2 file:font-bold file:text-violet-800" /></label>
          <label className="mt-5 flex cursor-pointer gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950"><input type="checkbox" checked={acknowledged} onChange={(event) => setAcknowledged(event.target.checked)} className="mt-1 size-4 shrink-0 accent-violet-700" /><span><strong>I understand:</strong> this image workflow cannot replace tube testing, cross-matching, or qualified laboratory verification.</span></label>
          {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-sm font-semibold text-rose-800">{error}</p>}
          {success && <p role="status" className="mt-4 rounded-xl bg-emerald-50 p-3 text-sm font-semibold text-emerald-800">{success}</p>}
          <button disabled={!file || !hospitalId || !acknowledged || uploading} className="mt-5 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-violet-700 text-sm font-extrabold text-white disabled:opacity-50">{uploading ? <LoaderCircle className="size-4 animate-spin" /> : <Upload className="size-4" />}Submit for lab review</button>
          <p className="mt-3 text-center text-xs text-slate-500">No automatic result is promised or fabricated.</p>
        </form>
        <section><div className="flex items-center justify-between"><div><p className="text-xs font-extrabold uppercase tracking-wider text-violet-700">My submissions</p><h2 className="mt-1 text-2xl font-black text-ink-950">Review status</h2></div><button onClick={() => void load()} className="grid size-10 place-items-center rounded-xl border border-slate-300 text-slate-600" aria-label="Refresh"><RefreshCw className="size-4" /></button></div>
          {loading && <div className="mt-6 flex items-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-5 animate-spin" />Loading submissions…</div>}
          {!loading && items.length === 0 && <div className="mt-5 rounded-3xl border border-dashed border-slate-300 p-8 text-center"><FileImage className="mx-auto size-9 text-slate-300" /><p className="mt-3 text-sm font-bold text-slate-600">No blood-slide image submitted.</p></div>}
          <div className="mt-5 space-y-4">{items.map((item) => <article key={item.id} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-black text-ink-950">{item.originalFilename}</h3><p className="mt-1 text-xs text-slate-500">{item.hospitalName} · {dateTime(item.createdAt)}</p></div><span className={`rounded-full border px-3 py-1 text-[11px] font-extrabold ${statusStyles[item.status]}`}>{statusLabels[item.status]}</span></div>{item.rejectionReason && <p className="mt-4 rounded-xl bg-rose-50 p-3 text-xs text-rose-800"><strong>Reason:</strong> {item.rejectionReason}</p>}<ResultBoundary item={item} /></article>)}</div>
        </section>
      </div>
      <div className="mt-8 rounded-3xl bg-ink-950 p-6 text-white"><div className="flex gap-3"><ShieldCheck className="size-6 shrink-0 text-emerald-300" /><div><h2 className="font-black">Clinical safety boundary</h2><p className="mt-2 text-sm leading-6 text-slate-300">Never use this page alone for transfusion, donation eligibility, emergency decisions, or diagnosis. Use a qualified laboratory and the hospital blood bank.</p><Link to="/blood-support" className="mt-3 inline-flex text-sm font-extrabold text-emerald-300">Open verified blood support →</Link></div></div></div>
    </div>
  )
}

function StaffReview({ item, canObserve, onUpdated }: { item: BloodGroupAnalysis; canObserve: boolean; onUpdated: (item: BloodGroupAnalysis) => void }) {
  const [imageUrl, setImageUrl] = useState('')
  const [antiA, setAntiA] = useState(false)
  const [antiB, setAntiB] = useState(false)
  const [antiD, setAntiD] = useState(false)
  const [note, setNote] = useState('')
  const [rejectReason, setRejectReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let url = ''
    loadBloodGroupImage(item.id, item.contentType).then((value) => { url = value; setImageUrl(value) })
      .catch((requestError) => setError(messageFromError(requestError)))
    return () => { if (url) URL.revokeObjectURL(url) }
  }, [item.contentType, item.id])

  async function observe(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError('')
    try { onUpdated(await recordBloodGroupObservations(item.id, { antiAReactive: antiA, antiBReactive: antiB, antiDReactive: antiD, note })) }
    catch (requestError) { setError(messageFromError(requestError)) }
    finally { setBusy(false) }
  }

  async function verify() {
    if (!item.preliminaryGroup) return
    setBusy(true); setError('')
    try { onUpdated(await verifyBloodGroupAnalysis(item.id, item.preliminaryGroup)) }
    catch (requestError) { setError(messageFromError(requestError)) }
    finally { setBusy(false) }
  }

  async function reject() {
    if (!rejectReason.trim()) return
    setBusy(true); setError('')
    try { onUpdated(await rejectBloodGroupAnalysis(item.id, rejectReason.trim())) }
    catch (requestError) { setError(messageFromError(requestError)) }
    finally { setBusy(false) }
  }

  return <div className="mt-6 grid gap-6 lg:grid-cols-2"><div className="overflow-hidden rounded-3xl border border-slate-200 bg-slate-950">{imageUrl ? <img src={imageUrl} alt="Private blood-slide submission" className="aspect-square h-full w-full object-contain" /> : <div className="grid aspect-square place-items-center text-slate-400"><LoaderCircle className="size-7 animate-spin" /></div>}</div><div><p className="font-mono text-xs text-slate-500">{item.patientNumber}</p><h2 className="mt-1 text-xl font-black text-ink-950">{item.originalFilename}</h2><p className="mt-1 text-sm text-slate-500">{statusLabels[item.status]}</p><ResultBoundary item={item} />
    {item.status === 'SUBMITTED' && canObserve && <form onSubmit={observe} className="mt-5 space-y-3 rounded-2xl border border-slate-200 p-4"><h3 className="font-black text-ink-950">Record visible agglutination</h3>{([['Anti-A', antiA, setAntiA], ['Anti-B', antiB, setAntiB], ['Anti-D (Rh)', antiD, setAntiD]] as const).map(([label, value, setter]) => <label key={label} className="flex items-center justify-between gap-4 text-sm font-bold text-slate-700"><span>{label}</span><select value={String(value)} onChange={(event) => setter(event.target.value === 'true')} className="h-10 rounded-xl border border-slate-300 bg-white px-3"><option value="false">No reaction</option><option value="true">Reactive</option></select></label>)}<label className="block text-xs font-bold text-slate-600">Observation note<textarea value={note} onChange={(event) => setNote(event.target.value)} maxLength={500} className="mt-1.5 min-h-20 w-full rounded-xl border border-slate-300 p-3 text-sm" /></label><button disabled={busy} className="flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-violet-700 text-sm font-extrabold text-white disabled:opacity-50"><Microscope className="size-4" />Record observations</button></form>}
    {item.status === 'SUBMITTED' && !canObserve && <p className="mt-5 rounded-xl bg-amber-50 p-3 text-sm font-semibold text-amber-900">A laboratory technician or blood-bank staff member must record the reactions first.</p>}
    {item.status === 'OBSERVATIONS_RECORDED' && <div className="mt-5 rounded-2xl border border-emerald-200 bg-emerald-50 p-4"><p className="text-xs font-extrabold uppercase tracking-wider text-emerald-700">Maker-checker control</p><p className="mt-2 text-sm leading-6 text-emerald-950">A different authorized staff member must verify the pattern. Recorded preliminary group: <strong>{item.preliminaryGroup ? groupLabels[item.preliminaryGroup] : 'Unavailable'}</strong>.</p><button disabled={busy || !item.preliminaryGroup} onClick={() => void verify()} className="mt-3 flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-emerald-700 text-sm font-extrabold text-white disabled:opacity-50"><BadgeCheck className="size-4" />Independently verify</button></div>}
    {(item.status === 'SUBMITTED' || item.status === 'OBSERVATIONS_RECORDED') && <div className="mt-4 flex gap-2"><input value={rejectReason} onChange={(event) => setRejectReason(event.target.value)} placeholder="Reason image cannot be reviewed" className="h-11 min-w-0 flex-1 rounded-xl border border-slate-300 px-3 text-sm" /><button disabled={busy || !rejectReason.trim()} onClick={() => void reject()} className="rounded-xl border border-rose-300 px-3 text-xs font-extrabold text-rose-700 disabled:opacity-50">Reject</button></div>}
    {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-sm font-semibold text-rose-800">{error}</p>}
  </div></div>
}

function StaffView({ canObserve }: { canObserve: boolean }) {
  const [hospitals, setHospitals] = useState<Hospital[]>([])
  const [hospitalId, setHospitalId] = useState('')
  const [filter, setFilter] = useState<BloodGroupAnalysisStatus | ''>('')
  const [items, setItems] = useState<BloodGroupAnalysis[]>([])
  const [selectedId, setSelectedId] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => { api.get<Hospital[]>('/api/v1/hospitals').then(({ data }) => { setHospitals(data); setHospitalId(data[0]?.id ?? '') }).catch((requestError) => setError(messageFromError(requestError))) }, [])
  useEffect(() => {
    if (!hospitalId) return
    setLoading(true); setError('')
    getBloodGroupAnalysisWorklist(hospitalId, filter || undefined).then((data) => { setItems(data); setSelectedId((current) => data.some((item) => item.id === current) ? current : (data[0]?.id ?? '')) }).catch((requestError) => setError(messageFromError(requestError))).finally(() => setLoading(false))
  }, [filter, hospitalId])
  const selected = items.find((item) => item.id === selectedId)
  const update = (next: BloodGroupAnalysis) => setItems((all) => all.map((item) => item.id === next.id ? next : item))
  return <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8"><div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between"><div><p className="text-xs font-extrabold uppercase tracking-[.2em] text-violet-700">Authorized laboratory worklist</p><h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">Blood-slide review</h1><p className="mt-2 text-slate-600">Human observations and independent verification remain mandatory.</p></div><div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-bold text-slate-600">Hospital<select value={hospitalId} onChange={(event) => setHospitalId(event.target.value)} className="mt-1.5 h-11 min-w-52 rounded-xl border border-slate-300 bg-white px-3 text-sm">{hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}</select></label><label className="text-xs font-bold text-slate-600">Status<select value={filter} onChange={(event) => setFilter(event.target.value as BloodGroupAnalysisStatus | '')} className="mt-1.5 h-11 min-w-52 rounded-xl border border-slate-300 bg-white px-3 text-sm"><option value="">All submissions</option>{Object.entries(statusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label></div></div>{error && <p role="alert" className="mt-6 rounded-2xl bg-rose-50 p-4 text-sm font-semibold text-rose-800">{error}</p>}<div className="mt-8 grid gap-5 lg:grid-cols-[.35fr_.65fr]"><aside className="space-y-3">{loading && <p className="flex items-center gap-2 text-sm font-bold text-slate-500"><LoaderCircle className="size-4 animate-spin" />Loading worklist…</p>}{!loading && items.length === 0 && <p className="rounded-2xl border border-dashed border-slate-300 p-6 text-sm text-slate-500">No submissions match this view.</p>}{items.map((item) => <button key={item.id} onClick={() => setSelectedId(item.id)} className={`w-full rounded-2xl border p-4 text-left transition ${selectedId === item.id ? 'border-violet-400 bg-violet-50' : 'border-slate-200 bg-white hover:border-violet-200'}`}><p className="font-mono text-[11px] text-slate-500">{item.patientNumber}</p><p className="mt-1 truncate text-sm font-black text-ink-950">{item.originalFilename}</p><p className="mt-2 text-xs font-bold text-violet-700">{statusLabels[item.status]}</p></button>)}</aside><section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">{selected ? <StaffReview item={selected} canObserve={canObserve} onUpdated={update} /> : <div className="grid min-h-72 place-items-center text-center"><div><Eye className="mx-auto size-9 text-slate-300" /><p className="mt-3 text-sm font-bold text-slate-500">Select a private submission to review.</p></div></div>}</section></div></div>
}

export function BloodGroupAnalysisPage() {
  const { session } = useAuth()
  if (!session) return null
  const isStaff = session.user.roles.some((role) => staffRoles.includes(role))
  const canObserve = session.user.roles.some((role) => observationRoles.includes(role))
  return isStaff ? <StaffView canObserve={canObserve} /> : <PatientView />
}
