import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { StaffAccessRequestPage } from './StaffAccessRequestPage'
import { api } from '../services/api'
vi.mock('../services/api', () => ({ api: { get: vi.fn(), post: vi.fn() }, messageFromError: () => 'Request failed' }))
afterEach(() => { cleanup(); vi.clearAllMocks() })
it('requests review without creating an account or claiming automatic code delivery', async () => {
  vi.mocked(api.get).mockResolvedValue({ data: [{ id: 'hospital', name: 'Hospital' }] })
  vi.mocked(api.post).mockResolvedValue({})
  render(<MemoryRouter><StaffAccessRequestPage /></MemoryRouter>)
  await screen.findByRole('option', { name: 'Hospital' })
  fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Test Clerk' } })
  fireEvent.change(screen.getByLabelText('Hospital'), { target: { value: 'hospital' } })
  fireEvent.change(screen.getByLabelText('Account / workspace'), { target: { value: 'OFFICE_CLERK' } })
  fireEvent.change(screen.getByLabelText('Mobile number'), { target: { value: '+919000001111' } })
  fireEvent.change(screen.getByLabelText('Hospital affiliation and role details'), { target: { value: 'Verified desk staff reference' } })
  fireEvent.submit(screen.getByRole('button', { name: 'Submit access request' }).closest('form')!)
  await waitFor(() => expect(api.post).toHaveBeenCalledWith('/api/v1/auth/staff-access-requests', expect.objectContaining({ accountType: 'OFFICE_CLERK', hospitalId: 'hospital' })))
  expect(await screen.findByRole('status')).toHaveTextContent('No code is sent automatically')
  expect(screen.queryByLabelText('Password')).not.toBeInTheDocument()
})
