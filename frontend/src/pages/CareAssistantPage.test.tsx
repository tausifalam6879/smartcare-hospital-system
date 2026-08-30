import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  getAssistantStatus, getConversationMessages, getConversations,
} from '../services/assistant'
import { CareAssistantPage } from './CareAssistantPage'

vi.mock('../services/api', () => ({ messageFromError: () => 'Request failed' }))

vi.mock('../services/assistant', () => ({
  getAssistantStatus: vi.fn(),
  getConversationMessages: vi.fn(),
  getConversations: vi.fn(),
  createConversation: vi.fn(),
  askAssistant: vi.fn(),
}))

describe('CareAssistantPage', () => {
  beforeEach(() => {
    vi.mocked(getAssistantStatus).mockResolvedValue({
      mode: 'PRIVATE_LOCAL_GROUNDED', patientIsolation: true, citationsRequired: true,
      externalDataSharing: false, documentSupport: 'Text PDFs are supported.',
    })
    vi.mocked(getConversations).mockResolvedValue([{
      id: 'conversation-1', title: 'Latest lab report', lastActivityAt: '2026-08-22T10:00:00Z',
    }])
    vi.mocked(getConversationMessages).mockResolvedValue([{
      id: 'message-1', role: 'ASSISTANT', content: 'Your report records haemoglobin 13.4 g/dL.',
      safetyClass: 'NORMAL', grounded: true, createdAt: '2026-08-22T10:01:00Z',
      citations: [{
        chunkId: 'chunk-1', number: 1, sourceType: 'MEDICAL_DOCUMENT', sourceId: 'document-1',
        sourceLabel: 'LAB REPORT — cbc.pdf', pageNumber: 1, excerpt: 'Haemoglobin 13.4 g/dL',
        provenance: 'patient uploaded', sourcePath: '/records',
      }],
    }])
  })

  it('shows a grounded answer, provenance and private-mode boundary', async () => {
    render(<MemoryRouter><CareAssistantPage /></MemoryRouter>)

    expect(screen.getByRole('heading', { name: /ask your record/i })).toBeInTheDocument()
    expect((await screen.findAllByText(/haemoglobin 13.4/i)).length).toBeGreaterThan(0)
    expect(screen.getByText(/LAB REPORT — cbc.pdf/i)).toBeInTheDocument()
    expect(screen.getByText(/patient uploaded · Page 1/i)).toBeInTheDocument()
    expect(screen.getByText(/No record text leaves this local grounded mode/i)).toBeInTheDocument()
    expect(screen.getByText(/Do not wait for an assistant response/i)).toBeInTheDocument()
  })
})
