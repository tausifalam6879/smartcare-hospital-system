import { afterEach, describe, expect, it } from 'vitest'
import { AxiosError } from 'axios'
import { api, messageFromError } from './api'
import { clearAuthSession, readAuthSession, writeAuthSession } from './authStorage'

describe('session request isolation', () => {
  afterEach(clearAuthSession)

  it.each(['/api/v1/auth/login', '/api/v1/auth/register', '/api/v1/auth/register-staff', '/api/v1/auth/recover'])(
    'does not attach stale credentials to %s', async (url) => {
      writeAuthSession(JSON.stringify({ accessToken: 'old-token' }))
      await api.post(url, {}, { adapter: async (config) => {
        expect(config.headers.Authorization).toBeUndefined()
        return { config, data: {}, status: 200, statusText: 'OK', headers: {} }
      } })
    },
  )

  it('preserves a new session when an old request fails late', async () => {
    writeAuthSession(JSON.stringify({ accessToken: 'old-token' }))
    await expect(api.get('/api/v1/account', { adapter: async (config) => {
      expect(config.headers.Authorization).toBe('Bearer old-token')
      writeAuthSession(JSON.stringify({ accessToken: 'new-token' }))
      throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined,
        { config, data: {}, status: 401, statusText: 'Unauthorized', headers: {} })
    } })).rejects.toThrow('Unauthorized')
    expect(JSON.parse(readAuthSession()!).accessToken).toBe('new-token')
  })

  it('clears the current session when its own token is rejected', async () => {
    writeAuthSession(JSON.stringify({ accessToken: 'current-token' }))
    await expect(api.get('/api/v1/account', { adapter: async (config) => {
      throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined,
        { config, data: {}, status: 401, statusText: 'Unauthorized', headers: {} })
    } })).rejects.toThrow('Unauthorized')
    expect(readAuthSession()).toBeNull()
  })
})

function axiosError(data: unknown) {
  return {
    isAxiosError: true,
    response: { data, status: 400 },
  }
}

describe('messageFromError', () => {
  it('turns field validation details into a useful message', () => {
    expect(messageFromError(axiosError({
      detail: 'One or more fields are invalid.',
      errors: { emergencyContact: "must contain a mobile number, not a person's name" },
    }))).toBe("Emergency mobile number must contain a mobile number, not a person's name.")
  })

  it('also reads problem details returned as JSON text', () => {
    expect(messageFromError(axiosError(JSON.stringify({
      detail: 'An account already uses this email address.',
    })))).toBe('An account already uses this email address.')
  })

  it('never exposes an HTML error page as user-facing text', () => {
    expect(messageFromError(axiosError(
      '<!DOCTYPE html><html><body><h1>404</h1></body></html>',
    ))).toBe('The API returned a web page instead of SmartCare data. Check the backend URL and try again.')
  })
})
