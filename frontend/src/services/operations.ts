import { api } from './api'
import type { AppointmentStatus, PaymentMethod } from './appointments'

export type DoctorDayStatus = 'ON_TIME' | 'DELAYED_30' | 'DELAYED_60' |
  'EMERGENCY_INTERRUPTION' | 'TEMPORARILY_UNAVAILABLE' | 'CANCELLED_FOR_DAY'
export type RecoveryChoice = 'RESCHEDULE_SAME_DOCTOR' | 'MOVE_TO_ELIGIBLE_DOCTOR' |
  'PRIORITY_FUTURE_QUEUE' | 'REFUND_REVIEW'
export type RecoveryStatus = 'AWAITING_PATIENT_CHOICE' | 'RESCHEDULED' | 'REFUND_REVIEW_REQUIRED'

export type DoctorOperationalStatus = {
  id: string
  doctorId: string
  doctorName: string
  hospitalId: string
  serviceDate: string
  status: DoctorDayStatus
  delayMinutes: number
  reason?: string
  updatedAt: string
}

export type RecoveryCase = {
  id: string
  appointmentId: string
  status: RecoveryStatus
  patientChoice?: RecoveryChoice
  hospitalId: string
  hospitalName: string
  departmentId: string
  departmentName: string
  originalDoctorId: string
  originalDoctorName: string
  originalDate: string
  originalQueuePosition?: number
  interruptionReason?: string
  targetDoctorId?: string
  targetDoctorName?: string
  targetDate?: string
  decidedAt?: string
  createdAt: string
}

export type OperationsQueueRow = {
  appointmentId: string
  patientNumber: string
  doctorId: string
  doctorName: string
  queuePosition?: number
  status: AppointmentStatus
  paymentMethod: PaymentMethod
}

export type OperationsDashboard = {
  hospitalId: string
  hospitalName: string
  serviceDate: string
  totalAppointments: number
  confirmed: number
  checkedIn: number
  inConsultation: number
  completed: number
  waitlisted: number
  noShows: number
  pendingPayments: number
  activeDoctors: number
  delayedOrUnavailableDoctors: number
  diagnosticLoad: number
  bloodInventoryAlerts: number
  ambulancesAvailable: number
  ambulancesOutOfService: number
  globalNotificationFailures?: number
  averageRecordedWaitMinutes: number
  doctorStatuses: DoctorOperationalStatus[]
  queue: OperationsQueueRow[]
}

export async function getOperationsDashboard(hospitalId: string, date: string) {
  return (await api.get<OperationsDashboard>('/api/v1/operations/dashboard', {
    params: { hospitalId, date },
  })).data
}

export async function updateDoctorDayStatus(input: {
  doctorId: string
  serviceDate: string
  status: DoctorDayStatus
  reason?: string
}) {
  return (await api.post<DoctorOperationalStatus>('/api/v1/operations/doctor-status', input)).data
}

export async function markAppointmentNoShow(appointmentId: string) {
  await api.post(`/api/v1/operations/appointments/${appointmentId}/no-show`)
}

export async function confirmCashAppointment(appointmentId: string) {
  await api.post(`/api/v1/appointments/${appointmentId}/cash-confirmation`)
}

export async function staffCheckInAppointment(appointmentId: string) {
  await api.post(`/api/v1/check-in/appointments/${appointmentId}`, { channel: 'RECEPTION_DESK' })
}

export async function getMyRecoveryCases() {
  return (await api.get<RecoveryCase[]>('/api/v1/operations/recovery/mine')).data
}

export async function resolveRecovery(caseId: string, input: {
  choice: RecoveryChoice
  targetDoctorId?: string
  targetDate?: string
}) {
  return (await api.post<RecoveryCase>(`/api/v1/operations/recovery/${caseId}/decision`, input)).data
}
