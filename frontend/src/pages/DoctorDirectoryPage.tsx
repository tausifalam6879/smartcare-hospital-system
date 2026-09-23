import { Building2, CalendarClock, CheckCircle2, IndianRupee, MapPin, Search, Stethoscope } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { isStaticDemo, publicAsset } from '../config/runtime'
import { IdentityAvatar } from '../components/IdentityAvatar'
import { api, messageFromError } from '../services/api'

type Hospital = { id: string; name: string; city: string }
type Department = { id: string; name: string }
type Doctor = {
  id: string
  name: string
  specialization: string
  hospitalName: string
  departmentName: string
  consultationFee: number
  building?: string
  floorLabel?: string
  roomNumber?: string
  dailyMaxCapacity: number
}
type Page<T> = { content: T[]; totalElements: number }

export function DoctorDirectoryPage() {
  const [hospitals, setHospitals] = useState<Hospital[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [hospitalId, setHospitalId] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [search, setSearch] = useState('')
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [status, setStatus] = useState<'loading' | 'ready' | 'error'>('loading')
  const [error, setError] = useState('')

  useEffect(() => {
    if (isStaticDemo) return
    api.get<Hospital[]>('/api/v1/hospitals').then(({ data }) => setHospitals(data)).catch(() => undefined)
    void loadDoctors()
  }, [])

  useEffect(() => {
    if (isStaticDemo) return
    setDepartmentId('')
    if (!hospitalId) {
      setDepartments([])
      return
    }
    api.get<Department[]>(`/api/v1/hospitals/${hospitalId}/departments`)
      .then(({ data }) => setDepartments(data))
      .catch(() => setDepartments([]))
  }, [hospitalId])

  async function loadDoctors(event?: FormEvent) {
    event?.preventDefault()
    if (isStaticDemo) return
    setStatus('loading')
    setError('')
    try {
      const response = await api.get<Page<Doctor>>('/api/v1/doctors', {
        params: { hospitalId: hospitalId || undefined, departmentId: departmentId || undefined, search: search || undefined, size: 50 },
      })
      setDoctors(response.data.content)
      setStatus('ready')
    } catch (requestError) {
      setError(messageFromError(requestError))
      setStatus('error')
    }
  }

  if (isStaticDemo) return <Navigate to="/hospitals" replace />

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <section className="relative overflow-hidden rounded-[2rem] border border-blue-100 bg-blue-50 shadow-[0_22px_55px_-40px_rgba(30,64,175,.7)]">
        <img src={publicAsset('images/smartcare-clinical-team.png')} alt="SmartCare clinical team" className="absolute inset-y-0 right-0 hidden h-full w-[48%] object-cover object-top md:block" />
        <div className="absolute inset-0 bg-gradient-to-r from-blue-50 via-blue-50/95 to-blue-50/10" />
        <div className="relative max-w-2xl px-6 py-9 sm:px-8 sm:py-12"><p className="text-xs font-extrabold uppercase tracking-[.2em] text-blue-700">Verified doctor directory</p><h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">Find the right doctor</h1><p className="mt-3 max-w-xl text-base leading-7 text-slate-600">Compare speciality, hospital, room, fee and daily OPD capacity before you book.</p><div className="mt-5 flex flex-wrap gap-2 text-xs font-bold text-blue-800"><span className="rounded-full bg-white/90 px-3 py-1.5"><CheckCircle2 className="mr-1 inline size-3.5" />Verified profiles</span><span className="rounded-full bg-white/90 px-3 py-1.5"><CalendarClock className="mr-1 inline size-3.5" />Capacity-aware booking</span></div></div>
      </section>

      <form onSubmit={loadDoctors} className="mt-6 grid gap-3 rounded-3xl border border-blue-100 bg-white p-4 shadow-sm md:grid-cols-[1fr_1fr_1.5fr_auto]">
        <label className="text-xs font-bold text-slate-600">Hospital
          <select value={hospitalId} onChange={(event) => setHospitalId(event.target.value)} className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-ink-950">
            <option value="">All hospitals</option>
            {hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}
          </select>
        </label>
        <label className="text-xs font-bold text-slate-600">Department
          <select value={departmentId} onChange={(event) => setDepartmentId(event.target.value)} disabled={!hospitalId} className="mt-1.5 h-12 w-full rounded-xl border border-slate-300 bg-white px-3 text-sm text-ink-950 disabled:bg-slate-50">
            <option value="">All departments</option>
            {departments.map((department) => <option key={department.id} value={department.id}>{department.name}</option>)}
          </select>
        </label>
        <label className="text-xs font-bold text-slate-600">Doctor or speciality
          <div className="relative mt-1.5"><Search className="absolute left-3 top-3.5 size-5 text-slate-400" /><input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="e.g. oncology" className="h-12 w-full rounded-xl border border-slate-300 pl-10 pr-3 text-sm" /></div>
        </label>
        <button type="submit" className="h-12 self-end rounded-xl bg-care-600 px-5 text-sm font-extrabold text-white hover:bg-care-700">Search</button>
      </form>

      <div className="mt-8" aria-live="polite">
        {status === 'loading' && <div className="grid gap-4 md:grid-cols-2">{[1, 2, 3, 4].map((item) => <div key={item} className="h-52 animate-pulse rounded-3xl bg-slate-200" />)}</div>}
        {status === 'error' && <div className="rounded-3xl border border-amber-200 bg-amber-50 p-6"><h2 className="font-extrabold text-amber-950">Directory is temporarily unavailable</h2><p className="mt-2 text-sm text-amber-900">{error}</p><button onClick={() => void loadDoctors()} className="mt-4 rounded-xl bg-amber-900 px-4 py-2 text-sm font-bold text-white">Try again</button></div>}
        {status === 'ready' && doctors.length === 0 && <div className="rounded-3xl border border-slate-200 bg-white p-10 text-center"><Stethoscope className="mx-auto size-10 text-slate-300" /><h2 className="mt-3 font-extrabold text-ink-950">No matching doctors yet</h2><p className="mt-1 text-sm text-slate-500">Try a broader search or another hospital.</p></div>}
        {status === 'ready' && doctors.length > 0 && (
          <><div className="mb-4 flex items-center justify-between"><p className="text-sm font-bold text-slate-500">{doctors.length} doctor{doctors.length === 1 ? '' : 's'} found</p><span className="hidden text-xs font-bold text-blue-700 sm:block">Select a profile to continue</span></div><div className="grid gap-4 xl:grid-cols-2">{doctors.map((doctor) => (
            <article key={doctor.id} className="overflow-hidden rounded-3xl border border-blue-100 bg-white shadow-[0_14px_40px_-32px_rgba(30,64,175,.7)] transition hover:-translate-y-0.5 hover:border-blue-300">
              <div className="flex items-start justify-between gap-4 bg-gradient-to-r from-blue-50 to-white p-5"><div className="flex items-center gap-4"><IdentityAvatar name={doctor.name} size="lg" /><div><div className="flex items-center gap-2"><h2 className="text-lg font-black text-ink-950">{doctor.name}</h2><CheckCircle2 className="size-4 text-blue-600" aria-label="Verified doctor" /></div><p className="text-sm font-bold text-blue-700">{doctor.specialization}</p><p className="mt-1 text-xs text-slate-500">{doctor.departmentName}</p></div></div><span className="rounded-full bg-white px-3 py-1.5 text-[10px] font-black uppercase tracking-wide text-violet-700 shadow-sm">Active</span></div>
              <div className="grid gap-4 p-5 sm:grid-cols-[1fr_auto]"><div className="space-y-2.5 text-sm text-slate-600"><p className="flex gap-2"><Building2 className="size-4 shrink-0 text-blue-500" />{doctor.hospitalName}</p><p className="flex gap-2"><MapPin className="size-4 shrink-0 text-violet-500" />{[doctor.building, doctor.floorLabel, doctor.roomNumber].filter(Boolean).join(' · ') || 'Location available from hospital desk'}</p><p className="flex gap-2"><CalendarClock className="size-4 shrink-0 text-amber-500" />Up to {doctor.dailyMaxCapacity} OPD visits daily</p></div><div className="sm:text-right"><p className="text-xs font-bold text-slate-400">Consultation fee</p><p className="mt-1 inline-flex items-center text-xl font-black text-ink-950"><IndianRupee className="size-4" />{doctor.consultationFee.toLocaleString('en-IN')}</p></div></div>
              <div className="flex items-center justify-between gap-3 border-t border-slate-100 px-5 py-4"><span className="text-xs font-semibold text-slate-500">Live capacity checked before issue</span><Link to={`/booking?doctorId=${doctor.id}`} className="rounded-xl bg-blue-700 px-4 py-2.5 text-xs font-extrabold text-white hover:bg-blue-800">View slots &amp; book →</Link></div>
            </article>
          ))}</div></>
        )}
      </div>
    </div>
  )
}
