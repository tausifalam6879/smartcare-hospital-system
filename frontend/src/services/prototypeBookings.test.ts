import { beforeEach, describe, expect, it } from 'vitest'
import { cancelPrototypeBooking, getPrototypeBookings, type PrototypeBooking } from './prototypeBookings'

const baseBooking: PrototypeBooking = {
  id: 'SIM-TEST',
  hospitalName: 'SmartCare Test Hospital',
  hospitalLocation: 'Ranchi · Jharkhand',
  ownership: 'GOVERNMENT',
  doctorName: 'Dr. Test',
  specialization: 'General Medicine',
  departmentName: 'General Medicine',
  serviceDate: '2026-09-11',
  paymentMethod: 'CASH',
  queuePosition: 10,
  estimatedWaitMinutes: 30,
  amount: 500,
  status: 'CASH_PENDING',
  createdAt: '2026-09-10T10:00:00.000Z',
}

function store(booking: PrototypeBooking) {
  sessionStorage.setItem('smartcare-prototype-bookings', JSON.stringify([booking]))
}

describe('prototype booking cancellation', () => {
  beforeEach(() => sessionStorage.clear())

  it('marks cash cancellation as not requiring a refund', () => {
    store(baseBooking)
    const cancelled = cancelPrototypeBooking(baseBooking.id)
    expect(cancelled?.status).toBe('CANCELLED')
    expect(cancelled?.refundStatus).toBe('NOT_DUE')
    expect(cancelled?.refundReference).toBeUndefined()
  })

  it('does not refund an unverified online demo payment', () => {
    store({ ...baseBooking, paymentMethod: 'ONLINE', status: 'PAYMENT_PENDING' })
    const cancelled = cancelPrototypeBooking(baseBooking.id)
    expect(cancelled?.refundStatus).toBe('NOT_DUE')
  })

  it('records a simulated refund for a verified online demo payment', () => {
    store({
      ...baseBooking,
      paymentMethod: 'ONLINE',
      status: 'CONFIRMED',
      receiptNumber: 'SC-SIM-RECEIPT',
    })
    const cancelled = cancelPrototypeBooking(baseBooking.id)
    expect(cancelled?.refundStatus).toBe('DEMO_REFUND_RECORDED')
    expect(cancelled?.refundReference).toMatch(/^SC-SIM-RF-/)
    expect(getPrototypeBookings()[0]?.cancelledAt).toBeTruthy()
  })
})
