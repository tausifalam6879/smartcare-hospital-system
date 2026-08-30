import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createAmbulanceRequest, getAmbulanceAvailability, getMyAmbulanceRequests,
} from '../services/ambulances'
import { getNavigationHospitals } from '../services/navigation'
import { AmbulancePage } from './AmbulancePage'

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ session: { user: { mobileNumber: '+919300000071', roles: ['PATIENT'] } } }),
}))

vi.mock('../services/navigation', () => ({ getNavigationHospitals: vi.fn() }))
vi.mock('../services/ambulances', () => ({
  getAmbulanceAvailability: vi.fn(), getMyAmbulanceRequests: vi.fn(),
  createAmbulanceRequest: vi.fn(), cancelAmbulanceRequest: vi.fn(),
  getAmbulanceFleet: vi.fn(), getAmbulanceWorklist: vi.fn(), assignAmbulance: vi.fn(),
  acknowledgeAmbulance: vi.fn(), advanceAmbulanceRequest: vi.fn(),
}))

const request = {
  id: 'request-1', hospitalId: 'hospital-1', hospitalName: 'City General Hospital',
  hospitalAddress: '10 Care Road', hospitalContactNumber: '+911140404088',
  transportType: 'PATIENT_TRANSPORT' as const, priority: 'EMERGENCY' as const,
  status: 'REQUESTED' as const, pickupAddress: '42 Test Colony, Delhi',
  contactNumber: '+919300000071', statusUpdatedAt: '2026-08-22T10:00:00Z',
  createdAt: '2026-08-22T10:00:00Z', timeline: [{
    toStatus: 'REQUESTED' as const, actorLabel: 'Requester',
    note: 'Request recorded; dispatch approval is pending.', eventAt: '2026-08-22T10:00:00Z',
  }],
}

describe('AmbulancePage', () => {
  beforeEach(() => {
    vi.mocked(getNavigationHospitals).mockResolvedValue([{
      id: 'hospital-1', code: 'RMQ', name: 'City General Hospital', city: 'Delhi',
    }])
    vi.mocked(getAmbulanceAvailability).mockResolvedValue({
      hospitalId: 'hospital-1', hospitalName: 'City General Hospital', availableVehicles: 2,
      activeVehicles: 3, syntheticData: true, checkedAt: '2026-08-22T10:00:00Z',
    })
    vi.mocked(getMyAmbulanceRequests).mockResolvedValue([request])
    vi.mocked(createAmbulanceRequest).mockResolvedValue(request)
  })

  it('makes dispatcher approval and synthetic fleet boundaries explicit', async () => {
    render(<MemoryRouter><AmbulancePage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /Ambulance coordination with accountable handoffs/i })).toBeInTheDocument()
    expect(await screen.findByText(/Synthetic demo fleet/i)).toBeInTheDocument()
    expect(screen.getByText(/No vehicle assigned/i)).toBeInTheDocument()
    expect(screen.getByText(/Authorized dispatcher review is pending/i)).toBeInTheDocument()

    const submit = screen.getByRole('button', { name: /Submit for dispatcher review/i })
    expect(submit).toBeDisabled()
    fireEvent.change(screen.getByLabelText(/Pickup address/i), { target: { value: '55 Community Road, Delhi' } })
    fireEvent.click(screen.getByRole('checkbox'))
    fireEvent.click(submit)

    expect(await screen.findByRole('status')).toHaveTextContent(/waiting for authorized dispatcher review/i)
    expect(createAmbulanceRequest).toHaveBeenCalledWith(expect.objectContaining({
      hospitalId: 'hospital-1', transportType: 'PATIENT_TRANSPORT',
    }))
  })
})
