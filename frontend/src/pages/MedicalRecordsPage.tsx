import {
  AlertCircle, CalendarDays, CheckCircle2, ClipboardPlus, Download, FileImage, FlaskConical,
  FileText, HeartPulse, Hospital, LoaderCircle, LockKeyhole, Pill, Plus, ShieldCheck,
  Stethoscope, Upload, X,
} from 'lucide-react'
import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, messageFromError } from '../services/api'
import { getMyDiagnosticOrders, type DiagnosticOrder } from '../services/diagnostics'
import {
  downloadMedicalDocument, getMyMedicalRecord, uploadMedicalDocument,
  type AllergySeverity, type DocumentType, type MedicalDocument, type MedicalRecord,
} from '../services/medicalRecords'
import { localDateString } from '../utils/appointmentLifecycle'

type HospitalSummary = { id: string; name: string }

const documentLabels: Record<DocumentType, string> = {
  LAB_REPORT: 'Lab report', MRI_REPORT: 'MRI report', CT_REPORT: 'CT report',
  X_RAY_REPORT: 'X-ray report', PRESCRIPTION: 'Prescription',
  DISCHARGE_SUMMARY: 'Discharge summary', REFERRAL: 'Referral', OTHER: 'Other document',
}

const assistantReadiness = {
  READY_FOR_TEXT_CHECK: { label: 'Text checked when asked', style: 'bg-blue-50 text-blue-800' },
  INDEXED: { label: 'Readable by assistant', style: 'bg-emerald-50 text-emerald-800' },
  OCR_REQUIRED: { label: 'OCR required', style: 'bg-amber-50 text-amber-800' },
  NO_TEXT: { label: 'No extractable text', style: 'bg-amber-50 text-amber-800' },
  FAILED: { label: 'Text extraction failed', style: 'bg-rose-50 text-rose-800' },
} as const

const severityStyle: Record<AllergySeverity, string> = {
  LOW: 'border-slate-200 bg-slate-50 text-slate-700',
  MODERATE: 'border-amber-200 bg-amber-50 text-amber-900',
  HIGH: 'border-orange-200 bg-orange-50 text-orange-900',
  CRITICAL: 'border-rose-300 bg-rose-50 text-rose-900',
}

function displayDate(value: string) {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date(`${value}T12:00:00`))
}

function fileSize(bytes: number) {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export function MedicalRecordsPage() {
  const [record, setRecord] = useState<MedicalRecord | null>(null)
  const [hospitals, setHospitals] = useState<HospitalSummary[]>([])
  const [diagnosticOrders, setDiagnosticOrders] = useState<DiagnosticOrder[]>([])
  const [loading, setLoading] = useState(true)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [downloadingId, setDownloadingId] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [hospitalId, setHospitalId] = useState('')
  const [documentType, setDocumentType] = useState<DocumentType>('LAB_REPORT')
  const [documentDate, setDocumentDate] = useState(localDateString())
  const [description, setDescription] = useState('')
  const [file, setFile] = useState<File | null>(null)

  async function load() {
    setLoading(true)
    setError('')
    try {
      const [recordData, hospitalResponse, orderData] = await Promise.all([
        getMyMedicalRecord(), api.get<HospitalSummary[]>('/api/v1/hospitals'), getMyDiagnosticOrders(),
      ])
      setRecord(recordData)
      setDiagnosticOrders(orderData)
      setHospitals(hospitalResponse.data)
      setHospitalId((current) => current || hospitalResponse.data[0]?.id || '')
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void load() }, [])

  const activeAllergies = useMemo(() => record?.allergies.filter((item) => item.status === 'ACTIVE') ?? [], [record])
  const verifiedDiagnostics = useMemo(() => diagnosticOrders.filter((item) => item.status === 'RESULT_VERIFIED' && item.result), [diagnosticOrders])

  async function upload(event: FormEvent) {
    event.preventDefault()
    if (!file || !hospitalId) return
    setUploading(true)
    setError('')
    setSuccess('')
    try {
      await uploadMedicalDocument({ hospitalId, documentType, documentDate, description, file })
      setSuccess('Document added securely to your health record.')
      setUploadOpen(false)
      setFile(null)
      setDescription('')
      await load()
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setUploading(false)
    }
  }

  async function download(document: MedicalDocument) {
    setDownloadingId(document.id)
    setError('')
    try {
      await downloadMedicalDocument(document)
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setDownloadingId('')
    }
  }

  if (loading && !record) return <div className="grid min-h-[65vh] place-items-center bg-slate-50"><div className="flex items-center gap-3 text-sm font-bold text-slate-500"><LoaderCircle className="size-6 animate-spin text-care-600" />Opening your private health record…</div></div>

  return (
    <div className="min-h-screen bg-[#f5f8fc]">
      <section className="overflow-hidden bg-[linear-gradient(120deg,#071b35_0%,#0b315d_62%,#12549c_100%)] text-white">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 lg:grid-cols-[1fr_.65fr] lg:px-8 lg:py-14">
          <div><div className="inline-flex items-center gap-2 rounded-full border border-emerald-300/20 bg-emerald-300/10 px-3 py-2 text-xs font-black uppercase tracking-[.18em] text-emerald-200"><LockKeyhole className="size-4" />Private patient record</div><h1 className="mt-5 text-4xl font-black tracking-tight sm:text-5xl">Your health story, kept together</h1><p className="mt-4 max-w-2xl text-sm leading-7 text-blue-100/75 sm:text-base">Doctor-finalized visits, medicines, allergy warnings and your uploaded reports—available only after authorization.</p><div className="mt-4 flex flex-wrap items-center gap-3"><p className="font-mono text-xs font-bold text-care-200">Patient no. {record?.patientNumber}</p><Link to="/assistant" className="inline-flex items-center gap-2 rounded-xl border border-white/15 bg-white/10 px-3 py-2 text-xs font-black text-white hover:bg-white/15">Ask this record →</Link><Link to="/diagnostics" className="inline-flex items-center gap-2 rounded-xl border border-white/15 bg-white/10 px-3 py-2 text-xs font-black text-white hover:bg-white/15">Track tests & results →</Link></div></div>
          <div className="grid grid-cols-2 gap-3 self-end sm:grid-cols-4 lg:grid-cols-2">{[[record?.visits.length ?? 0, 'Visits'], [activeAllergies.length, 'Allergies'], [verifiedDiagnostics.length, 'Verified results'], [record?.documents.length ?? 0, 'Files']].map(([count, label]) => <div key={label} className="rounded-2xl border border-white/10 bg-white/8 p-4 text-center backdrop-blur"><p className="text-3xl font-black">{count}</p><p className="mt-1 text-[11px] font-bold text-blue-100/60">{label}</p></div>)}</div>
        </div>
      </section>

      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        {error && <div role="alert" className="mb-5 flex gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-800"><AlertCircle className="size-5 shrink-0" />{error}</div>}
        {success && <div className="mb-5 flex gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-semibold text-emerald-800"><CheckCircle2 className="size-5 shrink-0" />{success}</div>}

        <section className="grid gap-5 lg:grid-cols-[.75fr_1.25fr]">
          <div className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
            <div className="flex items-center gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-rose-50 text-rose-700"><HeartPulse className="size-6" /></span><div><p className="text-xs font-black uppercase tracking-wider text-rose-700">Safety first</p><h2 className="text-xl font-black text-ink-950">Active allergies</h2></div></div>
            {activeAllergies.length === 0 ? <div className="mt-5 rounded-2xl border border-dashed border-slate-300 p-5 text-sm text-slate-500"><strong className="block text-slate-700">No clinician-recorded allergy</strong>This does not prove that you have no allergies. Tell your care team about known reactions.</div> : <div className="mt-5 space-y-3">{activeAllergies.map((allergy) => <div key={allergy.id} className={`rounded-2xl border p-4 ${severityStyle[allergy.severity]}`}><div className="flex items-center justify-between gap-3"><strong>{allergy.substance}</strong><span className="rounded-full bg-white/70 px-2 py-1 text-[10px] font-black">{allergy.severity}</span></div>{allergy.reaction && <p className="mt-1 text-sm">Reaction: {allergy.reaction}</p>}<p className="mt-2 text-[11px] opacity-70">Recorded by {allergy.recordedByDoctor}</p></div>)}</div>}
          </div>

          <div className="rounded-[2rem] border border-care-200 bg-care-50 p-6 sm:p-7">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-center gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-care-600 text-white"><Upload className="size-6" /></span><div><p className="text-xs font-black uppercase tracking-wider text-care-700">Your reports</p><h2 className="text-xl font-black text-ink-950">Add a private document</h2></div></div><button onClick={() => setUploadOpen((value) => !value)} className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-care-600 px-5 text-sm font-black text-white shadow-md shadow-blue-700/15">{uploadOpen ? <X className="size-5" /> : <Plus className="size-5" />}{uploadOpen ? 'Close' : 'Upload report'}</button></div>
            <div className="mt-5 flex gap-3 rounded-2xl bg-white/70 p-4 text-xs leading-5 text-care-900"><ShieldCheck className="size-5 shrink-0 text-emerald-600" /><p>PDF, JPG or PNG only, maximum 10 MB. Files are signature-checked, hashed and served through an authorized endpoint—never a public URL.</p></div>
          </div>
        </section>

        {uploadOpen && <form onSubmit={upload} className="mt-5 grid gap-4 rounded-[2rem] border border-care-200 bg-white p-6 shadow-soft md:grid-cols-2 lg:grid-cols-4">
          <label className="text-xs font-black text-slate-600">Hospital<select required value={hospitalId} onChange={(event) => setHospitalId(event.target.value)} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm"><option value="">Choose hospital</option>{hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}</select></label>
          <label className="text-xs font-black text-slate-600">Document type<select value={documentType} onChange={(event) => setDocumentType(event.target.value as DocumentType)} className="mt-2 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm">{Object.entries(documentLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
          <label className="text-xs font-black text-slate-600">Report date<input required type="date" max={localDateString()} value={documentDate} onChange={(event) => setDocumentDate(event.target.value)} className="mt-2 h-12 w-full rounded-xl border border-slate-300 px-3 text-sm" /></label>
          <label className="text-xs font-black text-slate-600">Choose file<input required type="file" accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" onChange={(event) => setFile(event.target.files?.[0] ?? null)} className="mt-2 block h-12 w-full rounded-xl border border-slate-300 bg-white p-2 text-xs file:mr-2 file:rounded-lg file:border-0 file:bg-care-50 file:px-3 file:py-1.5 file:font-bold file:text-care-800" /></label>
          <label className="text-xs font-black text-slate-600 md:col-span-2 lg:col-span-3">Description (optional)<input value={description} maxLength={600} onChange={(event) => setDescription(event.target.value)} placeholder="e.g. CBC report from follow-up visit" className="mt-2 h-12 w-full rounded-xl border border-slate-300 px-3 text-sm" /></label>
          <button disabled={!file || !hospitalId || uploading} className="inline-flex h-12 self-end items-center justify-center gap-2 rounded-xl bg-emerald-600 px-5 text-sm font-black text-white disabled:bg-slate-300">{uploading ? <LoaderCircle className="size-5 animate-spin" /> : <Upload className="size-5" />}Save securely</button>
        </form>}

        <section className="mt-9">
          <div><p className="text-xs font-black uppercase tracking-[.18em] text-care-700">Longitudinal record</p><h2 className="mt-1 text-3xl font-black text-ink-950">Consultation timeline</h2></div>
          {record?.visits.length === 0 ? <div className="mt-5 rounded-[2rem] border border-dashed border-slate-300 bg-white p-10 text-center"><ClipboardPlus className="mx-auto size-10 text-slate-300" /><h3 className="mt-4 font-black text-ink-950">No finalized consultation notes yet</h3><p className="mx-auto mt-2 max-w-xl text-sm leading-6 text-slate-500">Notes appear only after your linked doctor finalizes them for a real appointment. SmartCare does not generate diagnoses.</p><Link to="/dashboard" className="mt-5 inline-flex text-sm font-black text-care-700 hover:underline">Open My care →</Link></div> : <div className="mt-5 space-y-5">{record?.visits.map((visit) => <article key={visit.id} className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-sm"><div className="flex flex-col gap-4 border-b border-slate-100 bg-slate-50 p-5 sm:flex-row sm:items-center sm:justify-between sm:p-6"><div className="flex items-center gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-care-600 text-white"><Stethoscope className="size-6" /></span><div><h3 className="font-black text-ink-950">{visit.doctorName}</h3><p className="text-sm font-semibold text-care-700">{visit.specialization} · {visit.hospitalName}</p></div></div><span className="inline-flex items-center gap-2 text-xs font-bold text-slate-500"><CalendarDays className="size-4" />{displayDate(visit.visitDate)}</span></div><div className="grid gap-6 p-5 sm:p-6 lg:grid-cols-2"><div><p className="text-[11px] font-black uppercase tracking-wider text-slate-400">Doctor-finalized diagnosis</p><p className="mt-2 font-bold leading-6 text-ink-950">{visit.diagnosis}</p>{visit.symptoms && <><p className="mt-5 text-[11px] font-black uppercase tracking-wider text-slate-400">Documented symptoms</p><p className="mt-2 text-sm leading-6 text-slate-600">{visit.symptoms}</p></>}{visit.doctorNotes && <><p className="mt-5 text-[11px] font-black uppercase tracking-wider text-slate-400">Clinical notes</p><p className="mt-2 text-sm leading-6 text-slate-600">{visit.doctorNotes}</p></>}</div><div>{visit.prescription ? <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4"><div className="flex items-center gap-2"><Pill className="size-5 text-emerald-700" /><h4 className="font-black text-emerald-950">Prescription</h4></div><div className="mt-4 space-y-3">{visit.prescription.medicines.map((medicine, index) => <div key={`${medicine.medicineName}-${index}`} className="rounded-xl bg-white p-3"><p className="font-black text-ink-950">{medicine.medicineName} · {medicine.dosage}</p><p className="mt-1 text-xs text-slate-600">{medicine.frequency} · {medicine.duration}{medicine.route ? ` · ${medicine.route}` : ''}</p>{medicine.instructions && <p className="mt-1 text-xs font-semibold text-emerald-800">{medicine.instructions}</p>}</div>)}</div>{visit.prescription.generalInstructions && <p className="mt-3 text-xs font-semibold text-emerald-900">Note: {visit.prescription.generalInstructions}</p>}</div> : <div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-500">No structured prescription attached.</div>}{visit.followUpRecommendation && <div className="mt-4 rounded-2xl border border-violet-200 bg-violet-50 p-4"><p className="text-xs font-black uppercase tracking-wider text-violet-700">Follow-up</p><p className="mt-2 text-sm font-semibold leading-6 text-violet-950">{visit.followUpRecommendation}</p></div>}</div></div></article>)}</div>}
        </section>

        <section className="mt-10">
          <div className="flex items-end justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-[.18em] text-cyan-700">Authorized hospital results</p><h2 className="mt-1 text-3xl font-black text-ink-950">Verified diagnostic results</h2></div><Link to="/diagnostics" className="text-sm font-black text-care-700 hover:underline">Track all tests →</Link></div>
          {verifiedDiagnostics.length === 0 ? <div className="mt-5 rounded-[2rem] border border-dashed border-slate-300 bg-white p-9 text-center"><FlaskConical className="mx-auto size-9 text-slate-300" /><h3 className="mt-3 font-black text-ink-950">No verified result released yet</h3><p className="mt-2 text-sm text-slate-500">Only results verified by authorized diagnostic staff appear in this health record.</p></div> : <div className="mt-5 grid gap-4 lg:grid-cols-2">{verifiedDiagnostics.map((order) => <article key={order.id} className="rounded-3xl border border-cyan-200 bg-white p-5 shadow-sm sm:p-6"><div className="flex items-start justify-between gap-3"><div className="flex gap-3"><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-cyan-50 text-cyan-700"><FlaskConical className="size-5" /></span><div><h3 className="font-black text-ink-950">{order.procedureName}</h3><p className="text-xs font-bold text-care-700">{order.procedureCode} · {order.hospitalName}</p></div></div><span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${order.result?.overallFlag === 'CRITICAL' ? 'bg-rose-100 text-rose-800' : order.result?.overallFlag === 'ABNORMAL' ? 'bg-amber-100 text-amber-900' : 'bg-emerald-100 text-emerald-800'}`}>{order.result?.overallFlag}</span></div><p className="mt-4 text-sm font-semibold leading-6 text-slate-700">{order.result?.summary}</p>{order.result?.items.length ? <div className="mt-4 space-y-2">{order.result.items.map((item, index) => <div key={`${item.name}-${index}`} className="flex items-start justify-between gap-4 rounded-xl bg-slate-50 p-3 text-xs"><div><p className="font-black text-ink-950">{item.name}</p>{item.referenceRange && <p className="mt-1 text-slate-500">Reference: {item.referenceRange}</p>}</div><p className="text-right font-black text-slate-700">{item.value}{item.unit ? ` ${item.unit}` : ''}</p></div>)}</div> : null}<div className="mt-4 border-t border-slate-100 pt-3 text-[11px] text-slate-500"><p>Verified by {order.result?.verifiedBy}</p><p>{order.resultVerifiedAt ? new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(order.resultVerifiedAt)) : ''}</p></div></article>)}</div>}
        </section>

        <section className="mt-10">
          <div className="flex items-end justify-between gap-4"><div><p className="text-xs font-black uppercase tracking-[.18em] text-care-700">Private files</p><h2 className="mt-1 text-3xl font-black text-ink-950">Reports & documents</h2></div><button onClick={() => setUploadOpen(true)} className="hidden items-center gap-2 text-sm font-black text-care-700 hover:underline sm:flex"><Plus className="size-4" />Add report</button></div>
          {record?.documents.length === 0 ? <div className="mt-5 rounded-[2rem] border border-dashed border-slate-300 bg-white p-9 text-center text-sm text-slate-500">No private reports uploaded yet.</div> : <div className="mt-5 grid gap-4 md:grid-cols-2 lg:grid-cols-3">{record?.documents.map((document) => { const readiness = assistantReadiness[document.assistantReadiness]; return <article key={document.id} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm"><div className="flex items-start justify-between gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-care-50 text-care-700">{document.contentType === 'application/pdf' ? <FileText className="size-6" /> : <FileImage className="size-6" />}</span><span className="rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-black text-slate-600">{document.verificationStatus === 'CLINICIAN_VERIFIED' ? 'Clinician verified' : 'Patient uploaded'}</span></div><h3 className="mt-4 truncate font-black text-ink-950" title={document.originalFilename}>{document.originalFilename}</h3><p className="mt-1 text-xs font-bold text-care-700">{documentLabels[document.documentType]}</p><p className={`mt-3 inline-flex rounded-full px-2.5 py-1 text-[10px] font-black ${readiness.style}`}>Assistant: {readiness.label}</p><div className="mt-4 space-y-1 text-xs text-slate-500"><p className="flex items-center gap-2"><Hospital className="size-3.5" />{document.hospitalName}</p><p>{displayDate(document.documentDate)} · {fileSize(document.sizeBytes)}</p></div>{document.description && <p className="mt-3 line-clamp-2 text-sm leading-5 text-slate-600">{document.description}</p>}<button onClick={() => void download(document)} disabled={downloadingId === document.id} className="mt-5 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-care-200 bg-care-50 text-sm font-black text-care-800 disabled:opacity-60">{downloadingId === document.id ? <LoaderCircle className="size-4 animate-spin" /> : <Download className="size-4" />}Authorized download</button></article> })}</div>}
        </section>

        <div className="mt-10 flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm leading-6 text-amber-950"><AlertCircle className="mt-0.5 size-5 shrink-0" /><p><strong>Important:</strong> SmartCare stores and displays clinician-entered records; it does not diagnose, prescribe or alter your doctor’s advice. Contact qualified medical staff for medical decisions.</p></div>
      </div>
    </div>
  )
}
