import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getDiagnosticProcedures, getMyDiagnosticOrders } from '../services/diagnostics'
import { getNavigationHospitals } from '../services/navigation'
import { DiagnosticsPage } from './DiagnosticsPage'

vi.mock('../services/diagnostics', () => ({
  getMyDiagnosticOrders: vi.fn(),
  getDiagnosticProcedures: vi.fn(),
  scheduleDiagnosticOrder: vi.fn(),
  cancelDiagnosticOrder: vi.fn(),
}))

vi.mock('../services/navigation', () => ({ getNavigationHospitals: vi.fn() }))

describe('DiagnosticsPage', () => {
  beforeEach(() => {
    vi.mocked(getNavigationHospitals).mockResolvedValue([{
      id: 'hospital-1', code: 'RMQ', name: 'City General Hospital', city: 'Delhi',
    }])
    vi.mocked(getDiagnosticProcedures).mockResolvedValue([])
    vi.mocked(getMyDiagnosticOrders).mockResolvedValue([{
      id: 'order-1', appointmentId: 'appointment-1', patientNumber: 'RVQ-1',
      procedureId: 'procedure-1', procedureCode: 'CBC', procedureName: 'Complete Blood Count',
      modality: 'LAB', hospitalName: 'City General Hospital', orderedByDoctor: 'Dr. Asha Rao',
      status: 'RESULT_VERIFIED', priority: 'ROUTINE', clinicalNote: 'Persistent fatigue',
      preparationInstructions: 'No fasting required.', turnaroundHours: 6, fee: 450,
      scheduledDate: '2026-08-23', queuePosition: 1, building: 'Diagnostics Block',
      floorLabel: 'Ground Floor', roomNumber: 'Lab 2', orderedAt: '2026-08-22T10:00:00Z',
      resultVerifiedAt: '2026-08-23T10:00:00Z', result: {
        id: 'result-1', summary: 'CBC completed and verified.', overallFlag: 'NORMAL',
        verifiedBy: 'Lab Technician', verifiedAt: '2026-08-23T10:00:00Z', items: [{
          name: 'Haemoglobin', value: '13.4', unit: 'g/dL', referenceRange: '12.0-16.0', flag: 'NORMAL',
        }],
      },
    }])
  })

  it('shows only a staff-verified result with its provenance and clinical boundary', async () => {
    render(<MemoryRouter><DiagnosticsPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /Tests without the uncertainty/i })).toBeInTheDocument()
    expect(await screen.findByText('Complete Blood Count')).toBeInTheDocument()
    expect(screen.getByText('13.4 g/dL')).toBeInTheDocument()
    expect(screen.getByText(/Verified by Lab Technician/i)).toBeInTheDocument()
    expect(screen.getByText(/does not diagnose from these values/i)).toBeInTheDocument()
    expect(screen.getByText(/clinician must create the order/i)).toBeInTheDocument()
  })
})
