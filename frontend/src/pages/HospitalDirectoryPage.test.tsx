import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { HospitalDirectoryPage } from './HospitalDirectoryPage'

describe('HospitalDirectoryPage', () => {
  it('keeps the directory compact and shows hospital doctors inside SmartCare', () => {
    render(<MemoryRouter><HospitalDirectoryPage /></MemoryRouter>)

    expect(screen.getByRole('button', { name: /Private hospitals/ })).toBeInTheDocument()
    expect(screen.queryByText('Raj Hospitals')).not.toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Private hospitals/ }))
    fireEvent.change(screen.getByPlaceholderText('Search in this group'), { target: { value: 'Raj Hospitals' } })
    fireEvent.click(screen.getByRole('button', { name: /Raj Hospitals.*Ranchi/ }))

    expect(screen.getByRole('dialog', { name: 'Private hospitals' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Dr. Prakash Chandra' })).toBeInTheDocument()
    expect(screen.getByText('₹2,000')).toBeInTheDocument()
    expect(screen.getByText(/not a live provider listing/i)).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /Book this project OPD/i })[0]).toHaveAttribute('href', '/booking?facility=29&prototypeDoctor=0')
  })
})
