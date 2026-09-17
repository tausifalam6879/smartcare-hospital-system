import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getMyRecoveryCases, getOperationsDashboard } from '../services/operations'
import { OperationsPage } from './OperationsPage'

const authState = vi.hoisted(() => ({ roles: ['PATIENT'] as string[] }))

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ session: { user: { roles: authState.roles } } }),
}))

vi.mock('../services/api', () => ({
  api: { get: vi.fn((url: string) => Promise.resolve({
    data: url === '/api/v1/hospitals'
      ? [{ id: 'hospital-1', name: 'City General Hospital', city: 'Delhi' }]
      : { content: [] },
  })) },
  messageFromError: () => 'Request failed',
}))

vi.mock('../services/operations', () => ({
  getMyRecoveryCases: vi.fn(), resolveRecovery: vi.fn(), getOperationsDashboard: vi.fn(),
  updateDoctorDayStatus: vi.fn(), markAppointmentNoShow: vi.fn(),
}))

describe('OperationsPage', () => {
  afterEach(cleanup)

  beforeEach(() => {
    authState.roles = ['PATIENT']
    vi.mocked(getMyRecoveryCases).mockResolvedValue([{
      id: 'recovery-1', appointmentId: 'appointment-1', status: 'AWAITING_PATIENT_CHOICE',
      hospitalId: 'hospital-1', hospitalName: 'City General Hospital', originalDoctorId: 'doctor-1',
      departmentId: 'department-1', departmentName: 'General Medicine',
      originalDoctorName: 'Dr. Care', originalDate: '2026-08-24', originalQueuePosition: 4,
      interruptionReason: 'Doctor assigned to an emergency procedure.',
      createdAt: '2026-08-22T10:00:00Z',
    }])
  })

  it('keeps recovery patient-controlled and labels refund as review', async () => {
    render(<MemoryRouter><OperationsPage /></MemoryRouter>)

    expect(await screen.findByText(/will not move your doctor or date without your approval/i)).toBeInTheDocument()
    expect(screen.getByText(/OPD 4/i)).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText(/Choose what happens next/i), {
      target: { value: 'REFUND_REVIEW' },
    })
    expect(screen.getByText(/does not claim that money has already been refunded/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Confirm my choice/i })).toBeEnabled()
  })

  it('sends doctors to consultations instead of patient-only diagnostic pages', async () => {
    authState.roles = ['DOCTOR']
    vi.mocked(getMyRecoveryCases).mockResolvedValue([])
    vi.mocked(getOperationsDashboard).mockResolvedValue({
      hospitalId: 'hospital-1', hospitalName: 'City General Hospital', serviceDate: '2026-09-17',
      totalAppointments: 0, checkedIn: 0, inConsultation: 0, waitlisted: 0, noShows: 0,
      confirmed: 0, completed: 0,
      pendingPayments: 0, diagnosticLoad: 0, bloodInventoryAlerts: 0, ambulancesAvailable: 0,
      ambulancesOutOfService: 0, globalNotificationFailures: 0, activeDoctors: 0,
      delayedOrUnavailableDoctors: 0, averageRecordedWaitMinutes: 0, doctorStatuses: [], queue: [],
    })

    render(<MemoryRouter><OperationsPage /></MemoryRouter>)

    expect(await screen.findByRole('link', { name: /Doctor consultations/i })).toHaveAttribute('href', '/doctor/consultations')
    expect(screen.queryByRole('link', { name: /^Diagnostics$/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /^Blood support$/i })).not.toBeInTheDocument()
  })
})
