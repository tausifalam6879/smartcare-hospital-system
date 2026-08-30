import type { FacilityOwnership } from '../data/indiaFacilities'
import type { PaymentMethod } from './appointments'

export type PrototypeBookingStatus = 'CASH_PENDING' | 'PAYMENT_PENDING' | 'CONFIRMED' | 'CANCELLED'

export type PrototypeBooking = {
  id: string
  hospitalName: string
  hospitalLocation: string
  ownership: FacilityOwnership
  doctorName: string
  specialization: string
  departmentName: string
  serviceDate: string
  paymentMethod: PaymentMethod
  queuePosition: number
  estimatedWaitMinutes: number
  amount: number
  status: PrototypeBookingStatus
  providerReference?: string
  receiptNumber?: string
  createdAt: string
}

const storageKey = 'raahmediq-prototype-bookings'

export function getPrototypeBookings(): PrototypeBooking[] {
  if (typeof window === 'undefined') return []
  try {
    const stored = window.sessionStorage.getItem(storageKey)
    return stored ? JSON.parse(stored) as PrototypeBooking[] : []
  } catch {
    return []
  }
}

function storePrototypeBookings(bookings: PrototypeBooking[]) {
  if (typeof window !== 'undefined') {
    window.sessionStorage.setItem(storageKey, JSON.stringify(bookings))
  }
}

export function createPrototypeBooking(booking: Omit<PrototypeBooking, 'id' | 'createdAt'>) {
  const created: PrototypeBooking = {
    ...booking,
    id: `SIM-${Date.now().toString(36).toUpperCase()}`,
    createdAt: new Date().toISOString(),
  }
  storePrototypeBookings([created, ...getPrototypeBookings()])
  return created
}

export function updatePrototypeBooking(id: string, changes: Partial<PrototypeBooking>) {
  let updated: PrototypeBooking | undefined
  const bookings = getPrototypeBookings().map((booking) => {
    if (booking.id !== id) return booking
    updated = { ...booking, ...changes }
    return updated
  })
  storePrototypeBookings(bookings)
  return updated
}
