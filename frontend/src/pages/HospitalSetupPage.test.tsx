import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { HospitalSetupPage } from './HospitalSetupPage'
import { api } from '../services/api'
vi.mock('../services/api', () => ({ api: { get: vi.fn(), post: vi.fn() }, messageFromError: () => 'Request failed' }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
it('selects the saved hospital so department setup and invitations use its server id', async () => {
  const hospital = { id: 'saved-hospital', name: 'Test Hospital', departments: [] }
  vi.mocked(api.get).mockResolvedValueOnce({ data: [] }).mockResolvedValueOnce({ data: [hospital] }).mockResolvedValue({ data: { content: [] } })
  vi.mocked(api.post).mockResolvedValue({ data: hospital })
  render(<MemoryRouter><HospitalSetupPage /></MemoryRouter>)
  await screen.findByText('Add your first hospital above to continue.')
  fireEvent.change(screen.getByLabelText('Hospital name'), { target: { value: 'Test Hospital' } })
  fireEvent.submit(screen.getByRole('button', { name: 'Add hospital' }).closest('form')!)
  await screen.findByRole('button', { name: 'Add department' })
  await waitFor(() => expect(screen.getByLabelText('Hospital to manage')).toHaveValue('saved-hospital'))
  expect(api.post).toHaveBeenCalledWith('/api/v1/hospitals', expect.objectContaining({ name: 'Test Hospital', active: true }))
  expect(screen.getByRole('button', { name: 'Add doctor' })).toBeDisabled()
})
