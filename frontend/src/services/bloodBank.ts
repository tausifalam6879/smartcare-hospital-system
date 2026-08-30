import { api } from './api'

export type BloodGroup = 'A_POSITIVE' | 'A_NEGATIVE' | 'B_POSITIVE' | 'B_NEGATIVE' |
  'AB_POSITIVE' | 'AB_NEGATIVE' | 'O_POSITIVE' | 'O_NEGATIVE'
export type BloodComponent = 'WHOLE_BLOOD' | 'PACKED_RED_CELLS' | 'PLATELETS' | 'FRESH_FROZEN_PLASMA'
export type AvailabilityStatus = 'AVAILABLE' | 'LIMITED' | 'UNAVAILABLE' | 'STALE_OR_UNVERIFIED'
export type BloodRequestStatus = 'SEARCHING' | 'PARTIALLY_RESERVED' | 'RESERVED' |
  'UNAVAILABLE' | 'FULFILLED' | 'CANCELLED'
export type DonorEligibilityStatus = 'PENDING_VERIFICATION' | 'ELIGIBLE' |
  'TEMPORARILY_INELIGIBLE' | 'WITHDRAWN'

export type BloodAvailability = {
  bloodBankId: string
  bloodBankName: string
  addressLine: string
  contactNumber: string
  sourceType: 'HOSPITAL_MANAGED' | 'AUTHORIZED_PARTNER' | 'TRUSTED_INTEGRATION'
  distanceKm: number
  estimatedTransferMinutes: number
  bloodGroup: BloodGroup
  component: BloodComponent
  status: AvailabilityStatus
  availableUnits: number
  lastVerifiedAt?: string
  verificationValidUntil?: string
}

export type BloodAllocation = {
  id: string
  bloodBankId: string
  bloodBankName: string
  contactNumber: string
  distanceKm: number
  estimatedTransferMinutes: number
  units: number
  expiresOn: string
  status: 'RESERVED' | 'FULFILLED' | 'RELEASED'
}

export type BloodRequest = {
  id: string
  patientNumber: string
  hospitalId: string
  hospitalName: string
  appointmentId?: string
  createdBy: string
  bloodGroup: BloodGroup
  component: BloodComponent
  requestedUnits: number
  matchedUnits: number
  urgency: 'ROUTINE' | 'URGENT' | 'EMERGENCY'
  status: BloodRequestStatus
  clinicalReason: string
  createdAt: string
  resolvedAt?: string
  cancellationReason?: string
  allocations: BloodAllocation[]
}

export type DonorOptIn = {
  id: string
  eligibilityStatus: DonorEligibilityStatus
  verifiedBloodGroup?: BloodGroup
  contactPreference: 'MOBILE' | 'SMS' | 'WHATSAPP' | 'EMAIL'
  consentedAt: string
  eligibilityVerifiedAt?: string
  consentActive: boolean
}

export async function getBloodAvailability(hospitalId: string, bloodGroup: BloodGroup,
                                           component: BloodComponent, units: number) {
  return (await api.get<BloodAvailability[]>('/api/v1/blood-banks/availability', {
    params: { hospitalId, bloodGroup, component, units },
  })).data
}

export async function getMyBloodRequests() {
  return (await api.get<BloodRequest[]>('/api/v1/blood-requests/mine')).data
}

export async function getMyDonorConsent() {
  const response = await api.get<DonorOptIn | null>('/api/v1/blood-donors/me')
  return response.data || null
}

export async function createDonorConsent(contactPreference: DonorOptIn['contactPreference']) {
  return (await api.post<DonorOptIn>('/api/v1/blood-donors/consent', { contactPreference })).data
}

export async function withdrawDonorConsent() {
  return (await api.post<DonorOptIn>('/api/v1/blood-donors/withdraw')).data
}
