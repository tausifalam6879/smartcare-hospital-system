import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api } from '../services/api'
import { getMyBloodGroupAnalyses, submitBloodGroupImage } from '../services/bloodGroupAnalysis'
import { getMySavedAboPanels, saveAboPanel } from '../services/bloodReactionAnalysis'
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

vi.mock('../services/bloodReactionAnalysis', () => ({ saveAboPanel: vi.fn(), getMySavedAboPanels: vi.fn() }))

const submitted = {
  id: 'analysis-1', patientNumber: 'SC-2026-0001', hospitalId: 'hospital-1',
  hospitalName: 'City General Hospital', originalFilename: 'slide.png', contentType: 'image/png',
  sizeBytes: 4096, status: 'SUBMITTED' as const, modelInferenceStatus: 'NOT_CONFIGURED' as const,
  createdAt: '2026-08-22T10:00:00Z',
}

describe('BloodGroupAnalysisPage', () => {
  afterEach(() => cleanup())

  beforeEach(() => {
    vi.spyOn(api, 'get').mockResolvedValue({ data: [{ id: 'hospital-1', name: 'City General Hospital', city: 'Delhi' }] })
    vi.mocked(getMyBloodGroupAnalyses).mockResolvedValue([submitted])
    vi.mocked(submitBloodGroupImage).mockResolvedValue(submitted)
    vi.mocked(getMySavedAboPanels).mockResolvedValue([])
    vi.mocked(saveAboPanel).mockResolvedValue({ id: 'panel-1', antiAFilename: 'anti-a.png', antiBFilename: 'anti-b.png', antiDFilename: 'anti-d.png', antiAProbability: 0.99, antiBProbability: 0.01, antiDProbability: 0.99, antiAConfidence: 0.99, antiBConfidence: 0.99, antiDConfidence: 0.99, modelName: 'test', modelVersion: 'test', suggestedGroup: 'A_POSITIVE', status: 'PENDING_CLINICIAN_VERIFICATION', explanation: 'A+', createdAt: '2026-09-10T10:00:00Z' })
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

  it('sends three staff-labelled reaction-well files to the local model panel', async () => {
    render(<MemoryRouter><BloodGroupAnalysisPage /></MemoryRouter>)
    const antiAFile = new File(['a'], 'anti-a.png', { type: 'image/png' })
    const antiBFile = new File(['b'], 'anti-b.png', { type: 'image/png' })
    const antiDFile = new File(['d'], 'anti-d.png', { type: 'image/png' })

    fireEvent.change(await screen.findByLabelText('Anti-A well image'), { target: { files: [antiAFile] } })
    fireEvent.change(screen.getByLabelText('Anti-B well image'), { target: { files: [antiBFile] } })
    fireEvent.change(screen.getByLabelText('Anti-D well image'), { target: { files: [antiDFile] } })
    fireEvent.submit(screen.getByRole('button', { name: /Analyze reaction panel locally/i }).closest('form') as HTMLFormElement)

    expect((await screen.findAllByText('A+')).length).toBeGreaterThan(0)
    expect(saveAboPanel).toHaveBeenCalledWith({ antiAFile, antiBFile, antiDFile })
  })
})
