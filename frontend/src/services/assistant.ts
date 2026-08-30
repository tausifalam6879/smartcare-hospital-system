import { api } from './api'

export type Conversation = {
  id: string
  title: string
  lastActivityAt: string
}

export type Citation = {
  chunkId: string
  number: number
  sourceType: 'CLINICAL_VISIT' | 'MEDICAL_DOCUMENT'
  sourceId: string
  sourceLabel: string
  pageNumber?: number
  excerpt: string
  provenance: string
  sourcePath: string
}

export type AssistantMessage = {
  id: string
  role: 'USER' | 'ASSISTANT'
  content: string
  safetyClass: 'NORMAL' | 'EMERGENCY_ESCALATION' | 'CLINICAL_BOUNDARY' | 'EVIDENCE_UNAVAILABLE'
  grounded: boolean
  createdAt: string
  citations: Citation[]
}

export type AssistantStatus = {
  mode: string
  patientIsolation: boolean
  citationsRequired: boolean
  externalDataSharing: boolean
  documentSupport: string
}

export async function getAssistantStatus() {
  return (await api.get<AssistantStatus>('/api/v1/ai/status')).data
}

export async function getConversations() {
  return (await api.get<Conversation[]>('/api/v1/ai/conversations')).data
}

export async function createConversation() {
  return (await api.post<Conversation>('/api/v1/ai/conversations')).data
}

export async function getConversationMessages(conversationId: string) {
  return (await api.get<AssistantMessage[]>(`/api/v1/ai/conversations/${conversationId}/messages`)).data
}

export async function askAssistant(conversationId: string, question: string) {
  return (await api.post<{ conversationId: string; message: AssistantMessage }>(
    `/api/v1/ai/conversations/${conversationId}/messages`, { question },
  )).data
}
