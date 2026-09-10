import {
  ArrowRight,
  Bot,
  CalendarPlus,
  Check,
  ChevronRight,
  ClipboardCheck,
  Droplets,
  FlaskConical,
  HeartHandshake,
  Languages,
  MapPinned,
  Navigation,
  ShieldCheck,
  Siren,
  Stethoscope,
  TicketCheck,
  Users,
} from 'lucide-react'
import { publicAsset } from '../config/runtime'
import { Link } from 'react-router-dom'
import { ActionCard } from '../components/ActionCard'

const actions = [
  { title: 'Book OPD number', description: 'Choose a doctor and date, then protect your queue position fairly.', icon: TicketCheck, to: '/booking', tone: 'care' as const },
  { title: 'Find a doctor', description: 'Search verified doctors by hospital, department or speciality.', icon: Stethoscope, to: '/doctors', tone: 'blue' as const },
  { title: 'My queue', description: 'See people ahead, expected wait time and status updates in one view.', icon: TicketCheck, status: 'Phase 4', tone: 'amber' as const },
  { title: 'Hospital navigation', description: 'Follow clear indoor directions using verified QR checkpoints.', icon: MapPinned, status: 'Phase 5', tone: 'blue' as const },
  { title: 'Health records', description: 'Keep doctor-finalized visits, medicines, allergies and uploaded reports private.', icon: FlaskConical, to: '/records', status: 'Available', tone: 'care' as const },
  { title: 'Care assistant', description: 'Ask your private care record and inspect the authorized source behind each answer.', icon: Bot, to: '/assistant', status: 'Available', tone: 'blue' as const },
  { title: 'Diagnostics & results', description: 'Schedule clinician-ordered lab or imaging services and view staff-verified results.', icon: FlaskConical, to: '/diagnostics', status: 'Available', tone: 'care' as const },
  { title: 'Blood availability', description: 'View authorized inventory with a clear last-verified timestamp.', icon: Droplets, to: '/blood-support', status: 'Available', tone: 'rose' as const },
  { title: 'Ambulance coordination', description: 'Request transport and follow dispatcher-confirmed handoffs without automatic dispatch.', icon: Siren, to: '/ambulance', status: 'Available', tone: 'rose' as const },
]

const journey = [
  { number: '01', title: 'Find your care team', body: 'Search verified hospitals, departments and doctors before you leave home.' },
  { number: '02', title: 'Arrive prepared', body: 'Keep appointment details, documents and arrival guidance together.' },
  { number: '03', title: 'Move without confusion', body: 'Follow queue updates and hospital-maintained directions at every step.' },
  { number: '04', title: 'Continue after the visit', body: 'Track tests, reports and the next action in one private patient space.' },
]

const values = [
  { icon: ShieldCheck, title: 'Private by design', body: 'Patient-level access, clear roles and auditable actions protect sensitive care data.', tone: 'bg-blue-500' },
  { icon: HeartHandshake, title: 'Human-led care', body: 'Clinical decisions, triage and overrides always remain with qualified hospital staff.', tone: 'bg-emerald-500' },
  { icon: Navigation, title: 'Verified guidance', body: 'Directions come from hospital-maintained locations and QR checkpoints—not guesswork.', tone: 'bg-violet-500' },
  { icon: Languages, title: 'Easy to understand', body: 'Plain language, large controls and multilingual-ready experiences reduce anxiety.', tone: 'bg-amber-500' },
]

export function HomePage() {
  return (
    <>
      <section className="relative overflow-hidden bg-[#f2f7fc]">
        <div className="pointer-events-none absolute -left-32 top-8 size-96 rounded-full bg-blue-200/30 blur-3xl" />
        <div className="pointer-events-none absolute -right-28 bottom-0 size-[30rem] rounded-full bg-cyan-100/60 blur-3xl" />

        <div className="relative mx-auto grid max-w-[90rem] items-center gap-10 px-4 py-12 sm:px-6 sm:py-16 lg:grid-cols-[.92fr_1.08fr] lg:px-8 lg:py-20">
          <div className="z-10 max-w-2xl lg:py-4">
            <div className="inline-flex items-center gap-2 rounded-full border border-blue-200 bg-white/90 px-3.5 py-2 text-xs font-extrabold uppercase tracking-[.13em] text-care-700 shadow-sm">
              <ShieldCheck className="size-4" /> Built for Indian OPD workflows
            </div>
            <h1 className="text-balance mt-6 text-[2.75rem] font-black leading-[1.03] tracking-[-0.045em] text-ink-950 sm:text-6xl lg:text-[4.4rem]">
              Care made clear.<br />
              <span className="text-care-600">Queues made fair. Journeys connected.</span>
            </h1>
            <p className="mt-6 max-w-xl text-base leading-7 text-slate-600 sm:text-lg sm:leading-8">
              SmartCare brings advance OPD-number booking, live queue status, indoor navigation and the next care step into one patient journey.
            </p>

            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Link to="/booking" className="inline-flex min-h-13 items-center justify-center gap-2 rounded-xl bg-care-600 px-6 py-3.5 text-base font-extrabold text-white shadow-lg shadow-blue-700/20 transition hover:-translate-y-0.5 hover:bg-care-700">
                Book OPD number <ArrowRight className="size-5" />
              </Link>
              <Link to="/doctors" className="inline-flex min-h-13 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-6 py-3.5 text-base font-extrabold text-ink-950 transition hover:border-care-300 hover:bg-care-50">
                Find a doctor
              </Link>
              <Link to="/hospitals" className="inline-flex min-h-13 items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-6 py-3.5 text-base font-extrabold text-ink-950 transition hover:border-care-300 hover:bg-care-50">
                Explore hospitals
              </Link>
            </div>

            <div className="mt-8 grid max-w-lg grid-cols-2 gap-x-6 gap-y-3 text-sm font-bold text-slate-600 sm:grid-cols-3">
              {['Advance OPD token', 'Fair waitlist', 'QR navigation'].map((item) => (
                <span key={item} className="flex items-center gap-2"><Check className="size-4 text-emerald-600" />{item}</span>
              ))}
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-3xl lg:mr-0">
            <div className="relative overflow-hidden rounded-[2rem] bg-slate-900 shadow-[0_35px_80px_-28px_rgba(9,45,87,.4)]">
              <img
                src={publicAsset('images/doctor-consultation.jpg')}
                alt="Doctor listening carefully to a patient during a consultation"
                className="h-[28rem] w-full object-cover object-center sm:h-[34rem] lg:h-[38rem]"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-[#08294d]/55 via-transparent to-transparent" />
              <div className="absolute right-4 top-4 flex max-w-[calc(100%-2rem)] items-center gap-3 rounded-2xl border border-white/30 bg-white/95 p-3.5 shadow-xl backdrop-blur sm:right-6 sm:top-6 sm:p-4">
                <span className="grid size-10 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-700"><TicketCheck className="size-5" /></span>
                <div>
                  <p className="text-[10px] font-extrabold uppercase tracking-[.14em] text-slate-500">Example OPD token</p>
                  <p className="mt-0.5 text-sm font-black text-ink-950">#37 · Position confirmed</p>
                </div>
              </div>
              <div className="absolute bottom-4 left-4 max-w-[calc(100%-2rem)] rounded-2xl border border-white/30 bg-[#071b35]/92 p-4 text-white shadow-xl backdrop-blur sm:bottom-6 sm:left-6 sm:w-[22rem] sm:p-5">
                <div className="flex items-center justify-between gap-4"><div><p className="text-[10px] font-extrabold uppercase tracking-[.14em] text-blue-300">Example live queue</p><p className="mt-1 text-sm font-black">Now serving #28</p></div><span className="rounded-full bg-emerald-400/15 px-3 py-1 text-xs font-bold text-emerald-200">Live</span></div>
                <div className="mt-4 grid grid-cols-2 gap-3 border-t border-white/10 pt-3 text-xs"><p><span className="block text-lg font-black">8</span><span className="text-blue-100/65">Patients ahead</span></p><p><span className="block text-lg font-black">~42 min</span><span className="text-blue-100/65">Estimated wait</span></p></div>
              </div>
            </div>
          </div>
        </div>

        <div className="relative mx-auto max-w-7xl px-4 pb-8 sm:px-6 lg:-mt-4 lg:px-8 lg:pb-0">
          <div className="grid overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-[0_22px_60px_-30px_rgba(9,45,87,.35)] sm:grid-cols-3">
            {[
              [CalendarPlus, 'Book OPD number', 'Reserve before reaching hospital', '/booking'],
              [Users, 'Check my queue', 'Number, patients ahead & wait time', '/dashboard'],
              [MapPinned, 'Scan & navigate', 'Verified hospital checkpoints', '#journey'],
            ].map(([Icon, title, body, to], index) => {
              const ItemIcon = Icon as typeof Stethoscope
              return (
                <a key={title as string} href={to as string} className={`group flex items-center gap-4 p-5 transition hover:bg-blue-50/60 sm:p-6 ${index !== 2 ? 'border-b border-slate-200 sm:border-b-0 sm:border-r' : ''}`}>
                  <span className="grid size-12 shrink-0 place-items-center rounded-xl bg-care-50 text-care-700"><ItemIcon className="size-6" /></span>
                  <div><p className="font-black text-ink-950">{title as string}</p><p className="mt-0.5 text-xs leading-5 text-slate-500">{body as string}</p></div>
                  <ChevronRight className="ml-auto size-5 text-slate-300 transition group-hover:translate-x-1 group-hover:text-care-600" />
                </a>
              )
            })}
          </div>
        </div>
      </section>

      <section className="bg-white py-14 sm:py-20" id="services">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-3xl text-center">
            <p className="text-xs font-extrabold uppercase tracking-[.22em] text-care-700">Everything you need</p>
            <h2 className="mt-3 text-3xl font-black tracking-[-0.035em] text-ink-950 sm:text-4xl">Your complete hospital journey, connected</h2>
            <p className="mt-4 text-base leading-7 text-slate-600">Start with the verified doctor directory today. Each upcoming feature is clearly labelled while it is being prepared.</p>
          </div>
          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {actions.map((action) => <ActionCard key={action.title} {...action} />)}
          </div>
        </div>
      </section>

      <section className="overflow-hidden bg-[#f3f7fb] py-14 sm:py-20" id="journey">
        <div className="mx-auto grid max-w-7xl items-center gap-12 px-4 sm:px-6 lg:grid-cols-[1.04fr_.96fr] lg:px-8">
          <div className="relative min-h-[31rem] sm:min-h-[36rem]">
            <img src={publicAsset('images/hospital-lobby.jpg')} alt="Bright hospital reception and wayfinding area" className="absolute left-0 top-0 h-[82%] w-[82%] rounded-[1.75rem] object-cover shadow-[0_26px_60px_-30px_rgba(9,45,87,.45)]" />
            <img src={publicAsset('images/medical-centre.jpg')} alt="Modern medical centre exterior" className="absolute bottom-0 right-0 h-[48%] w-[55%] rounded-[1.5rem] border-[6px] border-[#f3f7fb] object-cover shadow-[0_24px_50px_-26px_rgba(9,45,87,.5)]" />
            <div className="absolute bottom-7 left-5 max-w-[15rem] rounded-2xl bg-care-700 p-5 text-white shadow-xl sm:left-8">
              <Navigation className="size-7 text-blue-200" />
              <p className="mt-4 text-lg font-black">Know where to go next.</p>
              <p className="mt-1 text-xs leading-5 text-blue-100">Verified checkpoints replace confusing indoor guesswork.</p>
            </div>
          </div>

          <div>
            <p className="text-xs font-extrabold uppercase tracking-[.22em] text-care-700">Plan your journey</p>
            <h2 className="text-balance mt-3 text-3xl font-black tracking-[-0.035em] text-ink-950 sm:text-4xl">From finding a doctor to getting home, stay one step ahead.</h2>
            <p className="mt-4 max-w-xl text-base leading-7 text-slate-600">SmartCare connects the practical steps around care so patients and families spend less energy figuring out the system.</p>
            <div className="mt-8 space-y-2">
              {journey.map((item) => (
                <div key={item.number} className="group grid grid-cols-[3rem_1fr] gap-4 rounded-2xl border border-transparent p-3 transition hover:border-blue-100 hover:bg-white">
                  <span className="grid size-12 place-items-center rounded-xl bg-white text-sm font-black text-care-700 shadow-sm">{item.number}</span>
                  <div className="pt-1"><h3 className="font-black text-ink-950">{item.title}</h3><p className="mt-1 text-sm leading-6 text-slate-600">{item.body}</p></div>
                </div>
              ))}
            </div>
            <Link to="/register" className="mt-7 inline-flex items-center gap-2 rounded-xl bg-care-600 px-5 py-3 text-sm font-extrabold text-white shadow-lg shadow-blue-700/15 transition hover:bg-care-700">
              Start your patient journey <ArrowRight className="size-4" />
            </Link>
          </div>
        </div>
      </section>

      <section className="bg-ink-950 py-14 text-white sm:py-20">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-3xl text-center">
            <p className="text-xs font-extrabold uppercase tracking-[.22em] text-blue-300">Built around trust</p>
            <h2 className="mt-3 text-3xl font-black tracking-[-0.035em] sm:text-4xl">Technology that respects how care really works</h2>
            <p className="mt-4 text-base leading-7 text-blue-100/75">Thoughtful safeguards keep information useful for patients and accountable to hospitals.</p>
          </div>
          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {values.map(({ icon: Icon, title, body, tone }) => (
              <article key={title} className="rounded-2xl border border-white/10 bg-white/[.07] p-6 backdrop-blur-sm transition hover:-translate-y-1 hover:bg-white/[.1]">
                <span className={`grid size-12 place-items-center rounded-xl text-white ${tone}`}><Icon className="size-6" /></span>
                <h3 className="mt-5 text-lg font-black">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-blue-100/75">{body}</p>
              </article>
            ))}
          </div>
          <div className="mt-8 grid grid-cols-2 gap-3 border-t border-white/10 pt-8 text-center sm:grid-cols-4">
            {[
              [Users, 'Patient-first'],
              [ClipboardCheck, 'Auditable'],
              [ShieldCheck, 'Role protected'],
              [Stethoscope, 'Clinician led'],
            ].map(([Icon, label]) => {
              const ItemIcon = Icon as typeof Users
              return <div key={label as string} className="flex items-center justify-center gap-2 text-sm font-bold text-blue-100"><ItemIcon className="size-4 text-blue-300" />{label as string}</div>
            })}
          </div>
        </div>
      </section>

      <section className="bg-white py-14 sm:py-20" id="support">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="relative overflow-hidden rounded-[2rem] bg-care-700 px-6 py-12 shadow-[0_30px_70px_-35px_rgba(9,45,87,.55)] sm:px-12 lg:px-16">
            <div className="absolute inset-0 opacity-20 [background:radial-gradient(circle_at_85%_30%,white,transparent_28%)]" />
            <div className="relative flex flex-col items-start justify-between gap-8 lg:flex-row lg:items-center">
              <div className="max-w-2xl text-white">
                <p className="text-xs font-extrabold uppercase tracking-[.2em] text-blue-200">Ready when you are</p>
                <h2 className="mt-3 text-3xl font-black tracking-[-0.03em] sm:text-4xl">A calmer hospital visit starts before you arrive.</h2>
                <p className="mt-4 text-base leading-7 text-blue-100">Find verified care now, or create your secure patient space for the journey ahead.</p>
              </div>
              <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row">
                <Link to="/doctors" className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-extrabold text-care-800 shadow-lg transition hover:-translate-y-0.5">Find a doctor <ArrowRight className="size-4" /></Link>
                <Link to="/register" className="inline-flex min-h-12 items-center justify-center rounded-xl border border-white/35 bg-white/10 px-5 py-3 text-sm font-extrabold text-white transition hover:bg-white/20">Create account</Link>
              </div>
            </div>
          </div>
        </div>
      </section>
    </>
  )
}
