import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { StaffAccessPage } from './StaffAccessPage'
import { api } from '../services/api'
import { MemoryRouter } from 'react-router-dom'
vi.mock('../services/api', () => ({ api: { get: vi.fn(), post: vi.fn(), delete: vi.fn() }, messageFromError: () => 'Request failed' }))
afterEach(() => { cleanup(); vi.clearAllMocks() })
it('issues an invitation for the selected mobile and role and can revoke it', async () => {
  vi.mocked(api.get).mockResolvedValue({ data: [{ id: 'hospital', name: 'Verified Hospital' }] })
  vi.mocked(api.post).mockResolvedValue({ data: { id: 'invitation', invitationCode: 'private-one-time-code', expiresAt: '2026-10-01T10:00:00Z' } })
  vi.mocked(api.delete).mockResolvedValue({})
  render(<MemoryRouter><StaffAccessPage /></MemoryRouter>)
  await screen.findAllByRole('option', { name: 'Verified Hospital' })
  fireEvent.change(screen.getByLabelText('Account / workspace'), { target: { value: 'OFFICE_CLERK' } })
  fireEvent.change(screen.getByLabelText('Hospital'), { target: { value: 'hospital' } })
  fireEvent.change(screen.getByLabelText('Invited mobile number'), { target: { value: '+919000000001' } })
  fireEvent.submit(screen.getByRole('button', { name: 'Issue one-time invitation' }).closest('form')!)
  expect(await screen.findByText('private-one-time-code')).toBeInTheDocument()
  expect(api.post).toHaveBeenCalledWith('/api/v1/auth/staff-invitations', { mobileNumber: '+919000000001', accountType: 'OFFICE_CLERK', hospitalId: 'hospital', doctorId: null })
  fireEvent.click(screen.getByRole('button', { name: 'Revoke this invitation' }))
  await waitFor(() => expect(screen.queryByText('private-one-time-code')).not.toBeInTheDocument())
  expect(api.delete).toHaveBeenCalledWith('/api/v1/auth/staff-invitations/invitation')
})
