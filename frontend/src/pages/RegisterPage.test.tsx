import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { RegisterPage } from './RegisterPage'

const register = vi.fn()

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ register }),
}))

describe('RegisterPage', () => {
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
