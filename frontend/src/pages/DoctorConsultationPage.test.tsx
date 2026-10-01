import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { DoctorConsultationPage } from './DoctorConsultationPage'
import { LanguageProvider } from '../context/LanguageContext'
import { getDoctorAppointments } from '../services/appointments'
import { localDateString } from '../utils/appointmentLifecycle'
import type { Appointment } from '../services/appointments'
import { getDiagnosticProcedures, type DiagnosticProcedure } from '../services/diagnostics'
import { finalizeClinicalVisit } from '../services/medicalRecords'

vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ session: { user: { roles: ['DOCTOR'] } } }) }))
vi.mock('../services/appointments', async importOriginal => ({ ...await importOriginal<object>(), getDoctorAppointments: vi.fn() }))
vi.mock('../services/diagnostics', () => ({ getDiagnosticProcedures: vi.fn().mockResolvedValue([]), createDiagnosticOrder: vi.fn() }))
vi.mock('../services/medicalRecords', () => ({ finalizeClinicalVisit: vi.fn().mockResolvedValue({}) }))
afterEach(() => { cleanup(); vi.clearAllMocks(); localStorage.removeItem('smartcare-language') })

it('submits the displayed native follow-up date even before its change event commits', async () => {
  vi.mocked(getDoctorAppointments).mockResolvedValue([{ id: 'date-test', patientNumber: 'QA', doctorId: 'doctor', doctorName: 'Doctor', specialization: 'Medicine', hospitalId: 'hospital', hospitalName: 'Hospital', departmentName: 'Medicine', serviceDate: localDateString(), queuePosition: 1, status: 'IN_CONSULTATION', paymentMethod: 'CASH', amount: 500, estimatedWaitMinutes: 0, createdAt: '' }])
  render(<MemoryRouter><DoctorConsultationPage /></MemoryRouter>)
  await waitFor(() => expect(screen.getByLabelText('Current consultation')).toHaveValue('date-test'))
  fireEvent.change(screen.getByLabelText('Doctor-finalized diagnosis *'), { target: { value: 'QA only' } })
  const date = screen.getByLabelText('Follow-up date') as HTMLInputElement
  date.value = '2099-10-01'
  fireEvent.submit(date.closest('form')!)
  await waitFor(() => expect(finalizeClinicalVisit).toHaveBeenCalledWith(expect.objectContaining({
    appointmentId: 'date-test', followUpDate: '2099-10-01', medicationReminderEnabled: false,
  })))
})

it('shows Hindi clinical labels and keeps inactive controls locked', async () => {
  localStorage.setItem('smartcare-language', 'hi')
  vi.mocked(getDoctorAppointments).mockResolvedValue([])
  render(<LanguageProvider><MemoryRouter><DoctorConsultationPage /></MemoryRouter></LanguageProvider>)
  expect(await screen.findByLabelText('लक्षण')).toBeDisabled()
  expect(screen.getByLabelText('डॉक्टर द्वारा तय निदान *')).toBeDisabled()
  expect(screen.getByPlaceholderText('पदार्थ या दवा')).toBeDisabled()
  expect(screen.getByRole('button', { name: 'विज़िट पूरी करें' })).toBeDisabled()
  expect(screen.getByText('मरीज़ों की कतार')).toBeInTheDocument()
  expect(screen.queryByText('Patient queue')).not.toBeInTheDocument()
})

it('locks clinical fields when no patient is in consultation', async () => {
  vi.mocked(getDoctorAppointments).mockResolvedValue([])
  render(<MemoryRouter><DoctorConsultationPage /></MemoryRouter>)
  expect(await screen.findByLabelText('Symptoms')).toBeDisabled()
  expect(screen.getByLabelText('Doctor-finalized diagnosis *')).toBeDisabled()
  expect(screen.getByPlaceholderText('Substance or medicine')).toBeDisabled()
  expect(screen.getByRole('button', { name: 'Finalize visit' })).toBeDisabled()
})

it('ignores a delayed test catalogue after the consultation has ended', async () => {
  let resolveCatalogue!: (value: DiagnosticProcedure[]) => void
  vi.mocked(getDiagnosticProcedures).mockReturnValueOnce(new Promise(resolve => { resolveCatalogue = resolve }))
  vi.mocked(getDoctorAppointments).mockResolvedValue([{ id: 'active', patientNumber: 'PAT', doctorId: 'doctor', doctorName: 'Doctor', specialization: 'Medicine', hospitalId: 'hospital', hospitalName: 'Hospital', departmentName: 'Medicine', serviceDate: localDateString(), queuePosition: 1, status: 'IN_CONSULTATION', paymentMethod: 'CASH', amount: 500, estimatedWaitMinutes: 0, createdAt: '' }])
  render(<MemoryRouter><DoctorConsultationPage /></MemoryRouter>)
  await waitFor(() => expect(getDiagnosticProcedures).toHaveBeenCalledWith('hospital'))
  vi.mocked(getDoctorAppointments).mockResolvedValue([])
  fireEvent.focus(window)
  await waitFor(() => expect(screen.getByLabelText('Current consultation')).toHaveValue(''))
  await act(async () => resolveCatalogue([{ id: 'old-test', hospitalId: 'hospital', hospitalName: 'Hospital', code: 'OLD', name: 'Outdated test', modality: 'LAB', turnaroundHours: 1, dailyCapacity: 10, estimatedDurationMinutes: 5, fee: 100 }]))
  expect(screen.queryByRole('option', { name: /Outdated test/ })).not.toBeInTheDocument()
})

it('preserves a draft on same-patient refresh but clears it when the active patient changes', async () => {
  const first: Appointment = { id: 'first', patientName: 'Test Patient One', patientGender: 'FEMALE', patientNumber: 'PAT-ONE', doctorId: 'doctor', doctorName: 'Doctor', specialization: 'Medicine', hospitalId: 'hospital', hospitalName: 'Hospital', departmentName: 'Medicine', serviceDate: localDateString(), queuePosition: 1, status: 'IN_CONSULTATION', paymentMethod: 'CASH', amount: 500, estimatedWaitMinutes: 0, createdAt: '' }
  vi.mocked(getDoctorAppointments).mockResolvedValue([first])
  render(<MemoryRouter><DoctorConsultationPage /></MemoryRouter>)
  await waitFor(() => expect(screen.getByLabelText('Current consultation')).toHaveValue('first'))
  expect(screen.getAllByLabelText('Test Patient One avatar')).toHaveLength(2)
  expect(screen.getByLabelText('Symptoms')).toBeEnabled()
  fireEvent.change(screen.getByLabelText('Symptoms'), { target: { value: 'First patient draft' } })
  fireEvent.change(screen.getByPlaceholderText('Substance or medicine'), { target: { value: 'First patient allergy' } })
  fireEvent.focus(window)
  await waitFor(() => expect(getDoctorAppointments).toHaveBeenCalledTimes(2))
  expect(screen.getByLabelText('Symptoms')).toHaveValue('First patient draft')
  vi.mocked(getDoctorAppointments).mockResolvedValue([{ ...first, id: 'second', patientName: null, patientGender: null, patientNumber: 'PAT-TWO', queuePosition: 2 }])
  fireEvent.focus(window)
  await waitFor(() => expect(screen.getByLabelText('Current consultation')).toHaveValue('second'))
  expect(screen.queryByLabelText('Test Patient One avatar')).not.toBeInTheDocument()
  expect(screen.getByLabelText('Symptoms')).toHaveValue('')
  expect(screen.getByPlaceholderText('Substance or medicine')).toHaveValue('')
})
