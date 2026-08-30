import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { api } from '../services/api'

type UserSummary = {
  id: string
  displayName: string
  mobileNumber: string
  email?: string
  patientNumber?: string
  roles: string[]
}

export type AuthSession = {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: UserSummary
}

export type RegisterInput = {
  mobileNumber: string
  email?: string
  name: string
  password: string
  dateOfBirth?: string
  emergencyContact?: string
  preferredLanguage: string
}

type AuthContextValue = {
  session: AuthSession | null
  login: (credential: string, password: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)
const storageKey = 'raahmediq-session'

function initialSession(): AuthSession | null {
  const raw = sessionStorage.getItem(storageKey)
  if (!raw) return null
  try {
    const value = JSON.parse(raw) as AuthSession
    if (new Date(value.expiresAt).getTime() <= Date.now()) {
      sessionStorage.removeItem(storageKey)
      return null
    }
    return value
  } catch {
    sessionStorage.removeItem(storageKey)
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(initialSession)

  const persist = (next: AuthSession) => {
    sessionStorage.setItem(storageKey, JSON.stringify(next))
    setSession(next)
  }

  const value = useMemo<AuthContextValue>(() => ({
    session,
    async login(credential, password) {
      const response = await api.post<AuthSession>('/api/v1/auth/login', { credential, password })
      persist(response.data)
    },
    async register(input) {
      const response = await api.post<AuthSession>('/api/v1/auth/register', input)
      persist(response.data)
    },
    logout() {
      sessionStorage.removeItem(storageKey)
      setSession(null)
    },
  }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
