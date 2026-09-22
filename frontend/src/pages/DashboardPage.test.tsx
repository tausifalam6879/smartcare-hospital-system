import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { DashboardPage } from './DashboardPage'

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ session: { user: { displayName: 'Demo Patient', roles: ['PATIENT'] } }, logout: vi.fn() }),
}))

vi.mock('../services/appointments', async (importOriginal) => ({
  ...await importOriginal<typeof import('../services/appointments')>(),
  getMyAppointments: vi.fn().mockResolvedValue([]),
}))

vi.mock('../services/payments', async (importOriginal) => ({
  ...await importOriginal<typeof import('../services/payments')>(),
  getMyPayments: vi.fn().mockResolvedValue([]),
}))

describe('DashboardPage', () => {
  it('groups patient services without hiding any destination', async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>)

    expect(await screen.findByText('No active booking')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'What would you like to do?' })).toBeInTheDocument()
    expect(screen.getAllByRole('img')).toHaveLength(4)
    for (const href of [
      '/booking', '/doctors', '/navigate', '/operations', '/diagnostics', '/records',
      '/blood-group-analysis', '/blood-support', '/ambulance', '/assistant',
      '/follow-ups', '/notifications',
    ]) {
      expect(screen.getAllByRole('link').some((link) => link.getAttribute('href') === href)).toBe(true)
    }
    expect(screen.getByText(/View past bookings/).closest('details')).not.toHaveAttribute('open')
  })
})
