import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { RecoverAccountPage } from './RecoverAccountPage'
import { api } from '../services/api'
vi.mock('../services/api', () => ({ api: { post: vi.fn() }, messageFromError: () => 'Request failed' }))
afterEach(() => { cleanup(); vi.clearAllMocks() })
it('redeems a code and removes sensitive inputs after success', async () => {
  vi.mocked(api.post).mockResolvedValue({})
  render(<MemoryRouter><RecoverAccountPage /></MemoryRouter>)
  fireEvent.change(screen.getByLabelText('Registered mobile number'), { target: { value: '+919000000000' } })
  fireEvent.change(screen.getByLabelText('Recovery code'), { target: { value: 'private-code' } })
  for (const label of ['New password', 'Confirm password']) fireEvent.change(screen.getByLabelText(label), { target: { value: 'recovered-password' } })
  fireEvent.click(screen.getByRole('button', { name: 'Reset password' }))
  expect(await screen.findByRole('status')).toHaveTextContent('Password reset')
  expect(screen.queryByLabelText('Recovery code')).not.toBeInTheDocument()
  expect(api.post).toHaveBeenCalledWith('/api/v1/auth/recover', { mobileNumber: '+919000000000', code: 'private-code', newPassword: 'recovered-password' })
})
