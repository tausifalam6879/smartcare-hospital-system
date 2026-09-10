import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { QueueBookingPage } from './QueueBookingPage'

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    session: {
      accessToken: 'test-token',
      tokenType: 'Bearer',
      expiresAt: '2099-01-01T00:00:00Z',
      user: { id: 'patient-1', displayName: 'Test Patient', mobileNumber: '9999999999', roles: ['PATIENT'] },
    },
  }),
}))

vi.mock('../services/api', () => ({
  api: {
    get: vi.fn((url: string) => Promise.resolve({ data: url === '/api/v1/hospitals' ? [] : { content: [] } })),
  },
  messageFromError: () => 'Request failed',
}))

describe('QueueBookingPage project hospital flow', () => {
  beforeEach(() => sessionStorage.clear())

  it('keeps directory selection, books the OPD, and completes demo payment', async () => {
    render(<MemoryRouter initialEntries={['/booking?facility=29&prototypeDoctor=0']}><QueueBookingPage /></MemoryRouter>)

    expect(await screen.findByText('Raj Hospitals')).toBeInTheDocument()
    expect(screen.getByDisplayValue(/Dr\. Prakash Chandra/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('radio', { name: /Pay online/i }))
    fireEvent.click(screen.getByRole('button', { name: /Book project OPD number/i }))

    expect(await screen.findByRole('heading', { name: /Authorize your OPD payment/i })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: /Continue to secure provider/i }))
    fireEvent.click(screen.getByRole('button', { name: /I approved in the provider app/i }))

    expect(screen.getByRole('heading', { name: /Authorizing securely/i })).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: /Demo payment verified/i }, { timeout: 3000 })).toBeInTheDocument()
    expect(screen.getByText(/project OPD number .* is confirmed/i)).toBeInTheDocument()

    const stored = JSON.parse(sessionStorage.getItem('smartcare-prototype-bookings') ?? '[]') as Array<{ status: string }>
    expect(stored[0]?.status).toBe('CONFIRMED')
  })
})
