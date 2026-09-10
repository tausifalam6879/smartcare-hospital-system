import { describe, expect, it } from 'vitest'
import { messageFromError } from './api'

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
