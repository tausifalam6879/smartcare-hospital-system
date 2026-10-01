import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { isStaticDemo } from '../config/runtime'
import { api } from '../services/api'
import { authExpiredEvent, authStorageKey, clearAuthSession, readAuthSession, writeAuthSession } from '../services/authStorage'

type UserSummary = {
  id: string
  displayName: string
  mobileNumber: string
  email?: string
  patientNumber?: string
  roles: string[]
  photo?: string
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
  accountType?: string
  invitationCode?: string
}

type AuthContextValue = {
  session: AuthSession | null
  login: (credential: string, password: string, accountType?: string) => Promise<void>
  register: (input: RegisterInput) => Promise<void>
  logout: () => void
  refreshProfile: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)
function demoSession(displayName = 'Demo Patient', credential = 'demo@smartcare.health'): AuthSession {
  return {
    accessToken: 'github-pages-demo-session',
    tokenType: 'Demo',
    expiresAt: new Date(Date.now() + 8 * 60 * 60 * 1000).toISOString(),
    user: {
      id: 'github-pages-demo-patient',
      displayName,
      mobileNumber: credential.includes('@') ? '+910000000000' : credential,
      email: credential.includes('@') ? credential : undefined,
      patientNumber: 'SC-DEMO-2026',
      roles: ['PATIENT'],
    },
  }
}

function initialSession(): AuthSession | null {
  const raw = readAuthSession()
  if (!raw) return null
  try {
    const value = JSON.parse(raw) as AuthSession
    if (new Date(value.expiresAt).getTime() <= Date.now()) {
      clearAuthSession()
      return null
    }
    return value
  } catch {
    clearAuthSession()
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(initialSession)

  useEffect(() => {
    const synchronize = () => setSession(initialSession())
    const storageChanged = (event: StorageEvent) => {
      if (event.key === authStorageKey) synchronize()
    }
    window.addEventListener('storage', storageChanged)
    window.addEventListener(authExpiredEvent, synchronize)
    return () => {
      window.removeEventListener('storage', storageChanged)
      window.removeEventListener(authExpiredEvent, synchronize)
    }
  }, [])

  useEffect(() => {
    if (!session) return
    const remaining = new Date(session.expiresAt).getTime() - Date.now()
    if (remaining <= 0) {
      clearAuthSession()
      setSession(null)
      return
    }
    const timer = window.setTimeout(() => {
      clearAuthSession()
      setSession(null)
    }, Math.min(remaining, 2_147_483_647))
    return () => window.clearTimeout(timer)
  }, [session])

  const persist = (next: AuthSession) => {
    writeAuthSession(JSON.stringify(next))
    setSession(next)
  }

  const value = useMemo<AuthContextValue>(() => ({
    session,
    async login(credential, password, accountType) {
      if (isStaticDemo) {
        if (accountType && accountType !== 'AUTO' && accountType !== 'PATIENT') throw new Error('Staff login requires the running hospital backend.')
        persist(demoSession('Demo Patient', credential))
        return
      }
      const response = await api.post<AuthSession>('/api/v1/auth/login', { credential, password, accountType })
      persist(response.data)
    },
    async register(input) {
      const staff = input.accountType && input.accountType !== 'PATIENT'
      if (isStaticDemo) {
        if (staff) throw new Error('Staff registration requires the running hospital backend; it is unavailable in the static demo.')
        persist(demoSession(input.name.trim() || 'Demo Patient', input.email || input.mobileNumber))
        return
      }
      const { invitationCode, accountType, ...patient } = input
      const response = await api.post<AuthSession>(staff ? '/api/v1/auth/register-staff' : '/api/v1/auth/register',
        staff ? { invitationCode, accountType, patient } : patient)
      persist(response.data)
    },
    logout() {
      clearAuthSession()
      setSession(null)
    },
    async refreshProfile() {
      if (!session || isStaticDemo) return
      const { data } = await api.get<{ displayName: string; photo?: string }>('/api/v1/account')
      persist({ ...session, user: { ...session.user, displayName: data.displayName, photo: data.photo } })
    },
  }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
