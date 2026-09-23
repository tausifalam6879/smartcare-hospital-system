import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { HomePage } from './HomePage'

vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ session: null }) }))
vi.mock('../config/runtime', () => ({ isStaticDemo: false, publicAsset: (path: string) => `/${path}` }))
vi.mock('../services/api', () => ({ api: { get: vi.fn().mockResolvedValue({ data: { content: [{ id: 'doctor-1', name: 'Dr. Test', specialization: 'Cardiology', consultationFee: 500 }] } }) } }))

describe('HomePage', () => {
  it('connects the care overview to booking, patient care and the doctor directory', async () => {
    render(<MemoryRouter><HomePage /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: /SmartCare/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /book an appointment/i })).toHaveAttribute('href', '/booking')
    expect(screen.getByRole('link', { name: /open my care/i })).toHaveAttribute('href', '/dashboard')
    expect((await screen.findByText('Dr. Test')).closest('a')).toHaveAttribute('href', '/doctors')
    expect(screen.queryByText('Phase 4')).not.toBeInTheDocument()
  })
})
