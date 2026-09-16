import { describe, expect, it } from 'vitest'
import type { Appointment, AppointmentStatus } from '../services/appointments'
import {
  canOpenAppointmentQueue,
  canPatientCancelAppointment,
  findNextAppointment,
  isCurrentAppointment,
} from './appointmentLifecycle'

function appointment(serviceDate: string, status: AppointmentStatus, createdAt = `${serviceDate}T08:00:00Z`): Appointment {
  return {
    id: `${serviceDate}-${status}`,
    patientNumber: 'RVQ-1', doctorId: 'doctor-1', doctorName: 'Doctor', specialization: 'Medicine',
    hospitalId: 'hospital-1', hospitalName: 'Hospital', departmentName: 'Medicine', serviceDate,
    status, paymentMethod: 'CASH', amount: 500, estimatedWaitMinutes: 10, createdAt,
  }
}

describe('appointment lifecycle presentation', () => {
  const today = '2026-09-16'

  it('does not present a past confirmed appointment as active or cancellable', () => {
    const stale = appointment('2026-09-11', 'CONFIRMED')
    expect(isCurrentAppointment(stale, today)).toBe(false)
    expect(canOpenAppointmentQueue(stale, today)).toBe(false)
    expect(canPatientCancelAppointment(stale, today)).toBe(false)
  })

  it('keeps a live consultation accessible even if its stored date is stale', () => {
    const live = appointment('2026-09-15', 'IN_CONSULTATION')
    expect(isCurrentAppointment(live, today)).toBe(true)
    expect(canOpenAppointmentQueue(live, today)).toBe(true)
  })

  it('selects the nearest upcoming appointment regardless of API ordering', () => {
    const result = findNextAppointment([
      appointment('2026-09-20', 'CONFIRMED'),
      appointment('2026-09-11', 'CONFIRMED'),
      appointment('2026-09-17', 'CASH_PENDING'),
    ], today)
    expect(result?.serviceDate).toBe('2026-09-17')
  })
})
