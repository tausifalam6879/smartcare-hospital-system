import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Link, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { HomePage } from './HomePage'

const auth = vi.hoisted(() => ({ session: null as null | { user: { roles: string[] } } }))
vi.mock('../context/AuthContext', () => ({ useAuth: () => auth }))
vi.mock('../config/runtime', () => ({ isStaticDemo: false, publicAsset: (path: string) => `/${path}` }))
vi.mock('../services/api', () => ({ api: { get: vi.fn().mockResolvedValue({ data: { content: [{ id: 'doctor-1', name: 'Dr. Test', specialization: 'Cardiology', consultationFee: 500 }] } }) } }))

describe('HomePage', () => {
  afterEach(cleanup)
  beforeEach(() => { auth.session = null })
  it.each(['PATIENT', 'DOCTOR', 'CASHIER'])('keeps Home accessible after %s login', async role => {
    auth.session = { user: { roles: [role] } }
    render(<MemoryRouter initialEntries={['/dashboard']}><Routes>
      <Route path="/dashboard" element={<Link to="/">Home</Link>} />
      <Route path="/" element={<HomePage />} />
    </Routes></MemoryRouter>)
    fireEvent.click(screen.getByRole('link', { name: 'Home' }))
    expect(await screen.findByText('Dr. Test')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /SmartCare/i })).toBeInTheDocument()
    expect(screen.queryByText(/Sign in to see your appointment/)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('link', { name: /Open My care/i }))
    expect(screen.getByRole('link', { name: 'Home' })).toBeInTheDocument()
  })
  it('connects the care overview to booking, patient care and the doctor directory', async () => {
    render(<MemoryRouter><HomePage /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: /SmartCare/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /book an appointment/i })).toHaveAttribute('href', '/booking')
    expect(screen.getByRole('link', { name: /open my care/i })).toHaveAttribute('href', '/dashboard')
    expect((await screen.findByText('Dr. Test')).closest('a')).toHaveAttribute('href', '/doctors')
    expect(screen.queryByText('Phase 4')).not.toBeInTheDocument()
  })
})
