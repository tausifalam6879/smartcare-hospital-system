import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { HomePage } from './HomePage'

describe('HomePage', () => {
  it('shows the SmartCare identity, OPD booking, and planned queue modules', () => {
    render(<MemoryRouter><HomePage /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: /care made clear/i })).toBeInTheDocument()
    expect(screen.getAllByText('Book OPD number').length).toBeGreaterThan(0)
    expect(screen.getAllByText('Find a doctor').length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: /book opd number reserve before reaching/i })).toHaveAttribute('href', '/booking')
    expect(screen.getByRole('link', { name: /check my queue number/i })).toHaveAttribute('href', '/dashboard')
    expect(screen.getByText('My queue')).toBeInTheDocument()
    expect(screen.getAllByText('Phase 4').length).toBeGreaterThan(0)
  })
})
