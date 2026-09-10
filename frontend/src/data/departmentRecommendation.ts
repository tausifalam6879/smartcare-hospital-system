export type DepartmentRecommendation = {
  department: string
  reason: string
  matchedKeywords: string[]
  emergency: boolean
}

const rules = [
  { department: 'Cardiology', keywords: ['chest pain', 'heart', 'palpitation', 'seene me dard'], reason: 'heart or chest-related symptoms' },
  { department: 'Neurology', keywords: ['headache', 'migraine', 'seizure', 'numbness', 'chakkar'], reason: 'brain, nerve, or balance-related symptoms' },
  { department: 'Orthopaedics', keywords: ['bone', 'joint', 'fracture', 'back pain', 'knee pain', 'haddi', 'kamar dard'], reason: 'bone, joint, or movement-related symptoms' },
  { department: 'Paediatrics', keywords: ['child', 'baby', 'infant', 'bachcha'], reason: 'child-health concerns' },
  { department: 'Obstetrics & Gynaecology', keywords: ['pregnancy', 'period', 'menstrual', 'gynaecology', 'pregnant'], reason: "women's health or pregnancy-related concerns" },
  { department: 'ENT', keywords: ['ear', 'nose', 'throat', 'hearing', 'sinus', 'kaan', 'gala'], reason: 'ear, nose, or throat-related symptoms' },
  { department: 'Dermatology', keywords: ['skin', 'rash', 'itch', 'hair loss', 'acne', 'khujli'], reason: 'skin, hair, or nail-related symptoms' },
  { department: 'General Medicine', keywords: ['fever', 'cough', 'cold', 'weakness', 'vomiting', 'bukhar', 'khansi'], reason: 'general adult medical symptoms' },
] as const

const emergencyTerms = ['severe chest pain', 'difficulty breathing', 'unconscious', 'heavy bleeding', 'stroke', 'cannot breathe']

export function recommendDepartment(input: string): DepartmentRecommendation | null {
  const normalized = input.trim().toLowerCase()
  if (!normalized) return null
  const scored = rules.map((rule) => ({
    rule,
    matches: rule.keywords.filter((keyword) => normalized.includes(keyword)),
  })).filter(({ matches }) => matches.length > 0).sort((a, b) => b.matches.length - a.matches.length)
  const emergency = emergencyTerms.some((term) => normalized.includes(term))
  const best = scored[0]
  if (!best) return {
    department: 'General Medicine',
    reason: 'the symptoms do not match a specialist routing rule',
    matchedKeywords: [],
    emergency,
  }
  return { department: best.rule.department, reason: best.rule.reason, matchedKeywords: best.matches, emergency }
}
