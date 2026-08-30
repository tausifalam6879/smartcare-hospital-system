import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { NavigationPage } from './NavigationPage'

const reception = {
  code: 'HELP_DESK', nameEn: 'Verified help desk', nameHi: 'सत्यापित सहायता डेस्क', type: 'RECEPTION' as const,
  building: 'Central Block', floorLabel: 'Ground Floor', zone: 'Blue', mapX: 70, mapY: 45,
}
const entrance = {
  code: 'ENTRY', nameEn: 'Main entrance', nameHi: 'मुख्य प्रवेश', type: 'ENTRANCE' as const,
  building: 'Central Block', floorLabel: 'Ground Floor', zone: 'Arrival', mapX: 10, mapY: 80,
}

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({ session: { accessToken: 'token', user: { id: 'patient-1', displayName: 'Patient', roles: ['PATIENT'] } } }),
}))

vi.mock('../services/api', () => ({ messageFromError: () => 'Request failed' }))

vi.mock('../services/navigation', () => ({
  getNavigationHospitals: vi.fn(() => Promise.resolve([{ id: 'hospital-1', code: 'RMQ-DEMO', name: 'Demo Care Centre', city: 'Delhi' }])),
  getAppointmentDestination: vi.fn(() => Promise.resolve({
    appointmentId: 'appointment-1', hospitalId: 'hospital-1', hospitalName: 'Demo Care Centre', doctorName: 'Dr. Unmapped',
    destination: reception, exactRoomMatch: false,
    guidanceEn: 'The room is not mapped; continue to the verified help desk.',
    guidanceHi: 'कमरा मैप नहीं है; सत्यापित सहायता डेस्क तक जाएँ।',
  })),
  getHospitalMap: vi.fn(() => Promise.resolve({
    hospitalId: 'hospital-1', hospitalName: 'Demo Care Centre', locations: [entrance, reception],
    checkpoints: [{ hospitalId: 'hospital-1', hospitalName: 'Demo Care Centre', publicCode: 'ENTRY-QR', labelEn: 'Entrance QR', labelHi: 'प्रवेश QR', entryPath: '/navigate/ENTRY-QR', location: entrance }],
  })),
  getCheckpoint: vi.fn(),
  getRoute: vi.fn((_hospital: string, _source: string, _destination: string, language: 'en' | 'hi') => Promise.resolve({
    available: true, language, stepFreeRequested: true, source: entrance, destination: reception,
    totalDistanceMeters: 40, estimatedMinutes: 2, pathCodes: ['ENTRY', 'HELP_DESK'],
    steps: [{ order: 1, instruction: language === 'hi' ? 'सहायता डेस्क तक जाएँ।' : 'Walk to the help desk.', distanceMeters: 40, durationSeconds: 50, fromCode: 'ENTRY', toCode: 'HELP_DESK', fromFloor: 'Ground Floor', toFloor: 'Ground Floor', floorTransition: false, stepFree: true }],
    message: language === 'hi' ? 'रास्ता मिल गया।' : 'Route found.', safetyNotice: language === 'hi' ? 'अस्पताल के संकेत मानें।' : 'Follow hospital signs.',
  })),
}))

describe('NavigationPage resilient appointment navigation', () => {
  it('shows a bilingual help-desk fallback and still renders the Dijkstra route', async () => {
    render(<MemoryRouter initialEntries={['/navigate?appointment=appointment-1']}><NavigationPage /></MemoryRouter>)

    expect(await screen.findByText('Safe fallback guidance')).toBeInTheDocument()
    expect(screen.getByText(/room is not mapped/i)).toBeInTheDocument()

    fireEvent.click(await screen.findByRole('button', { name: /Show my route/i }))
    expect(screen.getAllByText('Verified help desk').length).toBeGreaterThan(0)
    expect(await screen.findByText('Why did the app choose this path?')).toBeInTheDocument()
    expect(screen.getByText('Compare total cost')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'हिंदी' }))
    expect(await screen.findByText('सुरक्षित वैकल्पिक मार्गदर्शन')).toBeInTheDocument()
    expect((await screen.findAllByText(/सत्यापित सहायता डेस्क तक जाएँ/)).length).toBeGreaterThan(0)
    expect(screen.getByText('ऐप ने यह रास्ता क्यों चुना?')).toBeInTheDocument()
  })
})
