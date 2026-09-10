import { Ambulance, Bell, Building2, CalendarDays, ClipboardList, Clock3, Droplets, FileHeart, FlaskConical, Home, LayoutDashboard, LogIn, MapPin, Navigation, Phone, TicketCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { isStaticDemo } from '../config/runtime'
import { getUnreadCount } from '../services/notifications'
import { BrandMark } from './BrandMark'

const desktopLinks = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/hospitals', label: 'Hospitals', icon: Building2 },
  { to: '/booking', label: 'Book OPD', icon: TicketCheck },
  { to: '/navigate', label: 'Navigate', icon: Navigation },
  { to: '/diagnostics', label: 'Tests', icon: FlaskConical },
  { to: '/records', label: 'Records', icon: FileHeart },
  { to: '/blood-support', label: 'Blood', icon: Droplets },
  { to: '/dashboard', label: 'My care', icon: CalendarDays },
]

const mobileLinks = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/booking', label: 'Book OPD', icon: TicketCheck },
  { to: '/navigate', label: 'Navigate', icon: Navigation },
  { to: '/records', label: 'Records', icon: FileHeart },
  { to: '/dashboard', label: 'My care', icon: CalendarDays },
]

function navClass({ isActive }: { isActive: boolean }) {
  return `relative flex items-center gap-2 px-3 py-7 text-sm font-bold transition after:absolute after:inset-x-3 after:bottom-0 after:h-0.5 after:rounded-full ${
    isActive ? 'text-care-700 after:bg-care-600' : 'text-slate-600 after:bg-transparent hover:text-ink-950'
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
  const { session } = useAuth()
  const { pathname } = useLocation()
  const [unread, setUnread] = useState(0)
  const isStaff = Boolean(session?.user.roles.some((role) => ['AMBULANCE_DISPATCHER', 'BLOOD_BANK_STAFF', 'LAB_TECHNICIAN', 'HOSPITAL_ADMIN', 'SUPER_ADMIN'].includes(role)))
  useEffect(() => {
    if (isStaticDemo) { setUnread(0); return }
    if (!session) { setUnread(0); return }
    let active = true
    const refresh = () => getUnreadCount().then((count) => { if (active) setUnread(count) }).catch(() => undefined)
    void refresh()
    const timer = window.setInterval(refresh, 30_000)
    return () => { active = false; window.clearInterval(timer) }
  }, [session, pathname])
  return (
    <div className="min-h-screen bg-white">
      <ScrollToTop />
      <header className="sticky top-0 z-40 shadow-[0_1px_0_rgba(15,23,42,.08)]">
        <div className="bg-ink-950 text-white">
          <div className="mx-auto flex h-9 max-w-7xl items-center justify-between px-4 text-[11px] font-bold sm:px-6 lg:px-8">
            <NavLink to="/ambulance" className="flex items-center gap-2 text-blue-100 transition hover:text-white"><Ambulance className="size-3.5 text-red-400" /><span>Emergency care is always prioritized · Ambulance coordination</span></NavLink>
            <div className="hidden items-center gap-5 text-blue-100 md:flex"><span className="flex items-center gap-1.5"><Clock3 className="size-3.5" />24/7 patient guidance</span><span className="flex items-center gap-1.5"><MapPin className="size-3.5" />Hospital locations</span></div>
          </div>
        </div>
        <div className="bg-white/95 backdrop-blur-xl">
          <div className="mx-auto flex h-20 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
            <NavLink to="/" className="rounded-xl"><BrandMark /></NavLink>
            <nav className="hidden items-center lg:flex" aria-label="Primary navigation">
              {desktopLinks.map(({ to, label, icon: Icon }) => (
                <NavLink key={to} to={to} end={to === '/'} className={navClass}>
                  <Icon className="size-4" />{label}
                </NavLink>
              ))}
            </nav>
            <div className="flex items-center gap-2">
              {session ? (
                <>{isStaff && <NavLink to="/staff/tasks" aria-label="Open staff task inbox" className="relative grid size-11 place-items-center rounded-xl border border-slate-200 text-slate-600 transition hover:border-care-300 hover:bg-care-50 hover:text-care-700"><ClipboardList className="size-4" /></NavLink>}<NavLink to="/notifications" aria-label={`${unread} unread notifications`} className="relative grid size-11 place-items-center rounded-xl border border-slate-200 text-slate-600 transition hover:border-care-300 hover:bg-care-50 hover:text-care-700"><Bell className="size-4" />{unread > 0 && <span className="absolute -right-1 -top-1 grid min-w-5 place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-black leading-5 text-white">{unread > 99 ? '99+' : unread}</span>}</NavLink><NavLink to="/dashboard" className="flex items-center gap-2 rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm font-bold text-ink-950 transition hover:border-care-300 hover:bg-care-50">
                  <LayoutDashboard className="size-4 text-care-700" />
                  <span className="hidden sm:inline">{session.user.displayName.split(' ')[0]}</span>
                </NavLink></>
              ) : (
                <NavLink to="/login" className="hidden items-center gap-2 rounded-xl px-3.5 py-2.5 text-sm font-bold text-slate-700 transition hover:bg-slate-50 sm:flex">
                  <LogIn className="size-4" /> Sign in
                </NavLink>
              )}
              <NavLink to="/booking" className="flex items-center gap-2 rounded-xl bg-care-600 px-4 py-2.5 text-sm font-extrabold text-white shadow-md shadow-blue-700/15 transition hover:bg-care-700">
                <TicketCheck className="size-4" /><span className="hidden xs:inline">Book OPD number</span><span className="xs:hidden">Book OPD</span>
              </NavLink>
            </div>
          </div>
        </div>
      </header>

      {isStaticDemo && <div className="border-y border-amber-200 bg-amber-50 px-4 py-2 text-center text-xs font-bold text-amber-950">Public portfolio prototype · Sample browser data only · Backend-only medical workflows are not active on GitHub Pages.</div>}
      <main className="safe-bottom md:pb-0"><Outlet /></main>

      <footer className="bg-[#071b35] text-white">
        <div className="mx-auto grid max-w-7xl gap-10 px-4 py-12 sm:px-6 md:grid-cols-2 lg:grid-cols-[1.2fr_.8fr_.8fr_1fr] lg:px-8 lg:py-16">
          <div>
            <BrandMark inverted />
            <p className="mt-5 max-w-sm text-sm leading-6 text-blue-100/70">Care made clear, queues made fair and journeys kept connected—from OPD booking to hospital navigation.</p>
            <div className="mt-6 flex flex-wrap gap-2 text-[11px] font-bold text-blue-100/70"><span className="rounded-full border border-white/10 px-3 py-1.5">Privacy-first</span><span className="rounded-full border border-white/10 px-3 py-1.5">Clinician-led</span><span className="rounded-full border border-white/10 px-3 py-1.5">Accessible</span></div>
          </div>
          <div>
            <h2 className="text-sm font-black">Patient links</h2>
            <div className="mt-5 space-y-3 text-sm text-blue-100/70"><NavLink to="/booking" className="block hover:text-white">Book OPD number</NavLink><NavLink to="/hospitals" className="block hover:text-white">India hospital directory</NavLink><NavLink to="/doctors" className="block hover:text-white">Find a doctor</NavLink><NavLink to="/ambulance" className="block hover:text-white">Ambulance coordination</NavLink><NavLink to="/navigate" className="block hover:text-white">Hospital navigation</NavLink><NavLink to="/diagnostics" className="block hover:text-white">Diagnostics & results</NavLink><NavLink to="/blood-support" className="block hover:text-white">Blood support</NavLink><NavLink to="/blood-group-analysis" className="block hover:text-white">Blood-slide review</NavLink><NavLink to="/records" className="block hover:text-white">My health record</NavLink><NavLink to="/assistant" className="block hover:text-white">Care assistant</NavLink><NavLink to="/operations" className="block hover:text-white">Appointment recovery</NavLink><NavLink to="/dashboard" className="block hover:text-white">My care</NavLink></div>
          </div>
          <div>
            <h2 className="text-sm font-black">Care journey</h2>
            <div className="mt-5 space-y-3 text-sm text-blue-100/70"><NavLink to="/#services" className="block hover:text-white">Services</NavLink><NavLink to="/#journey" className="block hover:text-white">How it works</NavLink><NavLink to="/notifications" className="block hover:text-white">Live queue · Available</NavLink><NavLink to="/navigate" className="block hover:text-white">QR hospital map · Available</NavLink><NavLink to="/diagnostics" className="block hover:text-white">Verified diagnostics · Available</NavLink><NavLink to="/blood-support" className="block hover:text-white">Verified blood support · Available</NavLink><NavLink to="/ambulance" className="block hover:text-white">Ambulance workflow · Available</NavLink><NavLink to="/assistant" className="block hover:text-white">Cited care assistant · Available</NavLink></div>
          </div>
          <div>
            <h2 className="text-sm font-black">Patient support</h2>
            <div className="mt-5 space-y-4 text-sm text-blue-100/70"><p className="flex gap-3"><Phone className="mt-0.5 size-4 shrink-0 text-blue-300" />Contact your hospital care desk</p><p className="flex gap-3"><TicketCheck className="mt-0.5 size-4 shrink-0 text-blue-300" />Capacity-based OPD booking and fair waitlist</p><p className="flex gap-3"><Ambulance className="mt-0.5 size-4 shrink-0 text-red-400" />For emergencies, contact local emergency services immediately.</p></div>
          </div>
        </div>
        <div className="border-t border-white/10">
          <div className="mx-auto flex max-w-7xl flex-col gap-2 px-4 pb-24 pt-5 text-xs text-blue-100/55 sm:flex-row sm:items-center sm:justify-between sm:px-6 md:pb-5 lg:px-8"><p>© 2026 SmartCare. Navigate · Queue · Care.</p><p>Medical decisions remain with qualified care teams.</p></div>
        </div>
      </footer>

      <nav className="fixed inset-x-0 bottom-0 z-50 grid grid-cols-5 border-t border-slate-200 bg-white/95 px-1 pt-2 pb-[calc(.5rem+env(safe-area-inset-bottom))] backdrop-blur-xl md:hidden" aria-label="Mobile navigation">
        {mobileLinks.map(({ to, label, icon: Icon }) => (
          <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) =>
            `flex min-h-13 flex-col items-center justify-center gap-1 rounded-xl text-xs font-bold ${isActive ? 'text-care-700' : 'text-slate-500'}`
          }>
            <Icon className="size-5" />{label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
