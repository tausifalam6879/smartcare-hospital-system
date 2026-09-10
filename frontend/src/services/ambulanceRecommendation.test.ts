import { describe, expect, it } from 'vitest'
import { recommendAvailableAmbulance } from './ambulanceRecommendation'
import type { Ambulance, AmbulanceRequest } from './ambulances'

const request = { pickupAddress: 'North Delhi Sector 4', pickupLandmark: 'Blue gate' } as AmbulanceRequest
const vehicle = (id: string, status: Ambulance['status'], currentArea?: string, locationUpdatedAt?: string) => ({ id, callSign: id, status, currentArea, locationUpdatedAt } as Ambulance)

describe('recommendAvailableAmbulance', () => {
  it('excludes unavailable vehicles and prefers a matching service area', () => {
    const result = recommendAvailableAmbulance(request, [
      vehicle('BUSY', 'ASSIGNED', 'North Delhi'), vehicle('OTHER', 'AVAILABLE', 'South Delhi'),
      vehicle('MATCH', 'AVAILABLE', 'North Delhi response area'),
    ])
    expect(result?.vehicle.id).toBe('MATCH')
    expect(result?.locationConfidence).toBe('AREA_MATCH')
  })

  it('uses freshest shared location when no area matches', () => {
    const result = recommendAvailableAmbulance(request, [
      vehicle('OLD', 'AVAILABLE', 'East', '2026-01-01T00:00:00Z'),
      vehicle('NEW', 'AVAILABLE', 'West', '2026-01-02T00:00:00Z'),
    ])
    expect(result?.vehicle.id).toBe('NEW')
  })

  it('does not recommend a vehicle when none is available', () => {
    expect(recommendAvailableAmbulance(request, [vehicle('BUSY', 'ASSIGNED')])).toBeNull()
  })
})
