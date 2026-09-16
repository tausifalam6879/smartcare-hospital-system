import {
  activeAppointmentStatuses,
  cancellableAppointmentStatuses,
  type Appointment,
} from '../services/appointments'

const liveStatuses = ['CHECKED_IN', 'IN_CONSULTATION'] as const
const queueStatuses = ['CONFIRMED', ...liveStatuses] as const

export function localDateString(date = new Date()) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function isCurrentAppointment(appointment: Appointment, today = localDateString()) {
  return activeAppointmentStatuses.includes(appointment.status)
    && (appointment.serviceDate >= today || liveStatuses.includes(appointment.status as typeof liveStatuses[number]))
}

export function canOpenAppointmentQueue(appointment: Appointment, today = localDateString()) {
  return queueStatuses.includes(appointment.status as typeof queueStatuses[number])
    && (appointment.serviceDate >= today || liveStatuses.includes(appointment.status as typeof liveStatuses[number]))
}

export function canPatientCancelAppointment(appointment: Appointment, today = localDateString()) {
  return cancellableAppointmentStatuses.includes(appointment.status) && appointment.serviceDate >= today
}

export function findNextAppointment(appointments: Appointment[], today = localDateString()) {
  return appointments
    .filter((appointment) => isCurrentAppointment(appointment, today))
    .sort((left, right) => left.serviceDate.localeCompare(right.serviceDate)
      || left.createdAt.localeCompare(right.createdAt))[0]
}
