import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { AccountPage } from './AccountPage'
import { api } from '../services/api'
const logout = vi.fn(), refreshProfile = vi.fn().mockResolvedValue(undefined)
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ session: { user: { id: 'owner', roles: ['PATIENT'] } }, logout, refreshProfile }) }))
vi.mock('../services/api', () => ({ api: { get: vi.fn(), put: vi.fn(), post: vi.fn(), delete: vi.fn() }, messageFromError: () => 'Request failed' }))
afterEach(() => { cleanup(); vi.clearAllMocks() })
it('saves identity and rejects oversized photos without uploading', async () => {
  const profile = { displayName: 'Test Patient', gender: 'UNDISCLOSED', photo: null, hospitalIds: [] }
  vi.mocked(api.get).mockResolvedValue({ data: profile }); vi.mocked(api.put).mockResolvedValue({ data: { ...profile, displayName: 'Updated Patient' } })
  render(<MemoryRouter><AccountPage /></MemoryRouter>)
  const name = await screen.findByLabelText('Full name')
  fireEvent.change(name, { target: { value: 'Updated Patient' } })
  fireEvent.click(screen.getByRole('button', { name: 'Save profile' }))
  await waitFor(() => expect(api.put).toHaveBeenCalledWith('/api/v1/account', { displayName: 'Updated Patient', gender: 'UNDISCLOSED' }))
  const file = new File([new Uint8Array(1_048_577)], 'large.png', { type: 'image/png' })
  fireEvent.change(screen.getByLabelText('Profile photo'), { target: { files: [file] } })
  expect(await screen.findByRole('alert')).toHaveTextContent('under 1 MB')
  expect(api.post).not.toHaveBeenCalled()
})
it('requires matching passwords and signs out after a successful change', async () => {
  vi.mocked(api.get).mockResolvedValue({ data: { displayName: 'Test', photo: null, gender: null, hospitalIds: [] } }); vi.mocked(api.post).mockResolvedValue({})
  render(<MemoryRouter><AccountPage /></MemoryRouter>)
  fireEvent.change(await screen.findByLabelText('Current password'), { target: { value: 'old-password-strong' } })
  fireEvent.change(screen.getByLabelText('New password'), { target: { value: 'new-password-strong' } })
  fireEvent.change(screen.getByLabelText('Confirm new password'), { target: { value: 'mismatch-password' } })
  fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('do not match')
  expect(api.post).not.toHaveBeenCalled()
  fireEvent.change(screen.getByLabelText('Confirm new password'), { target: { value: 'new-password-strong' } })
  fireEvent.click(screen.getByRole('button', { name: 'Change password' }))
  await waitFor(() => expect(logout).toHaveBeenCalled())
})
