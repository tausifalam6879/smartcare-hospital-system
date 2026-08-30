import { api } from './api'

export type AmbulanceStatus =
  | 'AVAILABLE' | 'ASSIGNED' | 'EN_ROUTE_TO_PATIENT' | 'PATIENT_PICKED_UP'
  | 'EN_ROUTE_TO_HOSPITAL' | 'ARRIVED' | 'OUT_OF_SERVICE'

export type AmbulanceRequestStatus =
  | 'REQUESTED' | 'ASSIGNED' | 'ACKNOWLEDGED' | 'EN_ROUTE_TO_PATIENT'
  | 'PATIENT_PICKED_UP' | 'EN_ROUTE_TO_HOSPITAL' | 'ARRIVED' | 'COMPLETED' | 'CANCELLED'

export type AmbulancePriority = 'EMERGENCY' | 'URGENT' | 'SCHEDULED'
export type TransportType = 'PATIENT_TRANSPORT' | 'BLOOD_TRANSPORT'

export type AmbulanceAvailability = {
  hospitalId: string
  hospitalName: string
  availableVehicles: number
  activeVehicles: number
  syntheticData: boolean
  checkedAt: string
}

export type Ambulance = {
  id: string
  hospitalId: string
  hospitalName: string
  registrationNumber: string
  callSign: string
  status: AmbulanceStatus
  crewLabel: string
  crewContact?: string
  currentArea?: string
  latitude?: number
  longitude?: number
  locationUpdatedAt?: string
  active: boolean
  synthetic: boolean
}

export type AmbulanceTimelineEvent = {
  fromStatus?: AmbulanceRequestStatus
  toStatus: AmbulanceRequestStatus
  actorLabel: string
  note?: string
  eventAt: string
}

export type AmbulanceRequest = {
  id: string
  patientNumber?: string
  hospitalId: string
  hospitalName: string
  hospitalAddress: string
  hospitalContactNumber: string
  transportType: TransportType
  priority: AmbulancePriority
  status: AmbulanceRequestStatus
  pickupAddress: string
  pickupLandmark?: string
  contactNumber: string
  assistanceNotes?: string
  ambulance?: Pick<Ambulance, 'id' | 'registrationNumber' | 'callSign' | 'status' | 'currentArea' | 'locationUpdatedAt' | 'synthetic'>
  statusUpdatedAt: string
  dispatchedAt?: string
  acknowledgedAt?: string
  completedAt?: string
  cancelledAt?: string
  cancellationReason?: string
  createdAt: string
  timeline: AmbulanceTimelineEvent[]
}

export type CreateAmbulanceRequestInput = {
  hospitalId: string
  transportType: TransportType
  priority: AmbulancePriority
  pickupAddress: string
  pickupLandmark?: string
  contactNumber: string
  assistanceNotes?: string
  idempotencyKey: string
}

export async function getAmbulanceAvailability(hospitalId: string) {
  const response = await api.get<AmbulanceAvailability>('/api/v1/ambulances/availability', { params: { hospitalId } })
  return response.data
}

export async function getMyAmbulanceRequests() {
  const response = await api.get<AmbulanceRequest[]>('/api/v1/ambulance-requests/mine')
  return response.data
}

export async function createAmbulanceRequest(input: CreateAmbulanceRequestInput) {
  const response = await api.post<AmbulanceRequest>('/api/v1/ambulance-requests', input)
  return response.data
}

export async function cancelAmbulanceRequest(requestId: string, reason: string) {
  const response = await api.post<AmbulanceRequest>(`/api/v1/ambulance-requests/${requestId}/cancel`, { reason })
  return response.data
}

export async function getAmbulanceFleet(hospitalId: string) {
  const response = await api.get<Ambulance[]>('/api/v1/ambulances', { params: { hospitalId } })
  return response.data
}

export async function getAmbulanceWorklist(hospitalId: string) {
  const response = await api.get<AmbulanceRequest[]>('/api/v1/ambulance-requests', { params: { hospitalId } })
  return response.data
}

export async function assignAmbulance(requestId: string, ambulanceId: string) {
  const response = await api.post<AmbulanceRequest>(`/api/v1/ambulance-requests/${requestId}/assign`, { ambulanceId })
  return response.data
}

export async function acknowledgeAmbulance(requestId: string) {
  const response = await api.post<AmbulanceRequest>(`/api/v1/ambulance-requests/${requestId}/acknowledge`)
  return response.data
}

export async function advanceAmbulanceRequest(requestId: string, status: AmbulanceRequestStatus, note?: string) {
  const response = await api.post<AmbulanceRequest>(`/api/v1/ambulance-requests/${requestId}/status`, { status, note })
  return response.data
}
