import { Building2, IndianRupee, MapPin, Search, Stethoscope } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { isStaticDemo } from '../config/runtime'
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
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="max-w-2xl">
        <p className="text-xs font-extrabold uppercase tracking-[.2em] text-care-700">Verified directory</p>
        <h1 className="mt-2 text-4xl font-black tracking-tight text-ink-950">Find the right doctor</h1>
        <p className="mt-3 text-base leading-7 text-slate-600">Search active doctors and see their hospital location, schedule capacity, and consultation fee.</p>
      </div>

      <form onSubmit={loadDoctors} className="mt-8 grid gap-3 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-[1fr_1fr_1.5fr_auto]">
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
          <><p className="mb-4 text-sm font-bold text-slate-500">{doctors.length} doctor{doctors.length === 1 ? '' : 's'} found</p><div className="grid gap-4 md:grid-cols-2">{doctors.map((doctor) => (
            <article key={doctor.id} className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-start gap-4"><span className="grid size-14 shrink-0 place-items-center rounded-2xl bg-care-50 text-care-700"><Stethoscope className="size-7" /></span><div><h2 className="text-lg font-black text-ink-950">{doctor.name}</h2><p className="text-sm font-bold text-care-700">{doctor.specialization}</p></div></div>
              <div className="mt-5 space-y-2.5 text-sm text-slate-600"><p className="flex gap-2"><Building2 className="size-4 shrink-0 text-slate-400" />{doctor.hospitalName} · {doctor.departmentName}</p><p className="flex gap-2"><MapPin className="size-4 shrink-0 text-slate-400" />{[doctor.building, doctor.floorLabel, doctor.roomNumber].filter(Boolean).join(' · ') || 'Location available from hospital desk'}</p><p className="flex gap-2"><IndianRupee className="size-4 shrink-0 text-slate-400" />Consultation fee ₹{doctor.consultationFee.toLocaleString('en-IN')}</p></div>
              <div className="mt-5 flex items-center justify-between gap-3 border-t border-slate-100 pt-4"><span className="text-xs font-bold text-slate-500">Daily capacity: {doctor.dailyMaxCapacity}</span><Link to={`/booking?doctorId=${doctor.id}`} className="rounded-xl bg-care-600 px-3 py-2 text-xs font-extrabold text-white hover:bg-care-700">Book OPD number</Link></div>
            </article>
          ))}</div></>
        )}
      </div>
    </div>
  )
}
