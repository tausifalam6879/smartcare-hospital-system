import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { LanguageProvider } from '../context/LanguageContext'
import { CareFollowUpsPage } from './CareFollowUpsPage'
import { getMyFollowUps, updateFollowUpStatus, type CareFollowUp } from '../services/followUps'

vi.mock('../services/followUps', () => ({ getMyFollowUps: vi.fn(), updateFollowUpStatus: vi.fn() }))
afterEach(() => { cleanup(); vi.clearAllMocks(); localStorage.removeItem('smartcare-language') })
const item: CareFollowUp = { id: 'follow-up', clinicalVisitId: 'visit', visitDate: '2026-09-20', followUpDate: '2026-09-28', hospitalName: 'Hospital', doctorName: 'Dr. Care', specialization: 'Medicine', medicationReminderEnabled: true, status: 'SCHEDULED', overdue: false, canClose: true }

it('does not claim records are empty on load failure and allows retry', async () => {
  vi.mocked(getMyFollowUps).mockRejectedValueOnce(new Error('Unavailable')).mockResolvedValueOnce([item])
  render(<MemoryRouter><CareFollowUpsPage /></MemoryRouter>)
  fireEvent.click(await screen.findByRole('button', { name: 'Try again' }))
  expect(screen.queryByText('No follow-up scheduled')).not.toBeInTheDocument()
  expect(await screen.findByText('Dr. Care')).toBeInTheDocument()
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
})

it('uses server hospital-date eligibility and updates the same follow-up', async () => {
  vi.mocked(getMyFollowUps).mockResolvedValue([item])
  vi.mocked(updateFollowUpStatus).mockResolvedValue({ ...item, status: 'COMPLETED', canClose: false })
  render(<MemoryRouter><CareFollowUpsPage /></MemoryRouter>)
  fireEvent.click(await screen.findByRole('button', { name: 'Mark completed' }))
  expect(updateFollowUpStatus).toHaveBeenCalledWith('follow-up', 'COMPLETED')
  expect(await screen.findByText('Completed')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Mark missed' })).not.toBeInTheDocument()
})

it('renders Hindi and does not close a future follow-up', async () => {
  localStorage.setItem('smartcare-language', 'hi')
  vi.mocked(getMyFollowUps).mockResolvedValue([{ ...item, canClose: false }])
  render(<LanguageProvider><MemoryRouter><CareFollowUpsPage /></MemoryRouter></LanguageProvider>)
  expect(await screen.findByRole('button', { name: 'आने की पुष्टि करें' })).toBeInTheDocument()
  expect(screen.getByText('दवा रिमाइंडर चालू है')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'पूरा हुआ दर्ज करें' })).not.toBeInTheDocument()
})
