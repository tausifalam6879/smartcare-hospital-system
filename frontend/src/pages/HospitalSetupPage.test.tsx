import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { HospitalSetupPage } from './HospitalSetupPage'
import { api } from '../services/api'
vi.mock('../services/api', () => ({ api: { get: vi.fn(), post: vi.fn() }, messageFromError: () => 'Request failed' }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
it('selects the saved hospital so department setup and invitations use its server id', async () => {
  const hospital = { id: 'saved-hospital', name: 'Test Hospital', departments: [] }
  let saved = false
  vi.mocked(api.get).mockImplementation(async (url) => ({ data: url === '/api/v1/hospitals' ? (saved ? [hospital] : []) : String(url).endsWith('/departments') ? [] : { content: [] } }))
  vi.mocked(api.post).mockImplementation(async () => { saved = true; return { data: hospital } })
  render(<MemoryRouter><HospitalSetupPage /></MemoryRouter>)
  await screen.findByText('Add your first hospital above to continue.')
  fireEvent.change(screen.getByLabelText('Hospital name'), { target: { value: 'Test Hospital' } })
  fireEvent.submit(screen.getByRole('button', { name: 'Add hospital' }).closest('form')!)
  await screen.findByRole('button', { name: 'Add department' })
  await waitFor(() => expect(screen.getByLabelText('Hospital to manage')).toHaveValue('saved-hospital'))
  expect(api.post).toHaveBeenCalledWith('/api/v1/hospitals', expect.objectContaining({ name: 'Test Hospital', active: true }))
  expect(screen.getByRole('button', { name: 'Add doctor' })).toBeDisabled()
})
it('loads existing departments from the dedicated endpoint rather than hospital summaries', async () => {
  vi.mocked(api.get).mockImplementation(async (url) => ({ data: url === '/api/v1/hospitals' ? [{ id: 'hospital', name: 'Test Hospital', departments: [] }] : String(url).endsWith('/departments') ? [{ id: 'department', name: 'General Medicine' }] : { content: [] } }))
  render(<MemoryRouter><HospitalSetupPage /></MemoryRouter>)
  await screen.findByRole('option', { name: 'Test Hospital' })
  fireEvent.change(screen.getByLabelText('Hospital to manage'), { target: { value: 'hospital' } })
  await screen.findByRole('option', { name: 'General Medicine' })
  expect(screen.getByRole('button', { name: 'Add doctor' })).toBeEnabled()
})
it('shows persisted OPD schedules from doctor details instead of the summary response', async () => {
  vi.mocked(api.get).mockImplementation(async url => ({ data: url === '/api/v1/hospitals'
    ? [{ id: 'hospital', name: 'Test Hospital', departments: [] }]
    : String(url).endsWith('/departments') ? [{ id: 'department', name: 'Medicine' }]
    : url === '/api/v1/doctors' ? { content: [{ id: 'doctor', name: 'Test Doctor', schedules: [] }] }
    : { id: 'doctor', name: 'Test Doctor', schedules: [{ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '13:00' }] } }))
  render(<MemoryRouter><HospitalSetupPage /></MemoryRouter>)
  await screen.findByRole('option', { name: 'Test Hospital' })
  fireEvent.change(screen.getByLabelText('Hospital to manage'), { target: { value: 'hospital' } })
  expect(await screen.findByText('MONDAY 09:00–13:00')).toBeInTheDocument()
  expect(screen.queryByText('No OPD hours yet')).not.toBeInTheDocument()
})
