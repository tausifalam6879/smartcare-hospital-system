import { Ambulance, Bell, Building2, CalendarDays, ClipboardList, Clock3, Droplets, FileHeart, FlaskConical, Home, Languages, LayoutDashboard, LogIn, LogOut, MapPin, Microscope, Navigation, Phone, Stethoscope, TicketCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
import { isStaticDemo, publicAsset } from '../config/runtime'
import { getUnreadCount } from '../services/notifications'
import { BrandMark } from './BrandMark'

const patientDesktopLinks = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/hospitals', label: 'Hospitals', icon: Building2 },
  { to: '/booking', label: 'Book OPD', icon: TicketCheck },
  { to: '/navigate', label: 'Navigate', icon: Navigation },
  { to: '/diagnostics', label: 'Tests', icon: FlaskConical },
  { to: '/records', label: 'Records', icon: FileHeart },
  { to: '/blood-support', label: 'Blood', icon: Droplets },
  { to: '/dashboard', label: 'My care', icon: CalendarDays },
]

const patientMobileLinks = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/booking', label: 'Book OPD', icon: TicketCheck },
  { to: '/navigate', label: 'Navigate', icon: Navigation },
  { to: '/records', label: 'Records', icon: FileHeart },
  { to: '/dashboard', label: 'My care', icon: CalendarDays },
]

const patientFooterLinks = [
  ['/booking', 'Book OPD number'], ['/hospitals', 'India hospital directory'], ['/doctors', 'Find a doctor'],
  ['/ambulance', 'Ambulance coordination'], ['/navigate', 'Hospital navigation'], ['/diagnostics', 'Diagnostics & results'],
  ['/blood-support', 'Blood support'], ['/blood-group-analysis', 'Blood-slide review'], ['/records', 'My health record'],
  ['/follow-ups', 'Follow-ups & reminders'], ['/assistant', 'Care assistant'], ['/operations', 'Appointment recovery'], ['/dashboard', 'My care'],
]

const doctorLinks = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/doctor/consultations', label: 'Consultations', icon: Stethoscope },
  { to: '/operations', label: 'Day operations', icon: LayoutDashboard },
  { to: '/navigate', label: 'Navigate', icon: Navigation },
]

const hindiLabels: Record<string, string> = {
  Home: 'होम', Hospitals: 'अस्पताल', 'Book OPD': 'OPD बुक करें', Navigate: 'रास्ता', Tests: 'जाँच',
  Records: 'रिकॉर्ड', Blood: 'रक्त', 'My care': 'मेरी देखभाल', Consultations: 'परामर्श',
  'Day operations': 'आज का कार्य', Operations: 'संचालन', 'My tasks': 'मेरे कार्य', Dispatch: 'डिस्पैच',
  'Slide review': 'स्लाइड जाँच', Ambulance: 'एम्बुलेंस',
}

const staffLinksByRole: Record<string, typeof patientDesktopLinks> = {
  RECEPTIONIST: [
    { to: '/', label: 'Home', icon: Home },
    { to: '/operations', label: 'Operations', icon: LayoutDashboard },
    { to: '/navigate', label: 'Navigate', icon: Navigation },
  ],
  CASHIER: [
    { to: '/', label: 'Home', icon: Home },
    { to: '/operations', label: 'Operations', icon: LayoutDashboard },
    { to: '/navigate', label: 'Navigate', icon: Navigation },
  ],
  AMBULANCE_DISPATCHER: [
    { to: '/', label: 'Home', icon: Home },
    { to: '/staff/tasks', label: 'My tasks', icon: ClipboardList },
    { to: '/ambulance', label: 'Dispatch', icon: Ambulance },
    { to: '/navigate', label: 'Navigate', icon: Navigation },
  ],
  LAB_TECHNICIAN: [
    { to: '/', label: 'Home', icon: Home },
    { to: '/staff/tasks', label: 'My tasks', icon: ClipboardList },
    { to: '/blood-group-analysis', label: 'Slide review', icon: Microscope },
    { to: '/navigate', label: 'Navigate', icon: Navigation },
  ],
  BLOOD_BANK_STAFF: [
    { to: '/', label: 'Home', icon: Home },
    { to: '/staff/tasks', label: 'My tasks', icon: ClipboardList },
    { to: '/blood-group-analysis', label: 'Slide review', icon: Microscope },
    { to: '/navigate', label: 'Navigate', icon: Navigation },
  ],
  HOSPITAL_ADMIN: [
    { to: '/', label: 'Home', icon: Home },
    { to: '/staff/tasks', label: 'My tasks', icon: ClipboardList },
    { to: '/operations', label: 'Operations', icon: LayoutDashboard },
    { to: '/ambulance', label: 'Ambulance', icon: Ambulance },
    { to: '/navigate', label: 'Navigate', icon: Navigation },
  ],
  SUPER_ADMIN: [
    { to: '/', label: 'Home', icon: Home },
    { to: '/staff/tasks', label: 'My tasks', icon: ClipboardList },
    { to: '/operations', label: 'Operations', icon: LayoutDashboard },
    { to: '/ambulance', label: 'Ambulance', icon: Ambulance },
    { to: '/navigate', label: 'Navigate', icon: Navigation },
  ],
}

function navClass({ isActive }: { isActive: boolean }) {
  return `relative flex items-center gap-2 px-3 py-7 text-sm font-bold transition after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 after:rounded-full ${
    isActive ? 'text-care-700 after:bg-care-600' : 'text-slate-600 after:bg-transparent hover:text-ink-950'
  }`
}

function workspaceNavClass({ isActive }: { isActive: boolean }) {
  return `flex min-h-12 items-center gap-3 rounded-xl px-4 text-sm font-extrabold transition ${
    isActive ? 'bg-gradient-to-r from-blue-700 to-indigo-600 text-white shadow-md shadow-blue-800/15' : 'text-slate-600 hover:bg-blue-50 hover:text-ink-950'
  }`
}

function ScrollToTop() {
  const { pathname } = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])
  return null
}

export function MainLayout() {
  const { session, logout } = useAuth()
  const { language, setLanguage, text } = useLanguage()
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const [unread, setUnread] = useState(0)
  const staffRole = session?.user.roles.find((role) => staffLinksByRole[role])
  const isStaff = Boolean(staffRole)
  const isDoctor = Boolean(session?.user.roles.includes('DOCTOR'))
  const isPatient = Boolean(session?.user.roles.includes('PATIENT') && !isDoctor && !isStaff)
  const isFrontDesk = Boolean(session?.user.roles.some((role) => ['RECEPTIONIST', 'CASHIER'].includes(role)))
  const desktopLinks = isDoctor ? doctorLinks : staffRole ? staffLinksByRole[staffRole] : patientDesktopLinks
  const mobileLinks = (isDoctor || staffRole ? desktopLinks : patientMobileLinks).slice(0, 5)
  const workspacePath = isDoctor ? '/doctor/consultations' : isFrontDesk ? '/operations' : isStaff ? '/staff/tasks' : '/dashboard'
  const canUseAmbulance = !session || session.user.roles.some((role) => ['PATIENT', 'AMBULANCE_DISPATCHER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role))
  const footerLinks = isDoctor
    ? [['/doctor/consultations', 'Finalize consultations'], ['/operations', 'Doctor day operations'], ['/navigate', 'Hospital navigation']]
    : isStaff
      ? [...(!isFrontDesk ? [['/staff/tasks', 'My assigned work']] : []), ...(session?.user.roles.some((role) => ['RECEPTIONIST', 'CASHIER', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role)) ? [['/operations', 'Hospital operations']] : []), ...(canUseAmbulance ? [['/ambulance', 'Ambulance coordination']] : []), ...(session?.user.roles.some((role) => ['LAB_TECHNICIAN', 'BLOOD_BANK_STAFF'].includes(role)) ? [['/blood-group-analysis', 'Blood-slide review']] : []), ['/navigate', 'Hospital navigation']]
      : patientFooterLinks
  const journeyLinks = isDoctor
    ? [['/doctor/consultations', 'Clinical consultations'], ['/operations', 'Doctor day operations'], ['/navigate', 'Hospital navigation']]
    : isStaff
      ? [['/', 'SmartCare home'], ['/hospitals', 'Hospital directory'], ['/navigate', 'Hospital navigation']]
      : [['/notifications', 'Live queue · Available'], ['/navigate', 'QR hospital map · Available'], ['/diagnostics', 'Verified diagnostics · Available'], ['/blood-support', 'Verified blood support · Available'], ['/ambulance', 'Ambulance workflow · Available'], ['/assistant', 'Cited care assistant · Available']]
  useEffect(() => {
    if (isStaticDemo) { setUnread(0); return }
    if (!session || !isPatient) { setUnread(0); return }
    let active = true
    const refresh = () => getUnreadCount().then((count) => { if (active) setUnread(count) }).catch(() => undefined)
    void refresh()
    const timer = window.setInterval(refresh, 30_000)
    return () => { active = false; window.clearInterval(timer) }
  }, [isPatient, session, pathname])
  return (
    <div className="min-h-screen bg-white">
      <ScrollToTop />
      {session && <aside className="fixed inset-y-0 left-0 z-50 hidden w-64 flex-col border-r border-slate-200 bg-white px-4 py-5 shadow-[12px_0_40px_-32px_rgba(9,45,87,.35)] lg:flex">
        <NavLink to={workspacePath} className="px-2 py-1"><BrandMark /></NavLink>
        <nav className="mt-8 grid gap-1.5" aria-label="Workspace navigation">
          {desktopLinks.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} end={to === '/'} className={workspaceNavClass}><Icon className="size-5 shrink-0" />{language === 'hi' ? hindiLabels[label] ?? label : label}</NavLink>)}
        </nav>
        <div className="mt-auto overflow-hidden rounded-2xl border border-indigo-100 bg-indigo-50">
          <img src={publicAsset('images/smartcare-clinical-team.png')} alt="SmartCare clinical team illustration" className="h-36 w-full object-cover object-top" />
          <div className="p-4"><p className="font-black text-ink-950">{text('Care that stays connected', 'जुड़ी हुई देखभाल')}</p><p className="mt-1 text-xs leading-5 text-slate-600">{text('Your role shows only the tools you need.', 'आपकी भूमिका के अनुसार केवल ज़रूरी साधन दिखते हैं।')}</p></div>
        </div>
      </aside>}
      <header className={`sticky top-0 z-40 shadow-[0_1px_0_rgba(15,23,42,.08)] ${session ? 'lg:ml-64' : ''}`}>
        <div className={`bg-ink-950 text-white ${session ? 'lg:hidden' : ''}`}>
          <div className="mx-auto flex h-9 max-w-7xl items-center justify-between px-4 text-[11px] font-bold sm:px-6 lg:px-8">
            {canUseAmbulance ? <NavLink to="/ambulance" className="flex items-center gap-2 text-blue-100 transition hover:text-white"><Ambulance className="size-3.5 text-red-400" /><span>{text('Emergency care is always prioritized · Ambulance coordination', 'आपातकालीन देखभाल हमेशा प्राथमिकता है · एम्बुलेंस समन्वय')}</span></NavLink> : <span className="flex items-center gap-2 text-blue-100"><Ambulance className="size-3.5 text-red-400" /><span>{text('Emergency care is always prioritized', 'आपातकालीन देखभाल हमेशा प्राथमिकता है')}</span></span>}
            <button type="button" onClick={() => setLanguage(language === 'en' ? 'hi' : 'en')} className="ml-2 flex shrink-0 items-center gap-1 rounded-lg border border-white/15 px-2 py-1 text-[10px] font-black text-blue-100 md:hidden" aria-label={text('Switch to Hindi', 'अंग्रेज़ी में बदलें')}><Languages className="size-3" />{language === 'en' ? 'हिं' : 'EN'}</button>
            <div className="hidden items-center gap-5 text-blue-100 md:flex"><span className="flex items-center gap-1.5"><Clock3 className="size-3.5" />{text('24/7 patient guidance', '24/7 रोगी सहायता')}</span><span className="flex items-center gap-1.5"><MapPin className="size-3.5" />{text('Hospital locations', 'अस्पताल स्थान')}</span></div>
          </div>
        </div>
        <div className="bg-white/95 backdrop-blur-xl">
          <div className="mx-auto flex h-20 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
            <NavLink to="/" className={`rounded-xl ${session ? 'lg:hidden' : ''}`}><BrandMark /></NavLink>
            {session && <div className="hidden lg:block"><p className="text-xs font-extrabold uppercase tracking-[.18em] text-care-700">{isDoctor ? text('Doctor workspace', 'डॉक्टर कार्यक्षेत्र') : isStaff ? text('Staff workspace', 'स्टाफ कार्यक्षेत्र') : text('Patient workspace', 'रोगी कार्यक्षेत्र')}</p><p className="mt-1 text-sm font-bold text-slate-500">{text('Care, queue and hospital services', 'देखभाल, कतार और अस्पताल सेवाएँ')}</p></div>}
            <nav className={`hidden items-center ${session ? '' : 'lg:flex'}`} aria-label="Primary navigation">
              {desktopLinks.map(({ to, label, icon: Icon }) => (
                <NavLink key={to} to={to} end={to === '/'} className={navClass}>
                  <Icon className="size-4" />{language === 'hi' ? hindiLabels[label] ?? label : label}
                </NavLink>
              ))}
            </nav>
            <div className="flex items-center gap-2">
              <div className="hidden items-center rounded-xl border border-slate-200 bg-slate-50 p-1 sm:flex" aria-label="Language">
                <Languages className="mx-1 size-4 text-slate-500" />
                <button type="button" onClick={() => setLanguage('en')} aria-pressed={language === 'en'} className={`rounded-lg px-2 py-1.5 text-xs font-black ${language === 'en' ? 'bg-white text-care-700 shadow-sm' : 'text-slate-500'}`}>EN</button>
                <button type="button" onClick={() => setLanguage('hi')} aria-pressed={language === 'hi'} className={`rounded-lg px-2 py-1.5 text-xs font-black ${language === 'hi' ? 'bg-white text-care-700 shadow-sm' : 'text-slate-500'}`}>हिं</button>
              </div>
              {session ? (
                <>{isPatient && <NavLink to="/notifications" aria-label={`${unread} unread notifications`} className="relative grid size-11 place-items-center rounded-xl border border-slate-200 text-slate-600 transition hover:border-care-300 hover:bg-care-50 hover:text-care-700"><Bell className="size-4" />{unread > 0 && <span className="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-black leading-5 text-white">{unread > 99 ? '99+' : unread}</span>}</NavLink>}<NavLink to={workspacePath} className="flex items-center gap-2 rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm font-bold text-ink-950 transition hover:border-care-300 hover:bg-care-50">
                  <LayoutDashboard className="size-4 text-care-700" />
                  <span className="hidden sm:inline">{session.user.displayName.split(' ')[0]}</span>
                </NavLink><button type="button" aria-label="Sign out" title="Sign out" onClick={() => { logout(); navigate('/login', { replace: true }) }} className="grid size-11 place-items-center rounded-xl border border-slate-200 text-slate-600 transition hover:border-rose-300 hover:bg-rose-50 hover:text-rose-700"><LogOut className="size-4" /></button></>
              ) : (
                <NavLink to="/login" className="hidden items-center gap-2 rounded-xl px-3.5 py-2.5 text-sm font-bold text-slate-700 transition hover:bg-slate-50 sm:flex">
                  <LogIn className="size-4" /> {text('Sign in', 'साइन इन')}
                </NavLink>
              )}
              {!session || (!isDoctor && !isStaff) ? <NavLink to="/booking" className="flex items-center gap-2 rounded-xl bg-care-600 px-4 py-2.5 text-sm font-extrabold text-white shadow-md shadow-blue-700/15 transition hover:bg-care-700">
                <TicketCheck className="size-4" /><span className="hidden xs:inline">{text('Book OPD number', 'OPD नंबर बुक करें')}</span><span className="xs:hidden">{text('Book OPD', 'OPD बुक करें')}</span>
              </NavLink> : null}
            </div>
          </div>
        </div>
      </header>

      {isStaticDemo && <div className="border-y border-amber-200 bg-amber-50 px-4 py-2 text-center text-xs font-bold text-amber-950">Public portfolio prototype · Sample browser data only · Backend-only medical workflows are not active on GitHub Pages.</div>}
      <main className={`safe-bottom bg-[#f4f9fd] md:pb-0 ${session ? 'lg:ml-64' : ''}`}><Outlet /></main>

      {!session && <footer className="bg-[#071b35] text-white">
        <div className="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-2 lg:grid-cols-[1.2fr_.8fr_.8fr_1fr] lg:px-8 lg:py-16">
          <div>
            <BrandMark inverted />
            <p className="mt-5 max-w-sm text-sm leading-6 text-blue-100/70">Care made clear, queues made fair and journeys kept connected—from OPD booking to hospital navigation.</p>
            <div className="mt-6 flex flex-wrap gap-2 text-[11px] font-bold text-blue-100/70"><span className="rounded-full border border-white/10 px-3 py-1.5">Privacy-first</span><span className="rounded-full border border-white/10 px-3 py-1.5">Clinician-led</span><span className="rounded-full border border-white/10 px-3 py-1.5">Accessible</span></div>
          </div>
          <div>
            <h2 className="text-sm font-black">{isDoctor ? 'Doctor workspace' : isStaff ? 'Staff workspace' : 'Patient links'}</h2>
            <div className="mt-5 space-y-3 text-sm text-blue-100/70">{footerLinks.map(([to, label]) => <NavLink key={to} to={to} className="block hover:text-white">{label}</NavLink>)}</div>
          </div>
          <div>
            <h2 className="text-sm font-black">Care journey</h2>
            <div className="mt-5 space-y-3 text-sm text-blue-100/70">{journeyLinks.map(([to, label]) => <NavLink key={to} to={to} className="block hover:text-white">{label}</NavLink>)}</div>
          </div>
          <div>
            <h2 className="text-sm font-black">Patient support</h2>
            <div className="mt-5 space-y-4 text-sm text-blue-100/70"><p className="flex gap-3"><Phone className="mt-0.5 size-4 shrink-0 text-blue-300" />Contact your hospital care desk</p><p className="flex gap-3"><TicketCheck className="mt-0.5 size-4 shrink-0 text-blue-300" />Capacity-based OPD booking and fair waitlist</p><p className="flex gap-3"><Ambulance className="mt-0.5 size-4 shrink-0 text-red-400" />For emergencies, contact local emergency services immediately.</p></div>
          </div>
        </div>
        <div className="border-t border-white/10">
          <div className="mx-auto flex max-w-7xl flex-col gap-2 px-4 pb-24 pt-5 text-xs text-blue-100/55 sm:flex-row sm:items-center sm:justify-between sm:px-6 md:pb-5 lg:px-8"><p>© 2026 SmartCare. Navigate · Queue · Care.</p><p>Medical decisions remain with qualified care teams.</p></div>
        </div>
      </footer>}

      <nav className="fixed inset-x-0 bottom-0 z-50 grid grid-cols-5 border-t border-slate-200 bg-white/95 px-1 pt-2 pb-[calc(.5rem+env(safe-area-inset-bottom))] backdrop-blur-xl md:hidden" aria-label="Mobile navigation">
        {mobileLinks.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) =>
            `flex min-h-13 flex-col items-center justify-center gap-1 rounded-xl text-xs font-bold ${isActive ? 'text-care-700' : 'text-slate-500'}`
          }>
            <Icon className="size-5" />{language === 'hi' ? hindiLabels[label] ?? label : label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
