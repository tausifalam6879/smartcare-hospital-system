export const authStorageKey = 'smartcare-session'
export const authExpiredEvent = 'smartcare-auth-expired'

export function readAuthSession() {
  const persistent = localStorage.getItem(authStorageKey)
  if (persistent) return persistent

  const legacy = sessionStorage.getItem(authStorageKey)
  if (legacy) {
    localStorage.setItem(authStorageKey, legacy)
    sessionStorage.removeItem(authStorageKey)
  }
  return legacy
}

export function writeAuthSession(value: string) {
  localStorage.setItem(authStorageKey, value)
  sessionStorage.removeItem(authStorageKey)
}

export function clearAuthSession() {
  localStorage.removeItem(authStorageKey)
  sessionStorage.removeItem(authStorageKey)
}
