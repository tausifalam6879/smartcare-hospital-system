import { api } from './api'

export type NotificationType =
  | 'APPOINTMENT_RESERVED' | 'PAYMENT_REQUIRED' | 'PAYMENT_SUCCESSFUL' | 'APPOINTMENT_CONFIRMED'
  | 'WAITLISTED' | 'WAITLIST_PROMOTED' | 'APPOINTMENT_CANCELLED' | 'RESERVATION_EXPIRED'
  | 'CASH_PAYMENT_DEADLINE' | 'CHECK_IN_CONFIRMED' | 'QUEUE_POSITION_UPDATED' | 'NOW_SERVING'
  | 'VISIT_COMPLETED' | 'NO_SHOW' | 'BLOOD_REQUEST_CREATED' | 'BLOOD_REQUEST_UPDATED'
  | 'AMBULANCE_REQUEST_CREATED' | 'AMBULANCE_REQUEST_UPDATED' | 'DOCTOR_DELAYED'
  | 'DOCTOR_UNAVAILABLE' | 'APPOINTMENT_RECOVERY_REQUIRED' | 'APPOINTMENT_RESCHEDULED'
  | 'REFUND_REVIEW_REQUIRED' | 'BLOOD_GROUP_ANALYSIS_SUBMITTED' | 'BLOOD_GROUP_ANALYSIS_UPDATED'
  | 'FOLLOW_UP_SCHEDULED' | 'MEDICATION_REMINDER_CREATED' | 'DIAGNOSTIC_ORDER_CREATED'
  | 'DIAGNOSTIC_RESULT_VERIFIED'

export type CareNotification = {
  id: string
  appointmentId?: string
  type: NotificationType
  title: string
  message: string
  read: boolean
  readAt?: string
  createdAt: string
}

export async function getNotifications() {
  return (await api.get<CareNotification[]>('/api/v1/notifications')).data
}

export async function getUnreadCount() {
  return (await api.get<{ unreadCount: number }>('/api/v1/notifications/unread-count')).data.unreadCount
}

export async function markNotificationRead(id: string) {
  return (await api.patch<CareNotification>(`/api/v1/notifications/${id}/read`)).data
}

export async function markAllNotificationsRead() {
  await api.patch('/api/v1/notifications/read-all')
}
