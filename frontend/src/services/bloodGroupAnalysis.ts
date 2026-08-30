import { api } from './api'
import type { BloodGroup } from './bloodBank'

export type BloodGroupAnalysisStatus = 'SUBMITTED' | 'OBSERVATIONS_RECORDED' | 'LAB_VERIFIED' | 'REJECTED'
export type ModelInferenceStatus = 'NOT_CONFIGURED' | 'COMPLETED' | 'FAILED'

export type BloodGroupAnalysis = {
  id: string
  patientNumber: string
  hospitalId: string
  hospitalName: string
  originalFilename: string
  contentType: string
  sizeBytes: number
  status: BloodGroupAnalysisStatus
  modelInferenceStatus: ModelInferenceStatus
  modelSuggestedGroup?: BloodGroup
  modelConfidence?: number
  antiAReactive?: boolean
  antiBReactive?: boolean
  antiDReactive?: boolean
  preliminaryGroup?: BloodGroup
  verifiedGroup?: BloodGroup
  observationNote?: string
  observedAt?: string
  verifiedAt?: string
  rejectionReason?: string
  rejectedAt?: string
  createdAt: string
}

export async function submitBloodGroupImage(input: {
  hospitalId: string
  safetyAcknowledged: boolean
  file: File
}) {
  const form = new FormData()
  form.append('hospitalId', input.hospitalId)
  form.append('safetyAcknowledged', String(input.safetyAcknowledged))
  form.append('file', input.file)
  return (await api.post<BloodGroupAnalysis>('/api/v1/blood-group-analyses', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })).data
}

export async function getMyBloodGroupAnalyses() {
  return (await api.get<BloodGroupAnalysis[]>('/api/v1/blood-group-analyses/mine')).data
}

export async function getBloodGroupAnalysisWorklist(hospitalId: string, status?: BloodGroupAnalysisStatus) {
  return (await api.get<BloodGroupAnalysis[]>('/api/v1/blood-group-analyses/worklist', {
    params: { hospitalId, ...(status ? { status } : {}) },
  })).data
}

export async function recordBloodGroupObservations(id: string, input: {
  antiAReactive: boolean
  antiBReactive: boolean
  antiDReactive: boolean
  note?: string
}) {
  return (await api.post<BloodGroupAnalysis>(`/api/v1/blood-group-analyses/${id}/observations`, input)).data
}

export async function verifyBloodGroupAnalysis(id: string, confirmedGroup: BloodGroup) {
  return (await api.post<BloodGroupAnalysis>(`/api/v1/blood-group-analyses/${id}/verify`, {
    confirmedGroup,
  })).data
}

export async function rejectBloodGroupAnalysis(id: string, reason: string) {
  return (await api.post<BloodGroupAnalysis>(`/api/v1/blood-group-analyses/${id}/reject`, { reason })).data
}

export async function loadBloodGroupImage(id: string, contentType: string) {
  const response = await api.get<ArrayBuffer>(`/api/v1/blood-group-analyses/${id}/image`, {
    responseType: 'arraybuffer',
  })
  return URL.createObjectURL(new Blob([response.data], { type: contentType }))
}
