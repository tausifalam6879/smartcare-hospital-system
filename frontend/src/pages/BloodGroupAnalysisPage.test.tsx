import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../services/api'
import { getMyBloodGroupAnalyses, submitBloodGroupImage } from '../services/bloodGroupAnalysis'
import { BloodGroupAnalysisPage } from './BloodGroupAnalysisPage'

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ session: { user: { roles: ['PATIENT'] } } }),
}))

vi.mock('../services/bloodGroupAnalysis', () => ({
  getMyBloodGroupAnalyses: vi.fn(), submitBloodGroupImage: vi.fn(),
  getBloodGroupAnalysisWorklist: vi.fn(), loadBloodGroupImage: vi.fn(),
  recordBloodGroupObservations: vi.fn(), verifyBloodGroupAnalysis: vi.fn(),
  rejectBloodGroupAnalysis: vi.fn(),
}))

const submitted = {
  id: 'analysis-1', patientNumber: 'RMQ-2026-0001', hospitalId: 'hospital-1',
  hospitalName: 'City General Hospital', originalFilename: 'slide.png', contentType: 'image/png',
  sizeBytes: 4096, status: 'SUBMITTED' as const, modelInferenceStatus: 'NOT_CONFIGURED' as const,
  createdAt: '2026-08-22T10:00:00Z',
}

describe('BloodGroupAnalysisPage', () => {
  beforeEach(() => {
    vi.spyOn(api, 'get').mockResolvedValue({ data: [{ id: 'hospital-1', name: 'City General Hospital', city: 'Delhi' }] })
    vi.mocked(getMyBloodGroupAnalyses).mockResolvedValue([submitted])
    vi.mocked(submitBloodGroupImage).mockResolvedValue(submitted)
  })

  it('requires the lab-safety acknowledgement and never presents an unavailable model as a result', async () => {
    render(<MemoryRouter><BloodGroupAnalysisPage /></MemoryRouter>)

    expect(await screen.findByRole('heading', { name: /Blood-slide image review, with humans in control/i })).toBeInTheDocument()
    expect(await screen.findByText(/produced no AI blood-group result/i)).toBeInTheDocument()
    expect(screen.getByText(/No verified blood group is available/i)).toBeInTheDocument()
    const submit = screen.getByRole('button', { name: /Submit for lab review/i })
    expect(submit).toBeDisabled()

    const file = new File(['png-content'], 'slide.png', { type: 'image/png' })
    fireEvent.change(screen.getByLabelText(/Blood-slide image/i), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('checkbox'))
    expect(submit).toBeEnabled()
    fireEvent.submit(submit.closest('form') as HTMLFormElement)

    expect(await screen.findByRole('status')).toHaveTextContent(/Authorized laboratory review is now pending/i)
    expect(submitBloodGroupImage).toHaveBeenCalledWith(expect.objectContaining({
      hospitalId: 'hospital-1', safetyAcknowledged: true, file,
    }))
  })
})
