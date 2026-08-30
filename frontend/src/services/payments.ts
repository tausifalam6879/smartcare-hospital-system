import { api } from './api'

export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'REFUND_PENDING' | 'REFUNDED'
export type RefundStatus = 'REQUESTED' | 'COMPLETED' | 'FAILED'

export type Payment = {
  id: string
  appointmentId: string
  status: PaymentStatus
  provider: 'DEVELOPMENT'
  paymentMethod: 'ONLINE' | 'CASH'
  amount: number
  currency: string
  providerReference: string
  receiptNumber?: string
  expiresAt?: string
  completedAt?: string
  refundStatus?: RefundStatus
  checkoutUrl?: string
  verificationMessage: string
}

export async function createPaymentIntent(appointmentId: string) {
  return (await api.post<Payment>('/api/v1/payments/intents', { appointmentId }, {
    headers: { 'Idempotency-Key': crypto.randomUUID() },
  })).data
}

export async function getMyPayments() {
  return (await api.get<Payment[]>('/api/v1/payments/mine')).data
}

export const paymentStatusLabel: Record<PaymentStatus, string> = {
  PENDING: 'Verification pending',
  SUCCEEDED: 'Paid',
  FAILED: 'Payment failed',
  CANCELLED: 'Payment intent cancelled',
  REFUND_PENDING: 'Refund requested',
  REFUNDED: 'Refunded',
}
