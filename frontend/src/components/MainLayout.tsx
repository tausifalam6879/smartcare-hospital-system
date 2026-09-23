import { Ambulance, Bell, Building2, CalendarDays, ClipboardList, Droplets, FileHeart, FlaskConical, Home, Languages, LogIn, LogOut, Menu, Navigation, Search, Stethoscope, TicketCheck, X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
import { isStaticDemo, publicAsset } from '../config/runtime'
import { getUnreadCount } from '../services/notifications'
import { BrandMark } from './BrandMark'
import { IdentityAvatar } from './IdentityAvatar'

const patientLinks = [
  { to: '/', label: 'Home', hi: 'होम', icon: Home },
  { to: '/dashboard', label: 'My care', hi: 'मेरी देखभाल', icon: CalendarDays },
  { to: '/booking', label: 'Book appointment', hi: 'अपॉइंटमेंट बुक करें', icon: TicketCheck },
  { to: '/doctors', label: 'Find a doctor', hi: 'डॉक्टर खोजें', icon: Stethoscope },
  { to: '/hospitals', label: 'Hospitals', hi: 'अस्पताल', icon: Building2 },
  { to: '/navigate', label: 'Indoor navigation', hi: 'अस्पताल में रास्ता', icon: Navigation },
  { to: '/diagnostics', label: 'Tests & reports', hi: 'जाँच और रिपोर्ट', icon: FlaskConical },
  { to: '/records', label: 'Health records', hi: 'स्वास्थ्य रिकॉर्ड', icon: FileHeart },
  { to: '/blood-support', label: 'Blood support', hi: 'रक्त सहायता', icon: Droplets },
  { to: '/ambulance', label: 'Ambulance support', hi: 'एम्बुलेंस सहायता', icon: Ambulance },
  { to: '/follow-ups', label: 'Follow-ups', hi: 'फ़ॉलो-अप', icon: CalendarDays },
  { to: '/assistant', label: 'Care assistant', hi: 'देखभाल सहायक', icon: FileHeart },
  { to: '/blood-group-analysis', label: 'Blood-slide review', hi: 'ब्लड-स्लाइड जाँच', icon: FlaskConical },
  { to: '/operations', label: 'Booking changes', hi: 'बुकिंग बदलाव', icon: ClipboardList },
]
export function MainLayout() {
  const { session, logout } = useAuth()
  const { language, setLanguage, text } = useLanguage()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [drawer, setDrawer] = useState(false)
  const [search, setSearch] = useState('')
  const [unread, setUnread] = useState(0)
  const roles = session?.user.roles ?? []
  const doctor = roles.includes('DOCTOR')
  const staff = roles.some(role => ['RECEPTIONIST', 'CASHIER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN', 'AMBULANCE_DISPATCHER', 'LAB_TECHNICIAN', 'BLOOD_BANK_STAFF'].includes(role))
  const patient = Boolean(session && !doctor && !staff)
  const admin = roles.some(role => ['HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role))
  const desk = roles.some(role => ['RECEPTIONIST', 'CASHIER'].includes(role))
  const workspace = doctor ? '/doctor/consultations' : admin || desk ? '/operations' : staff ? '/staff/tasks' : '/dashboard'
  const links = doctor ? [
    { to: '/doctor/consultations', label: 'Consultations', hi: 'परामर्श', icon: Stethoscope },
    { to: '/operations', label: 'Day operations', hi: 'आज का कार्य', icon: ClipboardList }, patientLinks[5],
  ] : staff ? [
    ...(admin || desk ? [{ to: '/operations', label: 'Operations overview', hi: 'संचालन', icon: Home }] : []),
    ...(!desk ? [{ to: '/staff/tasks', label: 'My tasks', hi: 'मेरे कार्य', icon: ClipboardList }] : []),
    ...(admin || roles.includes('AMBULANCE_DISPATCHER') ? [patientLinks[9]] : []),
    ...(roles.some(role => ['LAB_TECHNICIAN', 'BLOOD_BANK_STAFF'].includes(role)) ? [{ to: '/blood-group-analysis', label: 'Slide review', hi: 'स्लाइड जाँच', icon: FlaskConical }] : []), patientLinks[5],
  ] : patientLinks
  const results = search.trim() ? links.filter(link => `${link.label} ${link.hi}`.toLowerCase().includes(search.toLowerCase())) : []
  useEffect(() => { window.scrollTo(0, 0); setDrawer(false); setSearch('') }, [pathname])
  useEffect(() => {
    const close = (event: KeyboardEvent) => { if (event.key === 'Escape') { setDrawer(false); setSearch('') } }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [])
  useEffect(() => {
    if (!patient || isStaticDemo) { setUnread(0); return }
    let active = true
    const refresh = () => getUnreadCount().then(count => { if (active) setUnread(count) }).catch(() => undefined)
    void refresh()
    const timer = window.setInterval(refresh, 30000)
    return () => { active = false; window.clearInterval(timer) }
  }, [patient, pathname])
  const roleLabel = doctor ? text('Doctor workspace', 'डॉक्टर कार्यक्षेत्र') : staff ? text('Hospital team', 'अस्पताल टीम') : text('Your care companion', 'आपकी देखभाल का साथी')
  return <div className="care-shell">
    {drawer && <button className="fixed inset-0 z-40 bg-slate-950/30 backdrop-blur-sm lg:hidden" aria-label="Close navigation" onClick={() => setDrawer(false)} />}
    <aside className={`care-sidebar ${drawer ? 'is-open' : ''}`}>
      <div className="flex items-center justify-between px-3 pb-7 pt-3"><NavLink to={session ? workspace : '/'}><BrandMark /></NavLink><button onClick={() => setDrawer(false)} aria-label="Close menu" className="lg:hidden"><X className="size-5" /></button></div>
      <p className="mb-3 px-4 text-[10px] font-bold uppercase tracking-[.2em] text-slate-400">{text('Your workspace', 'आपका कार्यक्षेत्र')}</p>
      <nav aria-label="Workspace navigation" className="grid gap-1">{links.map(({ to, label, hi, icon: Icon }) => <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => `care-nav-link ${isActive ? 'is-active' : ''}`}><Icon className="size-5 shrink-0" /><span>{language === 'hi' ? hi : label}</span></NavLink>)}</nav>
      <div className="mt-auto pt-7"><div className="overflow-hidden rounded-2xl bg-gradient-to-b from-blue-50 to-indigo-50 text-center"><img src={publicAsset('images/smartcare-clinical-team.png')} alt="SmartCare care team" className="h-32 w-full object-cover object-top" /><div className="px-3 py-4"><p className="text-sm font-bold text-blue-950">{text('Together for better care', 'बेहतर देखभाल के लिए साथ')}</p><p className="mt-1 text-xs text-blue-600">Care · Connect · Heal</p></div></div></div>
    </aside>
    <div className="care-content">
      <header className="care-topbar">
        <button aria-label="Open navigation" onClick={() => setDrawer(true)} className="grid size-10 shrink-0 place-items-center rounded-xl text-blue-800 hover:bg-white lg:hidden"><Menu className="size-5" /></button>
        <div className="relative min-w-0 flex-1 max-w-xl"><label className="flex h-11 items-center gap-3 rounded-xl border border-blue-100 bg-white/80 px-4"><Search className="size-5 shrink-0 text-blue-500" /><input value={search} onChange={event => setSearch(event.target.value)} aria-label="Search services" placeholder={text('Search appointments, doctors, services…', 'अपॉइंटमेंट, डॉक्टर, सेवाएँ खोजें…')} className="min-w-0 w-full bg-transparent text-sm outline-none" /></label>{search && <div className="absolute inset-x-0 top-13 z-50 rounded-xl border border-blue-100 bg-white p-2 shadow-xl">{results.length ? results.map(link => <NavLink className="block rounded-lg px-3 py-3 text-sm hover:bg-blue-50" key={link.to} to={link.to}>{language === 'hi' ? link.hi : link.label}</NavLink>) : <p className="px-3 py-3 text-sm text-slate-500">{text('No matching services', 'कोई सेवा नहीं मिली')}</p>}</div>}</div>
        <button onClick={() => setLanguage(language === 'en' ? 'hi' : 'en')} aria-label="Switch language" className="flex items-center gap-1 rounded-xl px-2 py-2 text-xs font-bold text-blue-800"><Languages className="size-4" />{language === 'en' ? 'हिं' : 'EN'}</button>
        {patient && <NavLink to="/notifications" aria-label={`${unread} unread notifications`} className="relative p-2 text-blue-800"><Bell className="size-5" />{unread > 0 && <span className="absolute right-0 top-0 size-2 rounded-full bg-rose-500" />}</NavLink>}
        {session ? <><NavLink to={workspace} className="flex items-center gap-3"><IdentityAvatar name={session.user.displayName} size="sm" /><span className="hidden xl:block"><strong className="block text-sm text-blue-950">{session.user.displayName}</strong><span className="text-xs text-slate-500">{roleLabel}</span></span></NavLink><button aria-label="Sign out" onClick={() => { logout(); navigate('/login', { replace: true }) }} className="p-2 text-slate-500 hover:text-rose-600"><LogOut className="size-4" /></button></> : <NavLink to="/login" className="flex shrink-0 items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-sm font-semibold text-white"><LogIn className="size-4" /><span className="hidden sm:inline">{text('Sign in', 'साइन इन')}</span></NavLink>}
      </header>
      {isStaticDemo && <p className="mx-5 rounded-xl bg-amber-50 p-3 text-xs text-amber-900">Public portfolio prototype · Sample data</p>}
      <main className="care-page"><Outlet /></main>
    </div>
  </div>
}
