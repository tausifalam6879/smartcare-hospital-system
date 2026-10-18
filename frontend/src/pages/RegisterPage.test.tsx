import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { RegisterPage } from './RegisterPage'

const register = vi.fn()
afterEach(() => { cleanup(); vi.clearAllMocks() })

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ register }),
}))

describe('RegisterPage', () => {
  it('activates an existing account with its current password without collecting a new identity', async () => {
    register.mockResolvedValue(undefined)
    render(<MemoryRouter><RegisterPage /></MemoryRouter>)
    fireEvent.change(screen.getByLabelText('Account / workspace'), { target: { value: 'CASHIER' } })
    fireEvent.click(screen.getByRole('checkbox', { name: /I already have a SmartCare account/ }))
    expect(screen.queryByLabelText(/Full name/i)).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Staff invitation code'), { target: { value: 'entire-code_with-hyphens' } })
    fireEvent.click(screen.getByRole('button', { name: 'Show invitation code' }))
    expect(screen.getByLabelText('Staff invitation code')).toHaveAttribute('type', 'text')
    fireEvent.change(screen.getByLabelText(/^Mobile number/i), { target: { value: '+919021609385' } })
    fireEvent.change(screen.getByLabelText(/^Current password/), { target: { value: 'existing-test-password' } })
    fireEvent.submit(screen.getByRole('button', { name: 'Activate staff account' }).closest('form')!)
    await waitFor(() => expect(register).toHaveBeenCalledWith(expect.objectContaining({ existingAccount: true, accountType: 'CASHIER', password: 'existing-test-password' })))
  })
  it('collects an invitation only for a staff role and submits that role with it', async () => {
    register.mockResolvedValue(undefined)
    render(<MemoryRouter><RegisterPage /></MemoryRouter>)
    expect(screen.queryByLabelText('Staff invitation code')).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Account / workspace'), { target: { value: 'OFFICE_CLERK' } })
    expect(screen.getByLabelText('Staff invitation code')).toBeRequired()
    fireEvent.change(screen.getByLabelText('Staff invitation code'), { target: { value: 'one-time-invite' } })
    fireEvent.change(screen.getByLabelText(/Full name/i), { target: { value: 'Test Clerk' } })
    fireEvent.change(screen.getByLabelText(/^Mobile number/i), { target: { value: '+919021609385' } })
    fireEvent.change(screen.getByLabelText(/^Password/i), { target: { value: 'unique-test-password' } })
    fireEvent.submit(screen.getByRole('button', { name: /Activate staff account/i }).closest('form')!)
    await waitFor(() => expect(register).toHaveBeenCalledWith(expect.objectContaining({ accountType: 'OFFICE_CLERK', invitationCode: 'one-time-invite' })))
  })
  it('explains that the emergency contact must be a mobile number', async () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>)

    fireEvent.change(screen.getByLabelText(/Full name/i), { target: { value: 'MD Tausif Alam' } })
    fireEvent.change(screen.getByLabelText(/^Mobile number/i), { target: { value: '+919021609384' } })
    fireEvent.change(screen.getByLabelText(/^Password/i), { target: { value: 'safe-test-password' } })
    fireEvent.change(screen.getByLabelText(/Emergency contact mobile number/i), { target: { value: 'MD TAUSIF ALAM' } })
    fireEvent.submit(screen.getByRole('button', { name: /Create account/i }).closest('form')!)

    expect(await screen.findByRole('alert')).toHaveTextContent(/must be a mobile number/i)
    expect(register).not.toHaveBeenCalled()
  })
})
