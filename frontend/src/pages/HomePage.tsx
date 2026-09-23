import { ArrowRight, Ambulance, CalendarDays, FileHeart, FlaskConical, MapPin, Stethoscope, TicketCheck, Droplets, Clock3, ShieldCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
import { publicAsset, isStaticDemo } from '../config/runtime'
import { api } from '../services/api'
import { IdentityAvatar } from '../components/IdentityAvatar'

type Doctor = { id: string; name: string; specialization: string; consultationFee: number; hospitalName: string }
export function HomePage() {
  const { session } = useAuth()
  const { text, language } = useLanguage()
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [status, setStatus] = useState('loading')
  useEffect(() => {
    if (isStaticDemo) { setStatus('empty'); return }
    let active = true
    api.get<{ content: Doctor[] }>('/api/v1/doctors', { params: { size: 4 } }).then(({ data }) => { if (active) { setDoctors(data.content); setStatus('ready') } }).catch(() => { if (active) setStatus('error') })
    return () => { active = false }
  }, [])
  if (session) return <Navigate to="/dashboard" replace />
  const services = [
    { title: text('Book appointment', 'अपॉइंटमेंट बुक करें'), detail: text('Choose your doctor', 'अपना डॉक्टर चुनें'), icon: CalendarDays, to: '/booking', tone: 'bg-blue-50 text-blue-600' },
    { title: text('Find a doctor', 'डॉक्टर खोजें'), detail: text('Care by specialty', 'विशेषज्ञ की देखभाल'), icon: Stethoscope, to: '/doctors', tone: 'bg-violet-50 text-violet-600' },
    { title: text('Tests & reports', 'जाँच और रिपोर्ट'), detail: text('Your results together', 'सभी नतीजे एक जगह'), icon: FlaskConical, to: '/diagnostics', tone: 'bg-cyan-50 text-cyan-600' },
    { title: text('Indoor navigation', 'अस्पताल में रास्ता'), detail: text('Find your way easily', 'आसानी से रास्ता खोजें'), icon: MapPin, to: '/navigate', tone: 'bg-amber-50 text-amber-600' },
    { title: text('Ambulance support', 'एम्बुलेंस सहायता'), detail: text('Connect with the care desk', 'केयर डेस्क से जुड़ें'), icon: Ambulance, to: '/ambulance', tone: 'bg-rose-50 text-rose-600' },
  ]
  return <div className="space-y-5">
    <div className="grid gap-4 xl:grid-cols-[1fr_245px]">
      <section className="relative min-h-64 overflow-hidden rounded-[20px] border border-white bg-blue-100">
        <img src={publicAsset('images/smartcare-clinical-team.png')} alt="SmartCare healthcare team" className="absolute inset-y-0 right-0 h-full w-1/2 object-cover object-top" />
        <div className="absolute inset-0 bg-gradient-to-r from-[#e3efff] via-[#e3efff]/95 to-transparent" />
        <div className="relative max-w-xl p-7 sm:p-9"><p className="text-sm font-medium text-blue-700">{text('Welcome to your care companion', 'आपके देखभाल साथी में स्वागत है')}</p><h1 className="mt-2 text-4xl font-bold tracking-tight text-blue-950 sm:text-5xl">SmartCare<span className="text-blue-500">.</span></h1><p className="mt-3 max-w-sm text-sm leading-6 text-slate-600">{text('Your appointments, hospital visits and next care step. All in one place.', 'आपके अपॉइंटमेंट, अस्पताल विज़िट और देखभाल का अगला कदम। सब एक जगह।')}</p><Link to="/booking" className="mt-5 inline-flex items-center gap-3 rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white">{text('Book an appointment', 'अपॉइंटमेंट बुक करें')}<ArrowRight className="size-4" /></Link></div>
      </section>
      <aside className="care-panel hidden flex-col justify-between xl:flex"><p className="flex items-center gap-2 text-xs font-semibold text-blue-800"><CalendarDays className="size-4" />{new Date().toLocaleDateString(language === 'hi' ? 'hi-IN' : 'en-IN', { weekday: 'short', day: 'numeric', month: 'long' })}</p><div><span className="text-5xl text-blue-200">“</span><p className="text-lg font-medium leading-7 text-blue-950">{text('A little clarity. A lot more care.', 'स्पष्ट जानकारी। बेहतर देखभाल।')}</p><p className="mt-4 text-right text-xs font-semibold text-blue-600">— SmartCare</p></div></aside>
    </div>
    <nav aria-label="Quick care services" className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">{services.map(({ title, detail, icon: Icon, to, tone }) => <Link key={to} to={to} className="care-panel flex items-center gap-3 transition hover:-translate-y-1 hover:shadow-md"><span className={`grid size-12 shrink-0 place-items-center rounded-2xl ${tone}`}><Icon className="size-6" /></span><span><strong className="block text-sm text-blue-950">{title}</strong><span className="mt-1 block text-xs leading-5 text-slate-500">{detail}</span></span></Link>)}</nav>
    <div className="grid gap-4 xl:grid-cols-[1.1fr_1fr_1fr]">
      <section className="care-panel"><div className="care-panel-heading"><span className="flex items-center gap-2"><Stethoscope className="size-5 text-blue-600" />{text('Meet your care team', 'अपनी देखभाल टीम से मिलें')}</span><Link to="/doctors" className="text-xs text-blue-600">{text('View all', 'सभी देखें')} →</Link></div>{doctors.map(doctor => <Link to="/doctors" key={doctor.id} className="flex items-center gap-3 border-b border-blue-50 py-3 last:border-0"><IdentityAvatar name={doctor.name} /><span className="min-w-0 flex-1"><strong className="block truncate text-sm text-blue-950">{doctor.name}</strong><span className="block text-xs text-slate-500">{doctor.specialization}</span></span><span className="text-xs font-semibold text-blue-600">₹{doctor.consultationFee.toLocaleString('en-IN')}</span></Link>)}{!doctors.length && <p className="py-8 text-sm text-slate-500">{status === 'loading' ? text('Loading care team…', 'टीम लोड हो रही है…') : text('Browse the doctor directory to find your care team.', 'अपनी टीम खोजने के लिए डॉक्टर डायरेक्टरी खोलें।')}</p>}</section>
      <section className="care-panel"><div className="care-panel-heading"><span className="flex items-center gap-2"><TicketCheck className="size-5 text-violet-600" />{text('Your next visit', 'आपकी अगली विज़िट')}</span></div><div className="rounded-xl bg-blue-50 p-5"><CalendarDays className="size-9 text-blue-400" /><h2 className="mt-4 text-lg font-semibold text-blue-950">{text('Your care, at a glance', 'आपकी देखभाल, एक नज़र में')}</h2><p className="mt-2 text-sm leading-6 text-slate-500">{text('Sign in to see your appointment, OPD number and the next action for your visit.', 'अपॉइंटमेंट, OPD नंबर और अगला कदम देखने के लिए साइन इन करें।')}</p><Link to="/dashboard" className="mt-5 flex items-center justify-between rounded-xl bg-white px-4 py-3 text-sm font-semibold text-blue-700">{text('Open My care', 'मेरी देखभाल खोलें')}<ArrowRight className="size-4" /></Link></div></section>
      <section className="care-panel"><div className="care-panel-heading"><span className="flex items-center gap-2"><MapPin className="size-5 text-cyan-600" />{text('Hospital navigation', 'अस्पताल में रास्ता')}</span></div><img src={publicAsset('images/hospital-lobby.jpg')} alt="Hospital reception and indoor corridors" className="h-36 w-full rounded-xl object-cover" /><p className="mt-4 text-sm leading-6 text-slate-500">{text('Find departments, consultation rooms and services from your hospital checkpoint.', 'अस्पताल के चेकपॉइंट से विभाग, परामर्श कक्ष और सेवाएँ खोजें।')}</p><Link to="/navigate" className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-blue-600">{text('Find my route', 'मेरा रास्ता खोजें')}<ArrowRight className="size-4" /></Link></section>
    </div>
    <div className="grid gap-4 lg:grid-cols-[1.3fr_1fr]">
      <section className="care-panel"><div className="care-panel-heading">{text('Everything for your care', 'आपकी देखभाल के लिए सब कुछ')}</div><div className="grid grid-cols-2 gap-3 sm:grid-cols-4">{[{ to: '/records', label: text('Health records', 'स्वास्थ्य रिकॉर्ड'), Icon: FileHeart }, { to: '/follow-ups', label: text('Follow-ups', 'फ़ॉलो-अप'), Icon: Clock3 }, { to: '/blood-support', label: text('Blood support', 'रक्त सहायता'), Icon: Droplets }, { to: '/assistant', label: text('Care assistant', 'देखभाल सहायक'), Icon: ShieldCheck }].map(({ to, label, Icon }) => <Link to={to} key={to} className="rounded-xl bg-blue-50/70 p-4 text-center text-xs font-semibold text-blue-900"><Icon className="mx-auto mb-3 size-7 text-blue-500" />{label}</Link>)}</div></section>
      <section className="relative overflow-hidden rounded-2xl border border-white bg-rose-50 p-6"><img src={publicAsset('images/smartcare-emergency-banner.png')} alt="Hospital ambulance" className="absolute inset-y-0 right-0 h-full w-1/2 object-cover" /><div className="absolute inset-0 bg-gradient-to-r from-rose-50 via-rose-50/90 to-transparent" /><div className="relative max-w-[65%]"><h2 className="text-lg font-semibold text-blue-950">{text('Ambulance support', 'एम्बुलेंस सहायता')}</h2><p className="mt-2 text-xs leading-5 text-slate-600">{text('Connect with your hospital transport team.', 'अपनी अस्पताल परिवहन टीम से जुड़ें।')}</p><Link to="/ambulance" className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-rose-600">{text('Open support', 'सहायता खोलें')}<ArrowRight className="size-4" /></Link></div></section>
    </div>
  </div>
}
