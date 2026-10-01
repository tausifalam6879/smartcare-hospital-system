import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { LiveQueuePage } from './LiveQueuePage'
import { LanguageProvider } from '../context/LanguageContext'
import { checkInAppointment, getPatientQueue } from '../services/queues'

vi.mock('../services/queues', () => ({ checkInAppointment: vi.fn(), getPatientQueue: vi.fn() }))
afterEach(() => { cleanup(); vi.clearAllMocks(); localStorage.removeItem('smartcare-language') })

it('shows Hindi queue status and navigation labels', async () => {
  localStorage.setItem('smartcare-language', 'hi')
  vi.mocked(getPatientQueue).mockResolvedValue({ appointmentId: 'qa', doctorId: 'doctor', doctorName: 'QA Doctor', hospitalName: 'QA Hospital', serviceDate: '2099-10-01', status: 'COMPLETED', patientsAhead: 0, estimatedWaitMinutes: 0, estimateNotice: 'Estimate only', lastUpdatedAt: '' })
  render(<LanguageProvider><MemoryRouter initialEntries={['/queue/qa']}><Routes><Route path='/queue/:appointmentId' element={<LiveQueuePage />} /></Routes></MemoryRouter></LanguageProvider>)
  expect(await screen.findByRole('heading', { name: 'विज़िट पूरी हुई' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'अभी अपडेट करें' })).toBeInTheDocument()
  expect(screen.getByRole('link', { name: 'इस कमरे का रास्ता देखें' })).toBeInTheDocument()
})

it('keeps a failed check-in explanation after a successful queue refresh', async () => {
  vi.mocked(getPatientQueue).mockResolvedValue({ appointmentId: 'qa', doctorId: 'doctor', doctorName: 'QA Doctor', hospitalName: 'QA Hospital', serviceDate: '2099-10-01', status: 'CONFIRMED', patientsAhead: 0, estimatedWaitMinutes: 0, estimateNotice: 'Estimate only', lastUpdatedAt: '' })
  vi.mocked(checkInAppointment).mockRejectedValue(new Error('Check-in window is not open'))
  render(<MemoryRouter initialEntries={['/queue/qa']}><Routes><Route path='/queue/:appointmentId' element={<LiveQueuePage />} /></Routes></MemoryRouter>)
  expect(await screen.findByRole('heading', { name: 'Appointment confirmed' })).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Check in with mobile' }))
  const explanation = (await screen.findByRole('alert')).textContent
  fireEvent.click(screen.getByRole('button', { name: 'Refresh now' }))
  await waitFor(() => expect(getPatientQueue).toHaveBeenCalledTimes(2))
  expect(screen.getByRole('alert')).toHaveTextContent(explanation!)
})
