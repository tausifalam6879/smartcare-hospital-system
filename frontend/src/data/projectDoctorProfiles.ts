import type { IndiaFacility } from './indiaFacilities'

export type ProjectDoctor = {
  name: string
  department: string
  specialty: string
  fee: number
  availability: string
  note?: string
}

const doctorNames = [
  'Dr. Aarya Mehta', 'Dr. Rohan Kumar', 'Dr. Meera Iyer', 'Dr. Sana Ahmed',
  'Dr. Vivek Das', 'Dr. Isha Kapoor', 'Dr. Aditya Sen', 'Dr. Nidhi Verma',
]

const careProfiles = [
  ['General Medicine', 'Internal Medicine', 700, 'Mon–Sat · Morning OPD'],
  ['Cardiology', 'Heart & vascular care', 950, 'Mon–Fri · Afternoon OPD'],
  ['Neurology', 'Brain and nerve care', 1100, 'Tue–Sat · Morning OPD'],
  ['Orthopaedics', 'Bone and joint care', 850, 'Mon–Sat · Morning OPD'],
  ['Paediatrics', 'Child health', 650, 'Mon–Sat · Day OPD'],
  ['Obstetrics & Gynaecology', "Women's health", 900, 'Mon–Fri · Day OPD'],
  ['ENT', 'Ear, nose and throat', 700, 'Mon–Sat · Morning OPD'],
  ['Dermatology', 'Skin, hair and nail care', 750, 'Tue–Sun · Afternoon OPD'],
] as const

export function projectDoctors(facility: IndiaFacility): ProjectDoctor[] {
  const seed = [...`${facility.name}${facility.state}`]
    .reduce((total, character) => total + character.charCodeAt(0), 0)
  const profiles: ProjectDoctor[] = Array.from({ length: 5 }, (_, index) => {
    const profile = careProfiles[(seed + index) % careProfiles.length]
    return {
      name: doctorNames[(seed + index * 3) % doctorNames.length],
      department: profile[0],
      specialty: profile[1],
      fee: profile[2],
      availability: profile[3],
      note: 'Synthetic doctor profile for RaahMediQ prototype testing.',
    }
  })
  if (facility.name === 'Raj Hospitals') profiles.unshift({
    name: 'Dr. Prakash Chandra',
    department: 'Neurosciences',
    specialty: 'Neuro & Spine Surgery',
    fee: 2000,
    availability: 'Prototype assumption · confirm in a real deployment',
    note: 'User-provided prototype profile: 14-day follow-up and night OT coordination are demo assumptions, not a live provider listing.',
  })
  return profiles
}
