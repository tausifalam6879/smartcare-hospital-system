export type AgglutinationResult = {
  agglutinationDetected: boolean
  agglutinationProbability: number
  confidence: number
  manualReviewRequired: boolean
  modelName: string
  modelVersion: string
  safetyNotice: string
}

export type BloodGroupPanelResult = {
  antiA: AgglutinationResult
  antiB: AgglutinationResult
  antiD: AgglutinationResult
  bloodGroup: string | null
  interpretationStatus: string
  explanation: string
  safetyNotice: string
}

export type SavedBloodReactionPanel = {
  id: string
  antiAFilename: string
  antiBFilename: string
  antiDFilename: string
  antiAProbability: number
  antiBProbability: number
  antiDProbability: number
  antiAConfidence: number
  antiBConfidence: number
  antiDConfidence: number
  modelName: string
  modelVersion: string
  suggestedGroup?: string
  patientNumber?: string
  status: 'PENDING_CLINICIAN_VERIFICATION' | 'MANUAL_REVIEW_REQUIRED' | 'CLINICIAN_VERIFIED' | 'REJECTED'
  explanation: string
  createdAt: string
  confirmedGroup?: string
  reviewedAt?: string
  reviewNote?: string
  rejectionReason?: string
}

const mlApiUrl = (import.meta.env.VITE_ML_API_URL || 'http://127.0.0.1:8001').replace(/\/$/, '')

export async function analyzeAboPanel(input: {
  antiAFile: File
  antiBFile: File
  antiDFile: File
}): Promise<BloodGroupPanelResult> {
  const body = new FormData()
  body.append('antiAFile', input.antiAFile)
  body.append('antiBFile', input.antiBFile)
  body.append('antiDFile', input.antiDFile)

  let response: Response
  try {
    response = await fetch(`${mlApiUrl}/analyze-abo-panel`, { method: 'POST', body })
  } catch {
    throw new Error('Local ML service is unavailable. Start it on http://127.0.0.1:8001 and retry.')
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => null) as { detail?: string } | null
    throw new Error(payload?.detail || 'The reaction-well panel could not be analyzed.')
  }

  return response.json() as Promise<BloodGroupPanelResult>
}

export async function saveAboPanel(input: { antiAFile: File; antiBFile: File; antiDFile: File }) {
  const body = new FormData()
  body.append('antiAFile', input.antiAFile)
  body.append('antiBFile', input.antiBFile)
  body.append('antiDFile', input.antiDFile)
  return (await api.post<SavedBloodReactionPanel>('/api/v1/blood-reaction-panels', body, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })).data
}

export async function getMySavedAboPanels() {
  return (await api.get<SavedBloodReactionPanel[]>('/api/v1/blood-reaction-panels/mine')).data
}

export async function getBloodReactionPanelWorklist() {
  return (await api.get<SavedBloodReactionPanel[]>('/api/v1/blood-reaction-panels/worklist')).data
}

export async function verifyBloodReactionPanel(id: string, confirmedGroup: string, note: string) {
  return (await api.post<SavedBloodReactionPanel>(`/api/v1/blood-reaction-panels/${id}/verify`, { confirmedGroup, note })).data
}

export async function rejectBloodReactionPanel(id: string, reason: string) {
  return (await api.post<SavedBloodReactionPanel>(`/api/v1/blood-reaction-panels/${id}/reject`, { reason })).data
}
import { api } from './api'
