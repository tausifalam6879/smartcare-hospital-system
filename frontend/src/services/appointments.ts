import { api } from './api'

export type PaymentMethod = 'ONLINE' | 'CASH'
export type AppointmentStatus = 'WAITLISTED' | 'RESERVED_PENDING_PAYMENT' | 'CASH_PENDING' | 'CONFIRMED' | 'CHECKED_IN' | 'IN_CONSULTATION' | 'COMPLETED' | 'NO_SHOW' | 'CANCELLED' | 'EXPIRED'

export type Appointment = {
  id: string
  doctorId: string
  doctorName: string
  specialization: string
  hospitalId: string
  hospitalName: string
  departmentName: string
  serviceDate: string
  queuePosition?: number
  status: AppointmentStatus
  paymentMethod: PaymentMethod
  amount: number
  reservationExpiresAt?: string
  cashDeadlineAt?: string
  estimatedWaitMinutes: number
  building?: string
  floorLabel?: string
  roomNumber?: string
  checkedInAt?: string
  consultationStartedAt?: string
  completedAt?: string
  createdAt: string
}

export type Availability = {
  doctorId: string
  serviceDate: string
  bookable: boolean
  effectiveCapacity: number
  reservedCount: number
  positionsAvailable: number
  waitlistCount: number
  scheduleStart: string
  scheduleEnd: string
}

export async function getAvailability(doctorId: string, date: string) {
  return (await api.get<Availability>('/api/v1/appointments/availability', { params: { doctorId, date } })).data
}

export async function createAppointment(doctorId: string, serviceDate: string, paymentMethod: PaymentMethod) {
  return (await api.post<Appointment>('/api/v1/appointments', { doctorId, serviceDate, paymentMethod }, {
    headers: { 'Idempotency-Key': crypto.randomUUID() },
  })).data
}

export async function getMyAppointments() {
  return (await api.get<Appointment[]>('/api/v1/appointments/mine')).data
}

export async function getDoctorAppointments() {
  return (await api.get<Appointment[]>('/api/v1/appointments/doctor/mine')).data
}

export async function cancelAppointment(id: string, reason = 'Cancelled by patient') {
  return (await api.post<Appointment>(`/api/v1/appointments/${id}/cancel`, { reason })).data
}

export const activeAppointmentStatuses: AppointmentStatus[] = [
  'WAITLISTED',
  'RESERVED_PENDING_PAYMENT',
  'CASH_PENDING',
  'CONFIRMED',
  'CHECKED_IN',
  'IN_CONSULTATION',
]

export const cancellableAppointmentStatuses: AppointmentStatus[] = [
  'WAITLISTED', 'RESERVED_PENDING_PAYMENT', 'CASH_PENDING', 'CONFIRMED',
]

export const appointmentStatusLabel: Record<AppointmentStatus, string> = {
  WAITLISTED: 'On waitlist',
  RESERVED_PENDING_PAYMENT: 'Payment verification pending',
  CASH_PENDING: 'Cash confirmation pending',
  CONFIRMED: 'Confirmed',
  CHECKED_IN: 'Checked in',
  IN_CONSULTATION: 'In consultation',
  COMPLETED: 'Completed',
  NO_SHOW: 'No-show',
  CANCELLED: 'Cancelled',
  EXPIRED: 'Expired',
}
