import { api } from './api'

export type FollowUpStatus = 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'MISSED'
export type CareFollowUp = { id: string; clinicalVisitId: string; visitDate: string; followUpDate: string; hospitalName: string; doctorName: string; specialization: string; instructions?: string; medicationReminderEnabled: boolean; status: FollowUpStatus; overdue: boolean; patientResponseAt?: string }

export async function getMyFollowUps() { return (await api.get<CareFollowUp[]>('/api/v1/follow-ups/mine')).data }
export async function updateFollowUpStatus(id: string, status: FollowUpStatus) { return (await api.patch<CareFollowUp>(`/api/v1/follow-ups/${id}/status`, { status })).data }
