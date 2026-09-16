import { beforeEach, describe, expect, it } from 'vitest'
import { authStorageKey, clearAuthSession, readAuthSession, writeAuthSession } from './authStorage'

describe('auth storage', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('persists a session so another browser tab can read it', () => {
    writeAuthSession('{"accessToken":"token"}')

    expect(localStorage.getItem(authStorageKey)).toContain('token')
    expect(readAuthSession()).toContain('token')
  })

  it('migrates a legacy tab-only session and clears all copies on logout', () => {
    sessionStorage.setItem(authStorageKey, '{"accessToken":"legacy"}')

    expect(readAuthSession()).toContain('legacy')
    expect(localStorage.getItem(authStorageKey)).toContain('legacy')
    expect(sessionStorage.getItem(authStorageKey)).toBeNull()

    clearAuthSession()
    expect(readAuthSession()).toBeNull()
  })
})
