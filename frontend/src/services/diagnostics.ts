import { api } from './api'

export type DiagnosticModality = 'LAB' | 'MRI' | 'CT' | 'X_RAY'
export type DiagnosticOrderStatus = 'ORDERED' | 'SCHEDULED' | 'SAMPLE_COLLECTED' |
  'IN_PROGRESS' | 'RESULT_VERIFIED' | 'CANCELLED'
export type DiagnosticPriority = 'ROUTINE' | 'URGENT'
export type DiagnosticResultFlag = 'NORMAL' | 'ABNORMAL' | 'CRITICAL' | 'INDETERMINATE'

export type DiagnosticProcedure = {
  id: string
  hospitalId: string
  hospitalName: string
  code: string
  name: string
  modality: DiagnosticModality
  preparationInstructions?: string
  turnaroundHours: number
  dailyCapacity: number
  estimatedDurationMinutes: number
  fee: number
  building?: string
  floorLabel?: string
  roomNumber?: string
}

export type DiagnosticResultItem = {
  name: string
  value: string
  unit?: string
  referenceRange?: string
  flag: DiagnosticResultFlag
}

export type VerifiedDiagnosticResult = {
  id: string
  summary: string
  findings?: string
  impression?: string
  overallFlag: DiagnosticResultFlag
  verifiedBy: string
  verifiedAt: string
  items: DiagnosticResultItem[]
}

export type VerifyDiagnosticResultRequest = {
  summary: string
  findings?: string
  impression?: string
  overallFlag: DiagnosticResultFlag
  items: DiagnosticResultItem[]
}

export type DiagnosticOrder = {
  id: string
  appointmentId: string
  patientNumber: string
  procedureId: string
  procedureCode: string
  procedureName: string
  modality: DiagnosticModality
  hospitalId: string
  hospitalName: string
  orderedByDoctor: string
  status: DiagnosticOrderStatus
  priority: DiagnosticPriority
  clinicalNote?: string
  preparationInstructions?: string
  turnaroundHours: number
  fee: number
  scheduledDate?: string
  queuePosition?: number
  building?: string
  floorLabel?: string
  roomNumber?: string
  orderedAt: string
  scheduledAt?: string
  sampleCollectedAt?: string
  processingStartedAt?: string
  resultVerifiedAt?: string
  cancellationReason?: string
  result?: VerifiedDiagnosticResult
}

export async function getMyDiagnosticOrders() {
  return (await api.get<DiagnosticOrder[]>('/api/v1/diagnostics/orders/mine')).data
}

export async function getDiagnosticProcedures(hospitalId: string, modality?: DiagnosticModality) {
  return (await api.get<DiagnosticProcedure[]>('/api/v1/diagnostics/procedures', {
    params: { hospitalId, ...(modality ? { modality } : {}) },
  })).data
}

export async function createDiagnosticOrder(input: {
  appointmentId: string
  procedureId: string
  priority: DiagnosticPriority
  clinicalNote?: string
}) {
  return (await api.post<DiagnosticOrder>('/api/v1/diagnostics/orders', input)).data
}

export async function scheduleDiagnosticOrder(orderId: string, serviceDate: string) {
  return (await api.post<DiagnosticOrder>(`/api/v1/diagnostics/orders/${orderId}/schedule`, { serviceDate })).data
}

export async function cancelDiagnosticOrder(orderId: string) {
  return (await api.post<DiagnosticOrder>(`/api/v1/diagnostics/orders/${orderId}/cancel`, {
    reason: 'Cancelled by patient',
  })).data
}

export async function verifyDiagnosticResult(orderId: string, request: VerifyDiagnosticResultRequest) {
  return (await api.post<DiagnosticOrder>(`/api/v1/diagnostics/orders/${orderId}/verify-result`, request)).data
}
