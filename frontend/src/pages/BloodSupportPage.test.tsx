import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  getBloodAvailability, getMyBloodRequests, getMyDonorConsent,
} from '../services/bloodBank'
import { getNavigationHospitals } from '../services/navigation'
import { BloodSupportPage } from './BloodSupportPage'

vi.mock('../services/bloodBank', () => ({
  getBloodAvailability: vi.fn(),
  getMyBloodRequests: vi.fn(),
  getMyDonorConsent: vi.fn(),
  createDonorConsent: vi.fn(),
  withdrawDonorConsent: vi.fn(),
}))

vi.mock('../services/navigation', () => ({ getNavigationHospitals: vi.fn() }))

describe('BloodSupportPage', () => {
  beforeEach(() => {
    vi.mocked(getNavigationHospitals).mockResolvedValue([{
      id: 'hospital-1', code: 'SC', name: 'City General Hospital', city: 'Delhi',
    }])
    vi.mocked(getMyDonorConsent).mockResolvedValue(null)
    vi.mocked(getMyBloodRequests).mockResolvedValue([{
      id: 'request-1', patientNumber: 'RVQ-1', hospitalId: 'hospital-1',
      hospitalName: 'City General Hospital', createdBy: 'Dr. Asha Rao', bloodGroup: 'O_POSITIVE',
      component: 'PACKED_RED_CELLS', requestedUnits: 2, matchedUnits: 1,
      urgency: 'EMERGENCY', status: 'PARTIALLY_RESERVED',
      clinicalReason: 'Clinician-authorized transfusion support.',
      createdAt: '2026-08-22T10:00:00Z', allocations: [{
        id: 'allocation-1', bloodBankId: 'bank-1', bloodBankName: 'City Blood Centre',
        contactNumber: '+911140404099', distanceKm: 0, estimatedTransferMinutes: 0,
        units: 1, expiresOn: '2026-09-10', status: 'RESERVED',
      }],
    }])
    vi.mocked(getBloodAvailability).mockResolvedValue([{
      bloodBankId: 'bank-1', bloodBankName: 'City Blood Centre',
      addressLine: 'Emergency Block', contactNumber: '+911140404099',
      sourceType: 'HOSPITAL_MANAGED', distanceKm: 0, estimatedTransferMinutes: 0,
      bloodGroup: 'O_POSITIVE', component: 'PACKED_RED_CELLS', status: 'AVAILABLE',
      availableUnits: 3, lastVerifiedAt: '2026-08-22T10:00:00Z',
      verificationValidUntil: '2026-08-22T16:00:00Z',
    }])
  })

  it('shows owner-only request status and requires an explicit verified availability search', async () => {
    render(<MemoryRouter><BloodSupportPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /Blood support without false assurance/i })).toBeInTheDocument()
    expect(await screen.findByText('Partially reserved')).toBeInTheDocument()
    expect(screen.queryByText(/Patients cannot self-issue/i)).not.toBeInTheDocument()
    expect(screen.getByText(/does not calculate transfusion compatibility/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: /Check current verification/i }))
    expect(await screen.findByText('City Blood Centre')).toBeInTheDocument()
    expect(screen.getByText(/3 recently verified units/i)).toBeInTheDocument()
    expect(screen.getByText(/Reconfirm availability before any transfer/i)).toBeInTheDocument()
  })
})
