import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../services/api'
import { getMyMedicalRecord } from '../services/medicalRecords'
import { MedicalRecordsPage } from './MedicalRecordsPage'

vi.mock('../services/api', () => ({
  api: { get: vi.fn() },
  messageFromError: () => 'Request failed',
}))

vi.mock('../services/medicalRecords', () => ({
  getMyMedicalRecord: vi.fn(),
  uploadMedicalDocument: vi.fn(),
  downloadMedicalDocument: vi.fn(),
}))

describe('MedicalRecordsPage', () => {
  beforeEach(() => {
    vi.mocked(api.get).mockResolvedValue({ data: [{ id: 'hospital-1', name: 'City General Hospital' }] })
    vi.mocked(getMyMedicalRecord).mockResolvedValue({
      patientNumber: 'SC-2026-0001',
      generatedAt: '2026-08-22T10:00:00Z',
      allergies: [{
        id: 'allergy-1', substance: 'Penicillin', reaction: 'Skin rash', severity: 'HIGH',
        status: 'ACTIVE', recordedByDoctor: 'Dr. Asha Rao', recordedAt: '2026-08-22T09:00:00Z',
      }],
      visits: [{
        id: 'visit-1', appointmentId: 'appointment-1', visitDate: '2026-08-22',
        hospitalName: 'City General Hospital', doctorName: 'Dr. Asha Rao', specialization: 'Medicine',
        symptoms: 'Fever', diagnosis: 'Clinician documented diagnosis',
        finalizedAt: '2026-08-22T09:30:00Z', followUpRecommendation: 'Follow up in five days.',
        prescription: {
          id: 'prescription-1', prescribedAt: '2026-08-22T09:30:00Z',
          medicines: [{ medicineName: 'Paracetamol', dosage: '500 mg', frequency: 'Twice daily', duration: '3 days' }],
        },
      }],
      documents: [{
        id: 'document-1', hospitalId: 'hospital-1', hospitalName: 'City General Hospital',
        documentType: 'LAB_REPORT', originalFilename: 'cbc-report.pdf', contentType: 'application/pdf',
        sizeBytes: 2048, documentDate: '2026-08-21', verificationStatus: 'PATIENT_UPLOADED',
        uploadedAt: '2026-08-22T09:40:00Z', contentPath: '/api/v1/medical-records/documents/document-1/content',
        assistantReadiness: 'READY_FOR_TEXT_CHECK',
      }],
    })
  })

  it('renders the private longitudinal record and clinician-authored details', async () => {
    render(<MemoryRouter><MedicalRecordsPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /your health story/i })).toBeInTheDocument()
    expect(screen.getByText('Penicillin')).toBeInTheDocument()
    expect(screen.getByText('Clinician documented diagnosis')).toBeInTheDocument()
    expect(screen.getByText(/Paracetamol · 500 mg/)).toBeInTheDocument()
    expect(screen.getByText('cbc-report.pdf')).toBeInTheDocument()
    expect(screen.getByText(/Assistant: Text checked when asked/i)).toBeInTheDocument()
    expect(screen.getByText(/does not diagnose, prescribe or alter/i)).toBeInTheDocument()
  })
})
