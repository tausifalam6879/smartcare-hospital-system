import type { Ambulance, AmbulanceRequest } from './ambulances'

export type AmbulanceRecommendation = {
  vehicle: Ambulance
  reason: string
  locationConfidence: 'AREA_MATCH' | 'FRESH_LOCATION' | 'LOCATION_UNAVAILABLE'
}

function words(value?: string) {
  return new Set((value ?? '').toLowerCase().split(/[^a-z0-9]+/).filter((word) => word.length >= 4))
}

export function recommendAvailableAmbulance(request: AmbulanceRequest, fleet: Ambulance[]): AmbulanceRecommendation | null {
  const available = fleet.filter((vehicle) => vehicle.status === 'AVAILABLE')
  if (available.length === 0) return null
  const pickupWords = words(`${request.pickupAddress} ${request.pickupLandmark ?? ''}`)
  const ranked = available.map((vehicle) => {
    const areaMatch = [...words(vehicle.currentArea)].some((word) => pickupWords.has(word))
    const locationTime = vehicle.locationUpdatedAt ? Date.parse(vehicle.locationUpdatedAt) : 0
    return { vehicle, areaMatch, locationTime }
  }).sort((left, right) => Number(right.areaMatch) - Number(left.areaMatch) || right.locationTime - left.locationTime || left.vehicle.callSign.localeCompare(right.vehicle.callSign))
  const best = ranked[0]
  if (best.areaMatch) return { vehicle: best.vehicle, locationConfidence: 'AREA_MATCH', reason: 'available vehicle with a matching last-shared service area' }
  if (best.locationTime > 0) return { vehicle: best.vehicle, locationConfidence: 'FRESH_LOCATION', reason: 'available vehicle with the most recently shared location' }
  return { vehicle: best.vehicle, locationConfidence: 'LOCATION_UNAVAILABLE', reason: 'available vehicle; live distance is unavailable and must be confirmed by the dispatcher' }
}
