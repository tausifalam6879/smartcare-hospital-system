import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getMyRecoveryCases } from '../services/operations'
import { OperationsPage } from './OperationsPage'

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ session: { user: { roles: ['PATIENT'] } } }),
}))

vi.mock('../services/operations', () => ({
  getMyRecoveryCases: vi.fn(), resolveRecovery: vi.fn(), getOperationsDashboard: vi.fn(),
  updateDoctorDayStatus: vi.fn(), markAppointmentNoShow: vi.fn(),
}))

describe('OperationsPage', () => {
  beforeEach(() => {
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
})
