import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { StaffAccessPage } from './StaffAccessPage'
import { api } from '../services/api'
import { MemoryRouter } from 'react-router-dom'
vi.mock('../services/api', () => ({ api: { get: vi.fn(), post: vi.fn(), delete: vi.fn() }, messageFromError: () => 'Request failed' }))
afterEach(() => { cleanup(); vi.clearAllMocks() })
beforeEach(() => {
  vi.mocked(api.get).mockImplementation(async (url) => url === '/api/v1/hospitals'
    ? { data: [{ id: 'hospital', name: 'Verified Hospital' }] }
    : url === '/api/v1/auth/staff-invitations/doctors' ? { data: [] }
    : { data: { content: [], totalPages: 0 } })
})
it('issues an invitation for the selected mobile and role and can revoke it', async () => {
  vi.mocked(api.post).mockResolvedValue({ data: { id: 'invitation', invitationCode: 'private-one-time-code', expiresAt: '2026-10-01T10:00:00Z' } })
  vi.mocked(api.delete).mockResolvedValue({})
  render(<MemoryRouter><StaffAccessPage /></MemoryRouter>)
  await screen.findAllByRole('option', { name: 'Verified Hospital' })
  fireEvent.change(screen.getByLabelText('Account / workspace'), { target: { value: 'OFFICE_CLERK' } })
  fireEvent.change(screen.getByLabelText('Hospital'), { target: { value: 'hospital' } })
  fireEvent.change(screen.getByLabelText('Invited mobile number'), { target: { value: '+919000000001' } })
  fireEvent.click(screen.getByRole('checkbox', { name: /I independently verified this person, their hospital role/ }))
  fireEvent.submit(screen.getByRole('button', { name: 'Issue one-time invitation' }).closest('form')!)
  expect(await screen.findByText('private-one-time-code')).toBeInTheDocument()
  expect(api.post).toHaveBeenCalledWith('/api/v1/auth/staff-invitations', { mobileNumber: '+919000000001', accountType: 'OFFICE_CLERK', hospitalId: 'hospital', doctorId: null })
  fireEvent.click(screen.getByRole('button', { name: 'Revoke this invitation' }))
  await waitFor(() => expect(screen.queryByText('private-one-time-code')).not.toBeInTheDocument())
  expect(api.delete).toHaveBeenCalledWith('/api/v1/auth/staff-invitations/invitation')
})

it('copies the full invitation with the original role and mobile and preserves it if the hospital changes', async () => {
  const writeText = vi.fn().mockResolvedValue(undefined)
  Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText } })
  vi.mocked(api.post).mockResolvedValue({ data: { id: 'invite', invitationCode: 'secure-full-code', expiresAt: '2030-10-01T10:00:00Z' } })
  render(<MemoryRouter><StaffAccessPage /></MemoryRouter>)
  await screen.findAllByRole('option', { name: 'Verified Hospital' })
  fireEvent.change(screen.getByLabelText('Hospital'), { target: { value: 'hospital' } })
  fireEvent.change(screen.getByLabelText('Account / workspace'), { target: { value: 'CASHIER' } })
  fireEvent.change(screen.getByLabelText('Invited mobile number'), { target: { value: '+919000000002' } })
  expect(screen.getByRole('button', { name: 'Issue one-time invitation' })).toBeDisabled()
  fireEvent.click(screen.getByRole('checkbox', { name: /I independently verified this person, their hospital role/ }))
  fireEvent.submit(screen.getByRole('button', { name: 'Issue one-time invitation' }).closest('form')!)
  await screen.findByText('secure-full-code')
  fireEvent.change(screen.getByLabelText('Hospital'), { target: { value: '' } })
  fireEvent.click(screen.getByRole('button', { name: 'Copy full invitation' }))
  await waitFor(() => expect(writeText).toHaveBeenCalledWith(expect.stringContaining('Hospital: Verified Hospital\nRole: Cashier\nMobile: +919000000002\nCode: secure-full-code')))
  expect(writeText).toHaveBeenCalledWith(expect.stringContaining('/register'))
})

it('reviews a pending request using its bound mobile and role', async () => {
  vi.mocked(api.get).mockImplementation(async url => url === '/api/v1/hospitals'
    ? { data: [{ id: 'hospital', name: 'Verified Hospital' }] }
    : url === '/api/v1/auth/staff-invitations/doctors' ? { data: [] }
    : url === '/api/v1/auth/staff-access-requests' ? { data: { totalPages: 1, content: [{ id: 'request', name: 'Verified Clerk', mobileNumber: '+919000000003', accountType: 'OFFICE_CLERK', hospitalId: 'hospital', message: 'Desk staff reference', createdAt: '2026-10-18T00:00:00Z' }] } }
    : { data: { content: [], totalPages: 0 } })
  vi.mocked(api.post).mockResolvedValue({ data: { id: 'invite', invitationCode: 'request-code', expiresAt: '2030-10-01T10:00:00Z' } })
  render(<MemoryRouter><StaffAccessPage /></MemoryRouter>)
  await screen.findAllByRole('option', { name: 'Verified Hospital' })
  fireEvent.change(screen.getByLabelText('Hospital'), { target: { value: 'hospital' } })
  fireEvent.click(await screen.findByRole('button', { name: 'Review for invitation' }))
  expect(screen.getByLabelText('Invited mobile number')).toHaveValue('+919000000003')
  fireEvent.click(screen.getByRole('checkbox', { name: /I independently verified this person, their hospital role/ }))
  fireEvent.submit(screen.getByRole('button', { name: 'Issue one-time invitation' }).closest('form')!)
  await waitFor(() => expect(api.post).toHaveBeenCalledWith('/api/v1/auth/staff-invitations', expect.objectContaining({ requestId: 'request', mobileNumber: '+919000000003', accountType: 'OFFICE_CLERK' })))
})

it('does not describe a hospital request failure as an empty hospital setup', async () => {
  vi.mocked(api.get).mockRejectedValue(new Error('offline'))
  render(<MemoryRouter><StaffAccessPage /></MemoryRouter>)
  expect(await screen.findByRole('alert')).toHaveTextContent('Request failed')
  expect(screen.queryByText(/No hospitals available/)).not.toBeInTheDocument()
  expect(screen.getByRole('button', { name: 'Retry hospitals' })).toBeInTheDocument()
})
