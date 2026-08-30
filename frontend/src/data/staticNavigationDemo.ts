import type {
  AppointmentDestination, Checkpoint, HospitalLocation, HospitalMap, HospitalSummary,
  LocationType, NavigationRoute,
} from '../services/navigation'

const hospital: HospitalSummary = {
  id: 'github-pages-demo-hospital',
  code: 'RMQ-DEMO',
  name: 'RaahMediQ Health Demo Care Centre',
  city: 'New Delhi',
}

function place(code: string, nameEn: string, nameHi: string, type: LocationType,
  building: string, floorLabel: string, mapX: number, mapY: number,
  roomNumber?: string): HospitalLocation {
  return { code, nameEn, nameHi, type, building, floorLabel, mapX, mapY, roomNumber }
}

const locations: HospitalLocation[] = [
  place('MAIN_ENTRANCE', 'Main entrance', 'मुख्य प्रवेश', 'ENTRANCE', 'Central Block', 'Ground Floor', 8, 82),
  place('MAIN_REGISTRATION', 'Main registration', 'मुख्य पंजीकरण', 'REGISTRATION', 'Central Block', 'Ground Floor', 24, 72),
  place('LIFT_A_G', 'Lift A · Ground floor', 'लिफ्ट A · भूतल', 'LIFT', 'Central Block', 'Ground Floor', 39, 69),
  place('LIFT_A_1', 'Lift A · 1st floor', 'लिफ्ट A · पहली मंज़िल', 'LIFT', 'Central Block', '1st Floor', 39, 54),
  place('LIFT_A_2', 'Lift A · 2nd floor', 'लिफ्ट A · दूसरी मंज़िल', 'LIFT', 'Aarogya Block', '2nd Floor', 39, 39),
  place('FIRST_FLOOR_RECEPTION', 'First-floor OPD help desk', 'पहली मंज़िल ओपीडी सहायता डेस्क', 'RECEPTION', 'Central Block', '1st Floor', 55, 54),
  place('OPD_118', 'Orthopaedics · OPD 118', 'हड्डी रोग · ओपीडी 118', 'DOCTOR_ROOM', 'Mobility Block', '1st Floor', 70, 48, 'OPD 118'),
  place('OPD_126', 'Paediatrics · OPD 126', 'बाल रोग · ओपीडी 126', 'DOCTOR_ROOM', 'Child Care Block', '1st Floor', 82, 53, 'OPD 126'),
  place('OPD_132', 'ENT · OPD 132', 'ईएनटी · ओपीडी 132', 'DOCTOR_ROOM', 'Speciality Block', '1st Floor', 72, 61, 'OPD 132'),
  place('OPD_136', 'Dermatology · OPD 136', 'त्वचा रोग · ओपीडी 136', 'DOCTOR_ROOM', 'Speciality Block', '1st Floor', 86, 64, 'OPD 136'),
  place('GENERAL_RECEPTION', 'General medicine reception', 'सामान्य चिकित्सा रिसेप्शन', 'RECEPTION', 'Aarogya Block', '2nd Floor', 59, 38),
  place('OPD_214', 'Internal medicine · OPD 214', 'आंतरिक चिकित्सा · ओपीडी 214', 'DOCTOR_ROOM', 'Aarogya Block', '2nd Floor', 80, 32, 'OPD 214'),
  place('NEURO_RECEPTION', 'Neurology reception', 'न्यूरोलॉजी रिसेप्शन', 'RECEPTION', 'Neuro Block', '2nd Floor', 67, 45),
  place('OPD_228', 'Neurology · OPD 228', 'न्यूरोलॉजी · ओपीडी 228', 'DOCTOR_ROOM', 'Neuro Block', '2nd Floor', 84, 43, 'OPD 228'),
  place('SAKHI_RECEPTION', "Women's health reception", 'महिला स्वास्थ्य रिसेप्शन', 'RECEPTION', 'Sakhi Block', '2nd Floor', 62, 29),
  place('OPD_242', "Women's health · OPD 242", 'महिला स्वास्थ्य · ओपीडी 242', 'DOCTOR_ROOM', 'Sakhi Block', '2nd Floor', 80, 24, 'OPD 242'),
  place('LAB_G', 'Sample collection lab', 'नमूना संग्रह लैब', 'LAB', 'Central Block', 'Ground Floor', 55, 59, 'LAB 08'),
  place('PHARMACY_G', '24-hour pharmacy', '24 घंटे की फार्मेसी', 'PHARMACY', 'Central Block', 'Ground Floor', 58, 84, 'P 01'),
  place('EMERGENCY_G', 'Emergency department', 'आपातकालीन विभाग', 'EMERGENCY', 'Central Block', 'Ground Floor', 82, 82, 'ER'),
  place('CANCER_RECEPTION', 'Cancer care reception', 'कैंसर देखभाल रिसेप्शन', 'RECEPTION', 'Cancer Care Block', 'Ground Floor', 48, 76),
  place('OPD_048', 'Oncology · OPD 048', 'कैंसर रोग · ओपीडी 048', 'DOCTOR_ROOM', 'Cancer Care Block', 'Ground Floor', 69, 77, 'OPD 048'),
  place('VISION_RECEPTION', 'Vision care reception', 'नेत्र देखभाल रिसेप्शन', 'RECEPTION', 'Vision Block', 'Ground Floor', 34, 81),
  place('OPD_056', 'Ophthalmology · OPD 056', 'नेत्र रोग · ओपीडी 056', 'DOCTOR_ROOM', 'Vision Block', 'Ground Floor', 45, 89, 'OPD 056'),
  place('LIFT_B_G', 'Lift B · Ground floor', 'लिफ्ट B · भूतल', 'LIFT', 'Central Block', 'Ground Floor', 64, 69),
  place('LIFT_B_3', 'Lift B · 3rd floor', 'लिफ्ट B · तीसरी मंज़िल', 'LIFT', 'Hriday Block', '3rd Floor', 64, 17),
  place('CARDIO_RECEPTION', 'Cardiology reception', 'हृदय रोग रिसेप्शन', 'RECEPTION', 'Hriday Block', '3rd Floor', 78, 17),
  place('OPD_307', 'Cardiology · OPD 307', 'हृदय रोग · ओपीडी 307', 'DOCTOR_ROOM', 'Hriday Block', '3rd Floor', 92, 17, 'OPD 307'),
]

const locationByCode = new Map(locations.map((location) => [location.code, location]))

function checkpoint(publicCode: string, locationCode: string, labelEn: string, labelHi: string): Checkpoint {
  return {
    hospitalId: hospital.id,
    hospitalName: hospital.name,
    publicCode,
    labelEn,
    labelHi,
    entryPath: `/navigate/${publicCode}`,
    location: locationByCode.get(locationCode)!,
  }
}

const checkpoints: Checkpoint[] = [
  checkpoint('RMQ-DEMO-ENTRANCE', 'MAIN_ENTRANCE', 'Main entrance QR', 'मुख्य प्रवेश QR'),
  checkpoint('RMQ-DEMO-REG', 'MAIN_REGISTRATION', 'Registration QR', 'पंजीकरण QR'),
  checkpoint('RMQ-DEMO-LIFT-A1', 'LIFT_A_1', 'Lift A · 1st floor QR', 'लिफ्ट A · पहली मंज़िल QR'),
  checkpoint('RMQ-DEMO-LIFT-A2', 'LIFT_A_2', 'Lift A · 2nd floor QR', 'लिफ्ट A · दूसरी मंज़िल QR'),
  checkpoint('RMQ-DEMO-LIFT-B3', 'LIFT_B_3', 'Lift B · 3rd floor QR', 'लिफ्ट B · तीसरी मंज़िल QR'),
  checkpoint('RMQ-DEMO-OPD-1', 'FIRST_FLOOR_RECEPTION', 'First-floor OPD help desk QR', 'पहली मंज़िल ओपीडी सहायता डेस्क QR'),
  checkpoint('RMQ-DEMO-OPD-2', 'GENERAL_RECEPTION', 'Second-floor OPD reception QR', 'दूसरी मंज़िल ओपीडी रिसेप्शन QR'),
]

type DemoEdge = {
  from: string
  to: string
  metres: number
  seconds: number
  stepFree: boolean
  en: string
  hi: string
}

const edges: DemoEdge[] = [
  { from: 'MAIN_ENTRANCE', to: 'MAIN_REGISTRATION', metres: 35, seconds: 35, stepFree: true, en: 'Go straight for 35 metres to the registration desk.', hi: '35 मीटर सीधे पंजीकरण डेस्क तक जाएँ।' },
  { from: 'MAIN_REGISTRATION', to: 'LIFT_A_G', metres: 28, seconds: 30, stepFree: true, en: 'Follow the blue line for 28 metres to Lift A.', hi: 'नीली लाइन पर 28 मीटर चलकर लिफ्ट A तक जाएँ।' },
  { from: 'LIFT_A_G', to: 'LIFT_A_1', metres: 6, seconds: 42, stepFree: true, en: 'Take Lift A to the 1st floor.', hi: 'लिफ्ट A से पहली मंज़िल पर जाएँ।' },
  { from: 'LIFT_A_G', to: 'LIFT_A_2', metres: 8, seconds: 55, stepFree: true, en: 'Take Lift A to the 2nd floor.', hi: 'लिफ्ट A से दूसरी मंज़िल पर जाएँ।' },
  { from: 'LIFT_A_1', to: 'FIRST_FLOOR_RECEPTION', metres: 18, seconds: 20, stepFree: true, en: 'Follow the amber OPD signs for 18 metres to the help desk.', hi: 'नारंगी ओपीडी संकेतों पर 18 मीटर सहायता डेस्क तक जाएँ।' },
  { from: 'FIRST_FLOOR_RECEPTION', to: 'OPD_118', metres: 22, seconds: 24, stepFree: true, en: 'Follow the mobility signs for 22 metres. OPD 118 is on the left.', hi: 'मोबिलिटी संकेतों पर 22 मीटर चलें। ओपीडी 118 बाईं ओर है।' },
  { from: 'FIRST_FLOOR_RECEPTION', to: 'OPD_126', metres: 30, seconds: 34, stepFree: true, en: 'Follow the yellow child-care signs for 30 metres to OPD 126.', hi: 'पीले बाल देखभाल संकेतों पर 30 मीटर चलकर ओपीडी 126 जाएँ।' },
  { from: 'FIRST_FLOOR_RECEPTION', to: 'OPD_132', metres: 25, seconds: 28, stepFree: true, en: 'Turn right at the purple sign and walk 25 metres to OPD 132.', hi: 'बैंगनी संकेत पर दाएँ मुड़ें और 25 मीटर चलकर ओपीडी 132 जाएँ।' },
  { from: 'OPD_132', to: 'OPD_136', metres: 16, seconds: 18, stepFree: true, en: 'Continue 16 metres along the purple corridor to OPD 136.', hi: 'बैंगनी गलियारे में 16 मीटर आगे ओपीडी 136 तक जाएँ।' },
  { from: 'LIFT_A_2', to: 'GENERAL_RECEPTION', metres: 22, seconds: 25, stepFree: true, en: 'Exit Lift A and follow the blue signs for 22 metres to reception.', hi: 'लिफ्ट A से निकलकर नीले संकेतों पर 22 मीटर रिसेप्शन तक जाएँ।' },
  { from: 'GENERAL_RECEPTION', to: 'OPD_214', metres: 30, seconds: 35, stepFree: true, en: 'Pass reception and continue 30 metres. OPD 214 is the second door on the left.', hi: 'रिसेप्शन पार करके 30 मीटर आगे जाएँ। ओपीडी 214 बाईं ओर दूसरा दरवाज़ा है।' },
  { from: 'GENERAL_RECEPTION', to: 'NEURO_RECEPTION', metres: 20, seconds: 23, stepFree: true, en: 'Follow the indigo signs for 20 metres to neurology reception.', hi: 'इंडिगो संकेतों पर 20 मीटर चलकर न्यूरोलॉजी रिसेप्शन जाएँ।' },
  { from: 'NEURO_RECEPTION', to: 'OPD_228', metres: 24, seconds: 27, stepFree: true, en: 'Continue 24 metres. OPD 228 is the second door on the right.', hi: '24 मीटर आगे जाएँ। ओपीडी 228 दाईं ओर दूसरा दरवाज़ा है।' },
  { from: 'GENERAL_RECEPTION', to: 'SAKHI_RECEPTION', metres: 18, seconds: 21, stepFree: true, en: "Follow the rose signs for 18 metres to women's health reception.", hi: 'गुलाबी संकेतों पर 18 मीटर चलकर महिला स्वास्थ्य रिसेप्शन जाएँ।' },
  { from: 'SAKHI_RECEPTION', to: 'OPD_242', metres: 26, seconds: 30, stepFree: true, en: 'Continue 26 metres to OPD 242 beside the counselling room.', hi: '26 मीटर आगे जाएँ। ओपीडी 242 परामर्श कक्ष के पास है।' },
  { from: 'MAIN_REGISTRATION', to: 'LAB_G', metres: 45, seconds: 48, stepFree: true, en: 'Follow the orange signs for 45 metres to the sample lab.', hi: 'नारंगी संकेतों पर 45 मीटर नमूना लैब तक जाएँ।' },
  { from: 'LAB_G', to: 'PHARMACY_G', metres: 30, seconds: 32, stepFree: true, en: 'Continue along the ground-floor corridor for 30 metres to the pharmacy.', hi: 'भूतल के गलियारे में 30 मीटर आगे फार्मेसी तक जाएँ।' },
  { from: 'PHARMACY_G', to: 'EMERGENCY_G', metres: 40, seconds: 42, stepFree: true, en: 'Follow the red emergency signs for 40 metres.', hi: 'लाल आपातकालीन संकेतों पर 40 मीटर चलें।' },
  { from: 'MAIN_REGISTRATION', to: 'CANCER_RECEPTION', metres: 32, seconds: 36, stepFree: true, en: 'Follow the crimson signs for 32 metres to cancer care reception.', hi: 'गहरे लाल संकेतों पर 32 मीटर चलकर कैंसर देखभाल रिसेप्शन जाएँ।' },
  { from: 'CANCER_RECEPTION', to: 'OPD_048', metres: 28, seconds: 32, stepFree: true, en: 'Continue 28 metres to OPD 048 after the counselling desk.', hi: '28 मीटर आगे परामर्श डेस्क के बाद ओपीडी 048 जाएँ।' },
  { from: 'MAIN_REGISTRATION', to: 'VISION_RECEPTION', metres: 20, seconds: 23, stepFree: true, en: 'Follow the sky-blue signs for 20 metres to vision reception.', hi: 'आसमानी नीले संकेतों पर 20 मीटर नेत्र रिसेप्शन तक जाएँ।' },
  { from: 'VISION_RECEPTION', to: 'OPD_056', metres: 18, seconds: 21, stepFree: true, en: 'Follow the eye symbols for 18 metres. OPD 056 is on the right.', hi: 'आँख के संकेतों पर 18 मीटर चलें। ओपीडी 056 दाईं ओर है।' },
  { from: 'MAIN_REGISTRATION', to: 'LIFT_B_G', metres: 55, seconds: 60, stepFree: true, en: 'Follow the green line for 55 metres to Lift B.', hi: 'हरी लाइन पर 55 मीटर चलकर लिफ्ट B तक जाएँ।' },
  { from: 'LIFT_B_G', to: 'LIFT_B_3', metres: 10, seconds: 65, stepFree: true, en: 'Take Lift B to the 3rd floor.', hi: 'लिफ्ट B से तीसरी मंज़िल पर जाएँ।' },
  { from: 'LIFT_B_3', to: 'CARDIO_RECEPTION', metres: 18, seconds: 20, stepFree: true, en: 'Follow the green heart signs for 18 metres to reception.', hi: 'हरे दिल के संकेतों पर 18 मीटर रिसेप्शन तक जाएँ।' },
  { from: 'CARDIO_RECEPTION', to: 'OPD_307', metres: 24, seconds: 28, stepFree: true, en: 'OPD 307 is 24 metres ahead, the third door on the right.', hi: 'ओपीडी 307 24 मीटर आगे दाईं ओर तीसरा दरवाज़ा है।' },
]

export const staticDemoHospitals = [hospital]
export const staticDemoMap: HospitalMap = {
  hospitalId: hospital.id,
  hospitalName: hospital.name,
  locations,
  checkpoints,
}

export function staticDemoCheckpoint(publicCode: string) {
  return checkpoints.find((item) => item.publicCode.toLowerCase() === publicCode.toLowerCase())
}

function edgeBetween(from: string, to: string) {
  return edges.find((edge) => (edge.from === from && edge.to === to) || (edge.from === to && edge.to === from))
}

export function staticDemoRoute(fromCheckpoint: string, requestedDestination: string,
  language: 'en' | 'hi', stepFree: boolean): NavigationRoute {
  const source = staticDemoCheckpoint(fromCheckpoint)?.location ?? locationByCode.get('MAIN_ENTRANCE')!
  const destination = locationByCode.get(requestedDestination) ?? locationByCode.get('MAIN_REGISTRATION')!
  const distances = new Map(locations.map((location) => [location.code, Number.POSITIVE_INFINITY]))
  const previous = new Map<string, string>()
  const remaining = new Set(locations.map((location) => location.code))
  distances.set(source.code, 0)

  while (remaining.size) {
    const current = [...remaining].reduce<string | undefined>((best, code) =>
      best === undefined || distances.get(code)! < distances.get(best)! ? code : best, undefined)
    if (!current || distances.get(current) === Number.POSITIVE_INFINITY) break
    remaining.delete(current)
    if (current === destination.code) break
    for (const edge of edges) {
      if (stepFree && !edge.stepFree) continue
      const neighbour = edge.from === current ? edge.to : edge.to === current ? edge.from : undefined
      if (!neighbour || !remaining.has(neighbour)) continue
      const candidate = distances.get(current)! + edge.seconds
      if (candidate < distances.get(neighbour)!) {
        distances.set(neighbour, candidate)
        previous.set(neighbour, current)
      }
    }
  }

  const pathCodes: string[] = []
  let cursor: string | undefined = destination.code
  while (cursor) {
    pathCodes.unshift(cursor)
    if (cursor === source.code) break
    cursor = previous.get(cursor)
  }
  const available = pathCodes[0] === source.code
  const safePath = available ? pathCodes : [source.code]
  const steps = safePath.slice(0, -1).map((fromCode, index) => {
    const toCode = safePath[index + 1]
    const edge = edgeBetween(fromCode, toCode)!
    const from = locationByCode.get(fromCode)!
    const to = locationByCode.get(toCode)!
    const forward = edge.from === fromCode
    const reverseInstruction = language === 'hi'
      ? `${from.nameHi} से ${to.nameHi} की ओर जाएँ।`
      : `Return from ${from.nameEn} towards ${to.nameEn}.`
    return {
      order: index + 1,
      instruction: forward ? (language === 'hi' ? edge.hi : edge.en) : reverseInstruction,
      distanceMeters: edge.metres,
      durationSeconds: edge.seconds,
      fromCode,
      toCode,
      fromFloor: from.floorLabel,
      toFloor: to.floorLabel,
      floorTransition: from.floorLabel !== to.floorLabel,
      stepFree: edge.stepFree,
    }
  })
  const totalDistanceMeters = steps.reduce((total, step) => total + step.distanceMeters, 0)
  const seconds = steps.reduce((total, step) => total + step.durationSeconds, 0)

  return {
    available,
    language,
    stepFreeRequested: stepFree,
    source,
    destination,
    totalDistanceMeters,
    estimatedMinutes: Math.max(1, Math.ceil(seconds / 60)),
    pathCodes: safePath,
    steps,
    message: available
      ? language === 'hi' ? 'सबसे कम लागत वाला सत्यापित डेमो रास्ता तैयार है।' : 'The lowest-cost verified demo route is ready.'
      : language === 'hi' ? 'कोई डेमो रास्ता उपलब्ध नहीं है।' : 'No demo route is available.',
    safetyNotice: language === 'hi'
      ? 'यह पोर्टफोलियो सिमुलेशन है। अस्पताल में संकेत और स्टाफ के निर्देश मानें।'
      : 'Portfolio simulation only. Follow hospital signs and staff instructions in a real facility.',
  }
}

export function staticDemoAppointmentDestination(appointmentId: string): AppointmentDestination {
  return {
    appointmentId,
    hospitalId: hospital.id,
    hospitalName: hospital.name,
    doctorName: 'Demo clinician',
    destination: locationByCode.get('OPD_214')!,
    exactRoomMatch: true,
    guidanceEn: 'A sample room is selected for the static portfolio demo.',
    guidanceHi: 'स्टैटिक पोर्टफोलियो डेमो के लिए एक नमूना कमरा चुना गया है।',
  }
}
