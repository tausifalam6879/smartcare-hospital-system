import type { FacilityOwnership } from '../data/indiaFacilities'
import type { PaymentMethod } from './appointments'

export type PrototypeBookingStatus = 'CASH_PENDING' | 'PAYMENT_PENDING' | 'CONFIRMED' | 'CANCELLED'
export type PrototypeRefundStatus = 'NOT_DUE' | 'DEMO_REFUND_RECORDED'

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
  estimatedWaitMinimumMinutes?: number
  estimatedWaitMaximumMinutes?: number
  waitEstimateSource?: 'ML_HYBRID' | 'FALLBACK'
  waitModelVersion?: string
  amount: number
  status: PrototypeBookingStatus
  providerReference?: string
  receiptNumber?: string
  refundStatus?: PrototypeRefundStatus
  refundReference?: string
  cancelledAt?: string
  createdAt: string
}

const storageKey = 'smartcare-prototype-bookings'

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

export function cancelPrototypeBooking(id: string) {
  const booking = getPrototypeBookings().find((item) => item.id === id)
  if (!booking || booking.status === 'CANCELLED') return booking

  const verifiedOnlinePayment = booking.paymentMethod === 'ONLINE'
    && booking.status === 'CONFIRMED'
    && Boolean(booking.receiptNumber)

  return updatePrototypeBooking(id, {
    status: 'CANCELLED',
    cancelledAt: new Date().toISOString(),
    refundStatus: verifiedOnlinePayment ? 'DEMO_REFUND_RECORDED' : 'NOT_DUE',
    refundReference: verifiedOnlinePayment
      ? `SC-SIM-RF-${Date.now().toString(36).toUpperCase()}`
      : undefined,
  })
}
