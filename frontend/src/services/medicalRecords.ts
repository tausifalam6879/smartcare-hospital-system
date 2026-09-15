import { api } from './api'

export type DocumentType = 'LAB_REPORT' | 'MRI_REPORT' | 'CT_REPORT' | 'X_RAY_REPORT' |
  'PRESCRIPTION' | 'DISCHARGE_SUMMARY' | 'REFERRAL' | 'OTHER'
export type AllergySeverity = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL'

export type PrescriptionItem = {
  medicineName: string
  dosage: string
  frequency: string
  duration: string
  route?: string
  instructions?: string
}

export type Prescription = {
  id: string
  generalInstructions?: string
  prescribedAt: string
  medicines: PrescriptionItem[]
}

export type ClinicalVisit = {
  id: string
  appointmentId: string
  visitDate: string
  hospitalName: string
  doctorName: string
  specialization: string
  symptoms?: string
  diagnosis: string
  doctorNotes?: string
  dischargeSummary?: string
  followUpRecommendation?: string
  finalizedAt: string
  prescription?: Prescription
}

export type Allergy = {
  id: string
  substance: string
  reaction?: string
  severity: AllergySeverity
  status: 'ACTIVE' | 'RESOLVED'
  recordedByDoctor: string
  recordedAt: string
}

export type MedicalDocument = {
  id: string
  hospitalId: string
  hospitalName: string
  documentType: DocumentType
  originalFilename: string
  contentType: string
  sizeBytes: number
  documentDate: string
  description?: string
  verificationStatus: 'PATIENT_UPLOADED' | 'CLINICIAN_VERIFIED'
  uploadedAt: string
  contentPath: string
  assistantReadiness: 'READY_FOR_TEXT_CHECK' | 'INDEXED' | 'OCR_REQUIRED' | 'NO_TEXT' | 'FAILED'
}

export type MedicalRecord = {
  patientNumber: string
  generatedAt: string
  visits: ClinicalVisit[]
  allergies: Allergy[]
  documents: MedicalDocument[]
}

export async function getMyMedicalRecord() {
  return (await api.get<MedicalRecord>('/api/v1/medical-records/mine')).data
}

export type VisitAllergyInput = { substance: string; reaction?: string; severity: AllergySeverity }
export type FinalizeClinicalVisitInput = {
  appointmentId: string
  diagnosis: string
  symptoms?: string
  doctorNotes?: string
  dischargeSummary?: string
  followUpRecommendation?: string
  prescriptionInstructions?: string
  medicines: PrescriptionItem[]
  allergies: VisitAllergyInput[]
  followUpDate?: string
  medicationReminderEnabled: boolean
}

export async function finalizeClinicalVisit(input: FinalizeClinicalVisitInput) {
  return (await api.post<ClinicalVisit>('/api/v1/medical-records/visits', input)).data
}

export async function uploadMedicalDocument(input: {
  hospitalId: string
  documentType: DocumentType
  documentDate: string
  description: string
  file: File
}) {
  const form = new FormData()
  form.append('hospitalId', input.hospitalId)
  form.append('documentType', input.documentType)
  form.append('documentDate', input.documentDate)
  if (input.description.trim()) form.append('description', input.description.trim())
  form.append('file', input.file)
  return (await api.post<MedicalDocument>('/api/v1/medical-records/documents', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })).data
}

export async function downloadMedicalDocument(document: MedicalDocument) {
  const response = await api.get<ArrayBuffer>(document.contentPath, { responseType: 'arraybuffer' })
  const url = URL.createObjectURL(new Blob([response.data], { type: document.contentType }))
  const anchor = window.document.createElement('a')
  anchor.href = url
  anchor.download = document.originalFilename
  anchor.click()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
