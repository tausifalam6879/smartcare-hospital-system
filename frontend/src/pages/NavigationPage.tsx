import {
  Accessibility, AlertTriangle, ArrowDown, BadgeCheck, Building2, Check, ChevronRight,
  CircleParking, Cross, DoorOpen, FlaskConical, Footprints, Languages, LoaderCircle, LocateFixed,
  Map as MapIcon, MapPin, Navigation, Network, Pill, QrCode, Route, ShieldCheck, Sparkles, Stethoscope,
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useState, type ComponentType } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { messageFromError } from '../services/api'
import {
  getAppointmentDestination, getCheckpoint, getHospitalMap, getNavigationHospitals, getRoute,
  type AppointmentDestination, type Checkpoint, type HospitalLocation, type HospitalMap,
  type LocationType, type NavigationRoute,
} from '../services/navigation'
import { getPrototypeBookings, type PrototypeBooking } from '../services/prototypeBookings'

type Language = 'en' | 'hi'

const copy = {
  en: {
    eyebrow: 'QR hospital guide', title: 'Find your room without getting lost',
    intro: 'Scan a SmartCare board or choose the nearest checkpoint. Directions use the hospital’s verified indoor map.',
    source: 'Where are you now?', sourceHelp: 'Choose the QR board nearest to you', destination: 'Where do you want to go?',
    choose: 'Choose destination', guide: 'Show my route', accessible: 'Step-free route',
    accessibleHelp: 'Uses lifts and accessible corridors only', verified: 'Verified checkpoint',
    appointment: 'Your appointment room is selected', loading: 'Preparing the hospital map…',
    route: 'Your route', steps: 'simple steps', mins: 'min', metres: 'metres', arrived: 'You have arrived',
    start: 'Start here', safetyTitle: 'Built for safe indoor guidance', change: 'Change selection',
    noMap: 'This hospital has not published a verified indoor map yet.', noRoute: 'No verified route found',
    mapLabel: 'Simple route sketch', mapNote: 'Follow the written steps and hospital signs. This sketch is not a live GPS map.',
    signIn: 'Sign in to automatically select your appointment room.', allHospitals: 'Hospital',
    safetySub: 'QR checkpoints · verified map · no indoor GPS claim',
    fallbackTitle: 'Safe fallback guidance', fallbackRoute: 'The selected room has no complete verified route. Guidance now ends at the nearest mapped help desk.',
    demoTitle: 'Project simulation route', demoRoute: 'This directory hospital has no real indoor map in the project. A clearly labelled SmartCare demo route is shown to demonstrate the navigation workflow.',
    why: 'Why did the app choose this path?', algorithm: 'Dijkstra route in plain language',
    algorithmText: 'Each verified location is a node and each corridor is a connection. Dijkstra compares the accumulated travel time and selects the lowest-cost verified path. Step-free mode removes stair connections.',
    scan: 'Scan verified nodes', compare: 'Compare total cost', exclude: 'Remove stairs', select: 'Choose lowest cost',
    blue: 'Blue = chosen path', green: 'Green = destination', numbers: 'Numbers = written steps',
    nodes: 'mapped nodes', connections: 'route connections', optimized: 'optimized for', accessibleMode: 'step-free access', fastestMode: 'shortest time',
    readSteps: 'Read the numbered instructions in order. The sketch explains route shape and sequence; hospital signs and staff instructions always take priority.',
    floor: 'Floor', sample: 'Demo map', exact: 'Verified room', helpDesk: 'Help-desk fallback',
  },
  hi: {
    eyebrow: 'QR अस्पताल गाइड', title: 'बिना भटके अपना कमरा खोजें',
    intro: 'SmartCare बोर्ड स्कैन करें या पास का QR स्थान चुनें। रास्ता अस्पताल के सत्यापित इनडोर मानचित्र से मिलता है।',
    source: 'आप अभी कहाँ हैं?', sourceHelp: 'अपने सबसे पास का QR बोर्ड चुनें', destination: 'आपको कहाँ जाना है?',
    choose: 'जगह चुनें', guide: 'मेरा रास्ता दिखाएँ', accessible: 'सीढ़ी रहित रास्ता',
    accessibleHelp: 'केवल लिफ्ट और सुविधाजनक गलियारे', verified: 'सत्यापित QR स्थान',
    appointment: 'आपका अपॉइंटमेंट कमरा चुना गया है', loading: 'अस्पताल का मानचित्र तैयार हो रहा है…',
    route: 'आपका रास्ता', steps: 'आसान चरण', mins: 'मिनट', metres: 'मीटर', arrived: 'आप पहुँच गए',
    start: 'यहाँ से शुरू करें', safetyTitle: 'सुरक्षित इनडोर मार्गदर्शन', change: 'चयन बदलें',
    noMap: 'इस अस्पताल ने अभी सत्यापित इनडोर मानचित्र जारी नहीं किया है।', noRoute: 'सत्यापित रास्ता नहीं मिला',
    mapLabel: 'सरल रास्ते का नक्शा', mapNote: 'लिखे हुए चरण और अस्पताल के संकेत मानें। यह लाइव GPS नक्शा नहीं है।',
    signIn: 'अपना अपॉइंटमेंट कमरा अपने आप चुनने के लिए साइन इन करें।', allHospitals: 'अस्पताल',
    safetySub: 'QR चेकपॉइंट · सत्यापित नक्शा · इनडोर GPS का दावा नहीं',
    fallbackTitle: 'सुरक्षित वैकल्पिक मार्गदर्शन', fallbackRoute: 'चुने गए कमरे तक पूरा सत्यापित रास्ता उपलब्ध नहीं है। अब रास्ता सबसे पास के मैप किए गए सहायता डेस्क तक जाता है।',
    demoTitle: 'प्रोजेक्ट सिमुलेशन रास्ता', demoRoute: 'इस डायरेक्टरी अस्पताल का वास्तविक इनडोर नक्शा प्रोजेक्ट में नहीं है। नेविगेशन प्रक्रिया दिखाने के लिए स्पष्ट लेबल वाला SmartCare डेमो रास्ता दिखाया गया है।',
    why: 'ऐप ने यह रास्ता क्यों चुना?', algorithm: 'Dijkstra रास्ता आसान भाषा में',
    algorithmText: 'हर सत्यापित जगह एक नोड और हर गलियारा एक कनेक्शन है। Dijkstra कुल यात्रा समय की तुलना करके सबसे कम लागत वाला सत्यापित रास्ता चुनता है। सीढ़ी-रहित मोड सीढ़ियों वाले कनेक्शन हटा देता है।',
    scan: 'सत्यापित नोड देखें', compare: 'कुल लागत तुलना', exclude: 'सीढ़ियाँ हटाएँ', select: 'सबसे कम लागत चुनें',
    blue: 'नीला = चुना रास्ता', green: 'हरा = मंज़िल', numbers: 'नंबर = लिखे चरण',
    nodes: 'मैप किए नोड', connections: 'रास्ते के कनेक्शन', optimized: 'अनुकूलित', accessibleMode: 'सीढ़ी-रहित पहुँच', fastestMode: 'सबसे कम समय',
    readSteps: 'नंबर वाले निर्देश क्रम से पढ़ें। चित्र रास्ते का आकार और क्रम समझाता है; अस्पताल के संकेत और स्टाफ के निर्देश हमेशा प्राथमिक हैं।',
    floor: 'मंज़िल', sample: 'डेमो नक्शा', exact: 'सत्यापित कमरा', helpDesk: 'सहायता डेस्क विकल्प',
  },
} as const

const destinationTypes: LocationType[] = ['DOCTOR_ROOM', 'LAB', 'IMAGING', 'PHARMACY', 'EMERGENCY', 'REGISTRATION', 'RECEPTION', 'EXIT']

const iconFor: Record<LocationType, ComponentType<{ className?: string }>> = {
  ENTRANCE: DoorOpen, REGISTRATION: CircleParking, LIFT: Accessibility, STAIRS: Footprints,
  CORRIDOR: Navigation, RECEPTION: BadgeCheck, DOCTOR_ROOM: Stethoscope, LAB: FlaskConical,
  IMAGING: Cross, PHARMACY: Pill, EMERGENCY: AlertTriangle, EXIT: DoorOpen,
}

function locationName(location: HospitalLocation, language: Language) {
  return language === 'hi' ? location.nameHi : location.nameEn
}

const floorTranslations: Record<string, string> = {
  'Ground Floor': 'भूतल', 'Lower Ground Floor': 'निचला भूतल', '1st Floor': 'पहली मंज़िल',
  '2nd Floor': 'दूसरी मंज़िल', '3rd Floor': 'तीसरी मंज़िल',
}

function floorName(value: string, language: Language) {
  return language === 'hi' ? floorTranslations[value] ?? value : value
}

function prototypeDestination(booking: PrototypeBooking) {
  const value = `${booking.departmentName} ${booking.specialization}`.toLowerCase()
  if (value.includes('cardio')) return 'OPD_307'
  if (value.includes('neuro')) return 'OPD_228'
  if (value.includes('ortho')) return 'OPD_118'
  if (value.includes('paed') || value.includes('pedi')) return 'OPD_126'
  if (value.includes('gyn') || value.includes('obstetric')) return 'OPD_242'
  if (value.includes('ent')) return 'OPD_132'
  if (value.includes('derma')) return 'OPD_136'
  if (value.includes('onco')) return 'OPD_048'
  if (value.includes('ophthal') || value.includes('vision')) return 'OPD_056'
  return 'OPD_214'
}

function routePoints(route: NavigationRoute | null, map: HospitalMap | null) {
  if (!route || !map) return ''
  const byCode = new Map(map.locations.map((location) => [location.code, location]))
  return route.pathCodes.map((code) => byCode.get(code)).filter(Boolean)
    .map((location) => `${location!.mapX},${location!.mapY}`).join(' ')
}

export function NavigationPage() {
  const { checkpointCode } = useParams()
  const [searchParams] = useSearchParams()
  const appointmentId = searchParams.get('appointment') ?? ''
  const prototypeBookingId = searchParams.get('prototypeBooking') ?? ''
  const { session } = useAuth()
  const [language, setLanguage] = useState<Language>('en')
  const [hospitals, setHospitals] = useState<{ id: string; name: string }[]>([])
  const [hospitalId, setHospitalId] = useState('')
  const [hospitalMap, setHospitalMap] = useState<HospitalMap | null>(null)
  const [sourceCode, setSourceCode] = useState('')
  const [destinationCode, setDestinationCode] = useState('')
  const [appointment, setAppointment] = useState<AppointmentDestination | null>(null)
  const [prototypeBooking, setPrototypeBooking] = useState<PrototypeBooking | null>(null)
  const [notice, setNotice] = useState<{ en: string; hi: string; kind: 'fallback' | 'demo' } | null>(null)
  const [route, setRoute] = useState<NavigationRoute | null>(null)
  const [stepFree, setStepFree] = useState(true)
  const [loading, setLoading] = useState(true)
  const [routing, setRouting] = useState(false)
  const [error, setError] = useState('')
  const t = copy[language]

  useEffect(() => {
    let active = true
    async function initialise() {
      setLoading(true)
      setError('')
      try {
        const list = await getNavigationHospitals()
        if (!active) return
        setHospitals(list)
        const demo = list.find((hospital) => hospital.code === 'SC-DEMO') ?? list[0]
        if (appointmentId && session) {
          try {
            const result = await getAppointmentDestination(appointmentId)
            if (!active) return
            setAppointment(result)
            setDestinationCode(result.destination.code)
            setHospitalId(result.hospitalId)
            if (!result.exactRoomMatch) setNotice({ en: result.guidanceEn, hi: result.guidanceHi, kind: 'fallback' })
          } catch (requestError) {
            if (!active) return
            if (demo) {
              setHospitalId(demo.id)
              setDestinationCode('MAIN_REGISTRATION')
              setNotice({
                en: 'This appointment hospital has no verified room map. A safe demo route to the help desk is available instead.',
                hi: 'इस अपॉइंटमेंट अस्पताल में सत्यापित कमरे का नक्शा नहीं है। इसके बदले सहायता डेस्क तक सुरक्षित डेमो रास्ता उपलब्ध है।',
                kind: 'demo',
              })
            } else setError(messageFromError(requestError))
          }
        } else if (prototypeBookingId) {
          const stored = getPrototypeBookings().find((item) => item.id === prototypeBookingId) ?? null
          setPrototypeBooking(stored)
          if (demo) {
            setHospitalId(demo.id)
            setDestinationCode(stored ? prototypeDestination(stored) : 'MAIN_REGISTRATION')
            setNotice({ en: copy.en.demoRoute, hi: copy.hi.demoRoute, kind: 'demo' })
          }
        } else if (checkpointCode) {
          const checkpoint = await getCheckpoint(checkpointCode)
          if (!active) return
          setHospitalId(checkpoint.hospitalId)
          setSourceCode(checkpoint.publicCode)
        } else if (demo) {
          setHospitalId(demo.id)
        }
      } catch (requestError) {
        if (active) setError(messageFromError(requestError))
      } finally {
        if (active) setLoading(false)
      }
    }
    void initialise()
    return () => { active = false }
  }, [appointmentId, checkpointCode, prototypeBookingId, session])

  useEffect(() => {
    if (!hospitalId) return
    let active = true
    setLoading(true)
    setRoute(null)
    getHospitalMap(hospitalId).then((data) => {
      if (!active) return
      setHospitalMap(data)
      setSourceCode((current) => data.checkpoints.some((item) => item.publicCode === current)
        ? current : data.checkpoints[0]?.publicCode ?? '')
      setDestinationCode((current) => data.locations.some((item) => item.code === current)
        ? current : '')
      setError('')
    }).catch((requestError) => {
      if (active) { setHospitalMap(null); setError(messageFromError(requestError)) }
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [hospitalId])

  const destinations = useMemo(() => hospitalMap?.locations.filter((location) =>
    destinationTypes.includes(location.type)) ?? [], [hospitalMap])
  const source = hospitalMap?.checkpoints.find((item) => item.publicCode === sourceCode)
  const destination = hospitalMap?.locations.find((item) => item.code === destinationCode)

  const buildRoute = useCallback(async () => {
    if (!hospitalId || !sourceCode || !destinationCode) return
    setRouting(true)
    setError('')
    try {
      const requested = await getRoute(hospitalId, sourceCode, destinationCode, language, stepFree)
      if (!requested.available && hospitalMap) {
        const safeDestination = hospitalMap.locations.find((item) => item.type === 'RECEPTION')
          ?? hospitalMap.locations.find((item) => item.type === 'REGISTRATION')
        if (safeDestination && safeDestination.code !== destinationCode) {
          const fallback = await getRoute(hospitalId, sourceCode, safeDestination.code, language, stepFree)
          if (fallback.available) {
            setDestinationCode(safeDestination.code)
            setNotice({ en: copy.en.fallbackRoute, hi: copy.hi.fallbackRoute, kind: 'fallback' })
            setRoute(fallback)
          } else setRoute(requested)
        } else setRoute(requested)
      } else setRoute(requested)
      window.setTimeout(() => {
        const resultElement = document.getElementById('route-result')
        if (resultElement && typeof resultElement.scrollIntoView === 'function') {
          resultElement.scrollIntoView({ behavior: 'smooth', block: 'start' })
        }
      }, 50)
    } catch (requestError) {
      setError(messageFromError(requestError))
    } finally {
      setRouting(false)
    }
  }, [destinationCode, hospitalId, hospitalMap, language, sourceCode, stepFree])

  useEffect(() => {
    if (route) void buildRoute()
    // Language/accessibility changes intentionally refresh an already displayed route.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [language, stepFree])

  if (loading && !hospitalMap) return <div className="grid min-h-[65vh] place-items-center bg-slate-50"><div className="flex items-center gap-3 font-bold text-slate-500"><LoaderCircle className="size-6 animate-spin text-care-600" />{t.loading}</div></div>

  return (
    <div className="min-h-screen bg-[#f5f8fc]">
      <section className="overflow-hidden bg-ink-950 text-white">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 lg:grid-cols-[1fr_.72fr] lg:px-8 lg:py-14">
          <div className="max-w-3xl">
            <div className="inline-flex items-center gap-2 rounded-full border border-blue-300/20 bg-blue-300/10 px-3 py-2 text-xs font-black uppercase tracking-[.18em] text-care-200"><QrCode className="size-4" />{t.eyebrow}</div>
            <h1 className="mt-5 text-balance text-4xl font-black tracking-tight sm:text-5xl lg:text-6xl">{t.title}</h1>
            <p className="mt-4 max-w-2xl text-sm leading-7 text-blue-100/75 sm:text-base">{t.intro}</p>
          </div>
          <div className="flex flex-col justify-between gap-5 rounded-3xl border border-white/10 bg-white/7 p-5 backdrop-blur">
            <div className="flex items-center gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-emerald-400/15 text-emerald-300"><ShieldCheck className="size-6" /></span><div><p className="font-black">{t.safetyTitle}</p><p className="text-xs text-blue-100/60">{t.safetySub}</p></div></div>
            <div className="grid grid-cols-2 gap-2 rounded-2xl bg-white/8 p-1.5" aria-label="Language">
              {(['en', 'hi'] as Language[]).map((item) => <button key={item} onClick={() => setLanguage(item)} className={`flex min-h-11 items-center justify-center gap-2 rounded-xl text-sm font-black transition ${language === item ? 'bg-white text-ink-950' : 'text-blue-100 hover:bg-white/5'}`}><Languages className="size-4" />{item === 'en' ? 'English' : 'हिंदी'}</button>)}
            </div>
          </div>
        </div>
      </section>

      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-10">
        {error && <div role="alert" className="mb-6 flex gap-3 rounded-2xl border border-rose-200 bg-rose-50 p-4 text-sm font-semibold text-rose-800"><AlertTriangle className="size-5 shrink-0" />{error}</div>}
        {notice && <div role="status" className={`mb-6 flex gap-3 rounded-2xl border p-4 text-sm ${notice.kind === 'demo' ? 'border-violet-200 bg-violet-50 text-violet-950' : 'border-amber-200 bg-amber-50 text-amber-950'}`}><span className={`grid size-10 shrink-0 place-items-center rounded-xl text-white ${notice.kind === 'demo' ? 'bg-violet-600' : 'bg-amber-600'}`}>{notice.kind === 'demo' ? <Sparkles className="size-5" /> : <MapPin className="size-5" />}</span><div><p className="font-black">{notice.kind === 'demo' ? t.demoTitle : t.fallbackTitle}</p><p className="mt-1 leading-6 opacity-80">{language === 'hi' ? notice.hi : notice.en}</p></div></div>}

        {hospitals.length > 1 && !checkpointCode && !appointment && !prototypeBooking && <label className="mb-6 block max-w-xl text-sm font-black text-slate-700">{t.allHospitals}<select value={hospitalId} onChange={(event) => setHospitalId(event.target.value)} className="mt-2 h-14 w-full rounded-2xl border border-slate-300 bg-white px-4 text-base font-bold"><option value="">{t.choose}</option>{hospitals.map((hospital) => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}</select></label>}

        {appointment && <div className="mb-6 flex flex-col gap-4 rounded-3xl border border-emerald-200 bg-emerald-50 p-5 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-start gap-3"><span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-emerald-600 text-white"><Stethoscope className="size-6" /></span><div><div className="flex flex-wrap items-center gap-2"><p className="text-xs font-black uppercase tracking-wider text-emerald-700">{t.appointment}</p><span className="rounded-full bg-white px-2 py-1 text-[10px] font-black text-emerald-800">{appointment.exactRoomMatch ? t.exact : t.helpDesk}</span></div><p className="mt-1 text-lg font-black text-emerald-950">{appointment.doctorName} · {locationName(appointment.destination, language)}</p><p className="text-sm text-emerald-800/70">{appointment.hospitalName}</p></div></div><Link to="/dashboard" className="text-sm font-black text-emerald-800 hover:underline">{t.change}</Link></div>}
        {prototypeBooking && <div className="mb-6 flex flex-col gap-4 rounded-3xl border border-violet-200 bg-white p-5 sm:flex-row sm:items-center sm:justify-between"><div className="flex items-start gap-3"><span className="grid size-12 shrink-0 place-items-center rounded-2xl bg-violet-600 text-white"><Route className="size-6" /></span><div><p className="text-xs font-black uppercase tracking-wider text-violet-700">{t.sample}</p><p className="mt-1 text-lg font-black text-ink-950">{prototypeBooking.doctorName} · {prototypeBooking.departmentName}</p><p className="text-sm text-slate-500">{prototypeBooking.hospitalName}</p></div></div><Link to="/dashboard" className="text-sm font-black text-violet-800 hover:underline">{t.change}</Link></div>}
        {appointmentId && !session && <div className="mb-6 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm font-semibold text-amber-900"><Link to="/login" className="font-black underline">Sign in</Link> — {t.signIn}</div>}

        {!hospitalMap ? <div className="rounded-3xl border border-dashed border-slate-300 bg-white p-10 text-center"><MapIcon className="mx-auto size-10 text-slate-300" /><p className="mt-3 font-bold text-slate-600">{t.noMap}</p></div> : <>
          <div className="grid gap-5 lg:grid-cols-2">
            <section className="rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
              <div className="flex items-center gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-care-50 text-care-700"><LocateFixed className="size-6" /></span><div><p className="text-xs font-black uppercase tracking-[.16em] text-care-700">1 · {t.start}</p><h2 className="text-xl font-black text-ink-950">{t.source}</h2></div></div>
              <p className="mt-3 text-sm text-slate-500">{t.sourceHelp}</p>
              <div className="mt-5 grid max-h-[25rem] gap-3 overflow-y-auto pr-1 sm:grid-cols-2">{hospitalMap.checkpoints.map((checkpoint: Checkpoint) => <button key={checkpoint.publicCode} onClick={() => { setSourceCode(checkpoint.publicCode); setRoute(null) }} className={`min-h-24 rounded-2xl border p-4 text-left transition ${sourceCode === checkpoint.publicCode ? 'border-care-500 bg-care-50 shadow-sm' : 'border-slate-200 hover:border-care-300'}`}><span className="flex items-start justify-between gap-2"><QrCode className={`size-5 ${sourceCode === checkpoint.publicCode ? 'text-care-700' : 'text-slate-400'}`} />{sourceCode === checkpoint.publicCode && <Check className="size-5 text-care-700" />}</span><strong className="mt-3 block text-sm text-ink-950">{language === 'hi' ? checkpoint.labelHi : checkpoint.labelEn}</strong><span className="mt-1 block text-xs text-slate-500">{floorName(checkpoint.location.floorLabel, language)}</span></button>)}</div>
              {source && <div className="mt-4 flex items-center gap-2 rounded-xl bg-emerald-50 px-3 py-2 text-xs font-black text-emerald-800"><BadgeCheck className="size-4" />{t.verified}: {source.publicCode}</div>}
            </section>

            <section className="rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
              <div className="flex items-center gap-3"><span className="grid size-12 place-items-center rounded-2xl bg-violet-50 text-violet-700"><MapPin className="size-6" /></span><div><p className="text-xs font-black uppercase tracking-[.16em] text-violet-700">2 · {t.choose}</p><h2 className="text-xl font-black text-ink-950">{t.destination}</h2></div></div>
              <div className="mt-5 grid max-h-[25rem] gap-3 overflow-y-auto pr-1 sm:grid-cols-2">{destinations.map((location) => { const Icon = iconFor[location.type]; return <button key={location.code} onClick={() => { setDestinationCode(location.code); setRoute(null) }} className={`min-h-24 rounded-2xl border p-4 text-left transition ${destinationCode === location.code ? 'border-violet-500 bg-violet-50 shadow-sm' : 'border-slate-200 hover:border-violet-300'}`}><span className="flex items-start justify-between gap-2"><Icon className={`size-5 ${destinationCode === location.code ? 'text-violet-700' : 'text-slate-400'}`} />{destinationCode === location.code && <Check className="size-5 text-violet-700" />}</span><strong className="mt-3 block text-sm text-ink-950">{locationName(location, language)}</strong><span className="mt-1 block text-xs text-slate-500">{location.building} · {floorName(location.floorLabel, language)}</span></button>})}</div>
            </section>
          </div>

          <section className="mt-5 rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm sm:p-7">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center sm:justify-between">
              <button type="button" role="switch" aria-checked={stepFree} onClick={() => setStepFree((value) => !value)} className={`flex min-h-16 items-center gap-3 rounded-2xl border px-4 text-left transition ${stepFree ? 'border-emerald-300 bg-emerald-50' : 'border-slate-200 bg-white'}`}><span className={`grid size-10 place-items-center rounded-xl ${stepFree ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-500'}`}><Accessibility className="size-5" /></span><span><strong className="block text-sm text-ink-950">{t.accessible}</strong><span className="text-xs text-slate-500">{t.accessibleHelp}</span></span><span className={`ml-auto h-6 w-11 rounded-full p-1 transition ${stepFree ? 'bg-emerald-600' : 'bg-slate-300'}`}><span className={`block size-4 rounded-full bg-white transition ${stepFree ? 'translate-x-5' : ''}`} /></span></button>
              <button onClick={() => void buildRoute()} disabled={!sourceCode || !destinationCode || routing} className="inline-flex min-h-16 items-center justify-center gap-3 rounded-2xl bg-care-600 px-8 text-base font-black text-white shadow-lg shadow-blue-700/20 transition hover:bg-care-700 disabled:cursor-not-allowed disabled:bg-slate-300 disabled:shadow-none">{routing ? <LoaderCircle className="size-6 animate-spin" /> : <Navigation className="size-6" />}{t.guide}<ChevronRight className="size-5" /></button>
            </div>
          </section>
        </>}

        {route && <section id="route-result" className="scroll-mt-32 pt-8">
          {!route.available ? <div className="rounded-[2rem] border border-amber-200 bg-amber-50 p-7"><AlertTriangle className="size-8 text-amber-700" /><h2 className="mt-4 text-2xl font-black text-amber-950">{t.noRoute}</h2><p className="mt-2 text-sm text-amber-900">{route.message}</p></div> : <div className="overflow-hidden rounded-[2rem] border border-slate-200 bg-white shadow-soft">
            <div className="grid bg-care-700 text-white lg:grid-cols-[1fr_.72fr]">
              <div className="p-6 sm:p-9"><p className="text-xs font-black uppercase tracking-[.2em] text-care-200">{t.route}</p><h2 className="mt-3 text-3xl font-black">{locationName(route.source, language)} <ArrowDown className="mx-2 inline size-6" /> {locationName(route.destination, language)}</h2><div className="mt-6 flex flex-wrap gap-3"><span className="rounded-full bg-white/12 px-4 py-2 text-sm font-black">~{route.estimatedMinutes} {t.mins}</span><span className="rounded-full bg-white/12 px-4 py-2 text-sm font-black">{route.totalDistanceMeters} {t.metres}</span><span className="rounded-full bg-white/12 px-4 py-2 text-sm font-black">{route.steps.length} {t.steps}</span></div></div>
              <div className="border-t border-white/10 bg-ink-950/30 p-6 sm:p-9 lg:border-l lg:border-t-0"><div className="flex gap-3"><ShieldCheck className="mt-0.5 size-6 shrink-0 text-emerald-300" /><div><p className="font-black">{route.message}</p><p className="mt-2 text-xs leading-5 text-blue-100/70">{route.safetyNotice}</p></div></div></div>
            </div>
            <div className="border-b border-blue-100 bg-gradient-to-r from-blue-50 to-cyan-50 p-5 sm:p-7">
              <div className="flex items-start gap-3"><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-care-700 text-white"><Network className="size-5" /></span><div className="min-w-0 flex-1"><p className="text-xs font-black uppercase tracking-[.15em] text-care-700">{t.algorithm}</p><h3 className="mt-1 font-black text-ink-950">{t.why}</h3><p className="mt-2 max-w-4xl text-sm leading-6 text-slate-600">{t.algorithmText}</p>
                <div className="mt-5 grid gap-2 sm:grid-cols-4">{[t.scan, t.compare, t.exclude, t.select].map((label, index) => <div key={label} className={`flex items-center gap-2 rounded-xl border px-3 py-2.5 text-xs font-black ${index === 2 && !stepFree ? 'border-slate-200 bg-white/50 text-slate-400 line-through' : 'border-blue-100 bg-white text-ink-950'}`}><span className="grid size-6 shrink-0 place-items-center rounded-full bg-care-600 text-[10px] text-white">{index + 1}</span>{label}</div>)}</div>
                <div className="mt-4 grid gap-2 sm:grid-cols-3"><div className="rounded-xl bg-white p-3"><strong className="block text-xl text-care-700">{hospitalMap?.locations.length ?? 0}</strong><span className="text-[11px] font-bold text-slate-500">{t.nodes}</span></div><div className="rounded-xl bg-white p-3"><strong className="block text-xl text-violet-700">{Math.max(0, route.pathCodes.length - 1)}</strong><span className="text-[11px] font-bold text-slate-500">{t.connections}</span></div><div className="rounded-xl bg-white p-3"><strong className="block text-sm text-emerald-700">{stepFree ? t.accessibleMode : t.fastestMode}</strong><span className="text-[11px] font-bold text-slate-500">{t.optimized}</span></div></div>
                <div className="mt-3 flex flex-wrap gap-2 text-[11px] font-black"><span className="rounded-full bg-white px-3 py-1.5 text-care-800">{t.blue}</span><span className="rounded-full bg-white px-3 py-1.5 text-emerald-800">{t.green}</span><span className="rounded-full bg-white px-3 py-1.5 text-violet-800">{t.numbers}</span></div>
              </div></div>
            </div>
            <div className="grid lg:grid-cols-[.72fr_1fr]">
              <div className="border-b border-slate-200 bg-slate-50 p-5 sm:p-7 lg:border-b-0 lg:border-r">
                <p className="text-xs font-black uppercase tracking-wider text-slate-500">{t.mapLabel}</p>
                <div className="mt-4 aspect-square overflow-hidden rounded-3xl border border-slate-200 bg-white p-3" aria-label={t.mapLabel}>
                  <svg viewBox="0 0 100 100" className="h-full w-full" role="img">
                    <defs><pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse"><path d="M 10 0 L 0 0 0 10" fill="none" stroke="#e2e8f0" strokeWidth=".35" /></pattern></defs>
                    <rect width="100" height="100" fill="url(#grid)" rx="4" />
                    {hospitalMap?.locations.map((point) => <circle key={`map-${point.code}`} cx={point.mapX} cy={point.mapY} r="1.2" fill="#cbd5e1" opacity={route.pathCodes.includes(point.code) ? 0 : .85} />)}
                    <polyline points={routePoints(route, hospitalMap)} fill="none" stroke="#1769c2" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" strokeDasharray="4 2" />
                    {route.pathCodes.map((code, index) => { const point = hospitalMap?.locations.find((item) => item.code === code); if (!point) return null; return <g key={code}><circle cx={point.mapX} cy={point.mapY} r={index === 0 || index === route.pathCodes.length - 1 ? 4 : 2.6} fill={index === route.pathCodes.length - 1 ? '#059669' : '#1769c2'} stroke="white" strokeWidth="1.2" /><text x={point.mapX} y={point.mapY - 6} textAnchor="middle" fontSize="3.2" fontWeight="800" fill="#0b213f">{index === 0 ? t.start : index === route.pathCodes.length - 1 ? t.arrived : index}</text></g> })}
                  </svg>
                </div><p className="mt-3 text-xs leading-5 text-slate-500">{t.mapNote}</p><p className="mt-2 rounded-xl bg-white p-3 text-xs font-bold leading-5 text-slate-600">{t.readSteps}</p><div className="mt-3 flex flex-wrap items-center gap-1.5">{route.pathCodes.map((code, index) => <span key={code} className="contents"><span className={`rounded-lg px-2 py-1 font-mono text-[10px] font-black ${index === route.pathCodes.length - 1 ? 'bg-emerald-100 text-emerald-800' : 'bg-blue-100 text-care-800'}`}>{code}</span>{index < route.pathCodes.length - 1 && <ChevronRight className="size-3 text-slate-400" />}</span>)}</div>
              </div>
              <ol className="divide-y divide-slate-100">{route.steps.map((step) => <li key={`${step.order}-${step.toCode}`} className="group flex gap-4 p-5 transition hover:bg-blue-50/50 sm:p-7"><span className="grid size-11 shrink-0 place-items-center rounded-2xl bg-care-50 text-lg font-black text-care-700 ring-4 ring-white group-hover:bg-care-600 group-hover:text-white">{step.order}</span><div className="min-w-0"><p className="font-bold leading-6 text-ink-950">{step.instruction}</p><div className="mt-2 flex flex-wrap gap-2 text-[11px] font-bold text-slate-500"><span>{step.distanceMeters} {t.metres}</span>{step.floorTransition && <span className="rounded-full bg-violet-50 px-2 py-1 text-violet-700">{floorName(step.fromFloor, language)} → {floorName(step.toFloor, language)}</span>}{step.stepFree && <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-1 text-emerald-700"><Accessibility className="size-3" />{t.accessible}</span>}</div></div></li>)}</ol>
            </div>
            <div className="flex items-center gap-3 border-t border-emerald-200 bg-emerald-50 p-5 text-emerald-900"><MapPin className="size-6 shrink-0" /><div><p className="font-black">{t.arrived}: {destination && locationName(destination, language)}</p><p className="text-xs">{[destination?.building, destination?.floorLabel ? floorName(destination.floorLabel, language) : '', destination?.roomNumber].filter(Boolean).join(' · ')}</p></div></div>
          </div>}
        </section>}
      </div>
    </div>
  )
}
