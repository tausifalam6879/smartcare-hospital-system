import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 12_000,
})

type ApiErrorBody = {
  detail?: string
  message?: string
  title?: string
  errors?: Record<string, string | string[]>
}

const fieldLabels: Record<string, string> = {
  mobileNumber: 'Mobile number',
  email: 'Email',
  name: 'Full name',
  password: 'Password',
  dateOfBirth: 'Date of birth',
  emergencyContact: 'Emergency mobile number',
  preferredLanguage: 'Language',
}

function errorBody(value: unknown): ApiErrorBody | undefined {
  if (typeof value === 'string') {
    try {
      return JSON.parse(value) as ApiErrorBody
    } catch {
      return value.trim() ? { detail: value } : undefined
    }
  }
  return value && typeof value === 'object' ? value as ApiErrorBody : undefined
}

function validationMessage(errors: ApiErrorBody['errors']) {
  if (!errors) return undefined
  const messages = Object.entries(errors).flatMap(([field, value]) => {
    const label = fieldLabels[field] ?? field.replace(/([A-Z])/g, ' $1').replace(/^./, (letter) => letter.toUpperCase())
    const details = Array.isArray(value) ? value : [value]
    return details.filter(Boolean).map((detail) => `${label} ${detail}.`.replace(/\.\.$/, '.'))
  })
  return messages.length ? messages.join(' ') : undefined
}

api.interceptors.request.use((config) => {
  const raw = sessionStorage.getItem('smartcare-session')
  if (raw) {
    try {
      const session = JSON.parse(raw) as { accessToken?: string }
      if (session.accessToken) config.headers.Authorization = `Bearer ${session.accessToken}`
    } catch {
      sessionStorage.removeItem('smartcare-session')
    }
  }
  config.headers['X-Correlation-ID'] = crypto.randomUUID()
  return config
})

export function messageFromError(error: unknown) {
  if (axios.isAxiosError(error)) {
    const body = errorBody(error.response?.data)
    const validation = validationMessage(body?.errors)
    if (validation) return validation
    return body?.detail ?? body?.message ?? (error.response ? 'The request could not be completed. Please check the entered details.' : 'The service is not reachable right now.')
  }
  return 'Something went wrong. Please try again.'
}
