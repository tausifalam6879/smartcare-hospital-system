import { api } from './api'
import { isStaticDemo } from '../config/runtime'
import {
  staticDemoAppointmentDestination, staticDemoCheckpoint, staticDemoHospitals,
  staticDemoMap, staticDemoRoute,
} from '../data/staticNavigationDemo'

export type LocationType = 'ENTRANCE' | 'REGISTRATION' | 'LIFT' | 'STAIRS' | 'CORRIDOR' |
  'RECEPTION' | 'DOCTOR_ROOM' | 'LAB' | 'IMAGING' | 'PHARMACY' | 'EMERGENCY' | 'EXIT'

export type HospitalSummary = { id: string; code: string; name: string; city: string }

export type HospitalLocation = {
  code: string
  nameEn: string
  nameHi: string
  type: LocationType
  building: string
  floorLabel: string
  zone?: string
  roomNumber?: string
  mapX: number
  mapY: number
}

export type Checkpoint = {
  hospitalId: string
  hospitalName: string
  publicCode: string
  labelEn: string
  labelHi: string
  entryPath: string
  location: HospitalLocation
}

export type HospitalMap = {
  hospitalId: string
  hospitalName: string
  locations: HospitalLocation[]
  checkpoints: Checkpoint[]
}

export type RouteStep = {
  order: number
  instruction: string
  distanceMeters: number
  durationSeconds: number
  fromCode: string
  toCode: string
  fromFloor: string
  toFloor: string
  floorTransition: boolean
  stepFree: boolean
}

export type NavigationRoute = {
  available: boolean
  language: 'en' | 'hi'
  stepFreeRequested: boolean
  source: HospitalLocation
  destination: HospitalLocation
  totalDistanceMeters: number
  estimatedMinutes: number
  pathCodes: string[]
  steps: RouteStep[]
  message: string
  safetyNotice: string
}

export type AppointmentDestination = {
  appointmentId: string
  hospitalId: string
  hospitalName: string
  doctorName: string
  destination: HospitalLocation
  exactRoomMatch: boolean
  guidanceEn: string
  guidanceHi: string
}

export async function getNavigationHospitals() {
  if (isStaticDemo) return staticDemoHospitals
  return (await api.get<HospitalSummary[]>('/api/v1/hospitals')).data
}

export async function getHospitalMap(hospitalId: string) {
  if (isStaticDemo && hospitalId === staticDemoMap.hospitalId) return staticDemoMap
  return (await api.get<HospitalMap>(`/api/v1/navigation/hospitals/${hospitalId}/map`)).data
}

export async function getCheckpoint(publicCode: string) {
  if (isStaticDemo) {
    const result = staticDemoCheckpoint(publicCode)
    if (result) return result
  }
  return (await api.get<Checkpoint>(`/api/v1/navigation/checkpoints/${encodeURIComponent(publicCode)}`)).data
}

export async function getRoute(hospitalId: string, fromCheckpoint: string, destinationCode: string,
  language: 'en' | 'hi', stepFree: boolean) {
  if (isStaticDemo && hospitalId === staticDemoMap.hospitalId) {
    return staticDemoRoute(fromCheckpoint, destinationCode, language, stepFree)
  }
  return (await api.get<NavigationRoute>(`/api/v1/navigation/hospitals/${hospitalId}/route`, {
    params: { fromCheckpoint, destinationCode, language, stepFree },
  })).data
}

export async function getAppointmentDestination(appointmentId: string) {
  if (isStaticDemo) return staticDemoAppointmentDestination(appointmentId)
  return (await api.get<AppointmentDestination>(`/api/v1/navigation/appointments/${appointmentId}/destination`)).data
}
