import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, expect, it, vi } from 'vitest'
import { LoginPage } from './LoginPage'
const login = vi.fn().mockResolvedValue(undefined)
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ login, session: null }) }))
afterEach(() => { cleanup(); vi.clearAllMocks() })
it('sends the selected workspace for server-side authorization without reusing an invitation', async () => {
  render(<MemoryRouter><LoginPage /></MemoryRouter>)
  fireEvent.change(screen.getByLabelText('Account / workspace'), { target: { value: 'DOCTOR' } })
  fireEvent.change(screen.getByLabelText('Mobile number or email'), { target: { value: '+919000000001' } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'unique-test-password' } })
  fireEvent.submit(screen.getByRole('button', { name: 'Sign in' }).closest('form')!)
  await waitFor(() => expect(login).toHaveBeenCalledWith('+919000000001', 'unique-test-password', 'DOCTOR'))
  expect(screen.queryByLabelText('Staff invitation code')).not.toBeInTheDocument()
})
