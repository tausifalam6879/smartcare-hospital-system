import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { DoctorConsultationPage } from './DoctorConsultationPage'
import { getDoctorAppointments } from '../services/appointments'
import { localDateString } from '../utils/appointmentLifecycle'
import type { Appointment } from '../services/appointments'

vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ session: { user: { roles: ['DOCTOR'] } } }) }))
vi.mock('../services/appointments', async importOriginal => ({ ...await importOriginal<object>(), getDoctorAppointments: vi.fn() }))
vi.mock('../services/diagnostics', () => ({ getDiagnosticProcedures: vi.fn().mockResolvedValue([]), createDiagnosticOrder: vi.fn() }))
afterEach(cleanup)

it('preserves a draft on same-patient refresh but clears it when the active patient changes', async () => {
  const first: Appointment = { id: 'first', patientNumber: 'PAT-ONE', doctorId: 'doctor', doctorName: 'Doctor', specialization: 'Medicine', hospitalId: 'hospital', hospitalName: 'Hospital', departmentName: 'Medicine', serviceDate: localDateString(), queuePosition: 1, status: 'IN_CONSULTATION', paymentMethod: 'CASH', amount: 500, estimatedWaitMinutes: 0, createdAt: '' }
  vi.mocked(getDoctorAppointments).mockResolvedValue([first])
  render(<MemoryRouter><DoctorConsultationPage /></MemoryRouter>)
  await waitFor(() => expect(screen.getByLabelText('Current consultation')).toHaveValue('first'))
  fireEvent.change(screen.getByLabelText('Symptoms'), { target: { value: 'First patient draft' } })
  fireEvent.change(screen.getByPlaceholderText('Substance or medicine'), { target: { value: 'First patient allergy' } })
  fireEvent.focus(window)
  await waitFor(() => expect(getDoctorAppointments).toHaveBeenCalledTimes(2))
  expect(screen.getByLabelText('Symptoms')).toHaveValue('First patient draft')
  vi.mocked(getDoctorAppointments).mockResolvedValue([{ ...first, id: 'second', patientNumber: 'PAT-TWO', queuePosition: 2 }])
  fireEvent.focus(window)
  await waitFor(() => expect(screen.getByLabelText('Current consultation')).toHaveValue('second'))
  expect(screen.getByLabelText('Symptoms')).toHaveValue('')
  expect(screen.getByPlaceholderText('Substance or medicine')).toHaveValue('')
})
