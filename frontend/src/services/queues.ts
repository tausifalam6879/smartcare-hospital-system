import { api } from './api'
import type { AppointmentStatus } from './appointments'

export type CheckInChannel = 'MOBILE_WEB' | 'QR_CODE' | 'RECEPTION_DESK' | 'KIOSK'

export type CheckIn = {
  id: string
  appointmentId: string
  channel: CheckInChannel
  privacyToken: string
  queuePosition: number
  appointmentStatus: AppointmentStatus
  checkedInAt: string
}

export type PatientQueue = {
  appointmentId: string
  doctorId: string
  doctorName: string
  hospitalName: string
  serviceDate: string
  status: AppointmentStatus
  yourPosition?: number
  privacyToken?: string
  currentlyServingPosition?: number
  patientsAhead: number
  estimatedWaitMinutes: number
  estimateNotice: string
  building?: string
  floorLabel?: string
  roomNumber?: string
  lastUpdatedAt: string
}

export type PublicQueue = {
  doctorId: string
  doctorName: string
  hospitalId: string
  hospitalName: string
  serviceDate: string
  currentlyServingPosition?: number
  currentlyServingToken?: string
  checkedInWaiting: number
  expectedConsultationMinutes: number
  lastUpdatedAt: string
}

export async function checkInAppointment(appointmentId: string, channel: CheckInChannel = 'MOBILE_WEB') {
  return (await api.post<CheckIn>(`/api/v1/check-in/appointments/${appointmentId}`, { channel })).data
}

export async function serveNextPatient(doctorId: string, date: string) {
  return (await api.post<PublicQueue>(`/api/v1/queues/${doctorId}/serve-next`, undefined, {
    params: { date },
  })).data
}

export async function getPatientQueue(appointmentId: string) {
  return (await api.get<PatientQueue>(`/api/v1/queues/appointments/${appointmentId}`)).data
}
