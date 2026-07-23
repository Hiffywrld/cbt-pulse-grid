import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiClient } from '../../lib/api/api-client'
import type { SessionStore, StoredSession } from '../../lib/storage/session-storage'
import type { CurrentUser, TokenResponse } from '../../types/auth'
import { createAuthApi } from './auth-api'

const tokens: TokenResponse = {
  accessToken: 'new-access-token',
  refreshToken: 'new-refresh-token',
  tokenType: 'Bearer',
  expiresAt: '2030-01-01T10:15:00Z',
}

const profile: CurrentUser = {
  id: 'user-1',
  email: 'admin@example.edu',
  institutionId: null,
  roles: ['SUPER_ADMIN'],
}

describe('authentication API contract', () => {
  let storedSession: StoredSession | null
  let sessionStore: SessionStore

  beforeEach(() => {
    storedSession = { ...tokens, accessToken: 'stale-access-token' }
    sessionStore = {
      read: vi.fn(() => storedSession),
      write: vi.fn((session) => { storedSession = session }),
      clear: vi.fn(() => { storedSession = null }),
    }
  })

  it('uses narrow authenticated profile and password endpoints', async () => {
    const request = vi.fn().mockResolvedValue({})
    const api = createAuthApi({ request } as unknown as ApiClient)
    request.mockResolvedValue({} as never)
    await api.updateProfile({ firstName: 'Amina', lastName: 'Okafor', avatarKey: 'emerald-orbit' })
    expect(request).toHaveBeenCalledWith('/api/v1/auth/profile', {
      method: 'PUT',
      body: { firstName: 'Amina', lastName: 'Okafor', avatarKey: 'emerald-orbit' },
    })
    await api.changePassword({ currentPassword: 'old', newPassword: 'new-password', confirmPassword: 'new-password' })
    expect(request).toHaveBeenLastCalledWith('/api/v1/auth/change-password', {
      method: 'POST',
      body: { currentPassword: 'old', newPassword: 'new-password', confirmPassword: 'new-password' },
    })
  })

  it('sends the exact public login request and parses the token and profile responses', async () => {
    const responses = [tokens, profile]
    const receiverSensitiveFetch: typeof fetch = function (this: typeof globalThis) {
      if (this !== globalThis) throw new TypeError('Illegal invocation')
      return Promise.resolve(new Response(JSON.stringify(responses.shift()), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }))
    }
    const fetcher = vi.fn(receiverSensitiveFetch)
    const authApi = createAuthApi(new ApiClient('http://localhost:8080', sessionStore, fetcher))

    const loginResponse = await authApi.login({
      email: 'admin@example.edu',
      password: 'CorrectPassword1!',
    })
    sessionStore.write(loginResponse)
    const profileResponse = await authApi.me()

    expect(fetcher).toHaveBeenCalledTimes(2)
    const [loginUrl, loginInit] = fetcher.mock.calls[0]
    const loginHeaders = new Headers(loginInit?.headers)
    expect(loginUrl).toBe('http://localhost:8080/api/v1/auth/login')
    expect(loginInit?.method).toBe('POST')
    expect(loginHeaders.get('Accept')).toBe('application/json')
    expect(loginHeaders.get('Content-Type')).toBe('application/json')
    expect(loginHeaders.has('Authorization')).toBe(false)
    expect(loginInit?.body).toBe(JSON.stringify({
      email: 'admin@example.edu',
      password: 'CorrectPassword1!',
    }))
    expect(loginResponse).toEqual(tokens)

    const [profileUrl, profileInit] = fetcher.mock.calls[1]
    expect(profileUrl).toBe('http://localhost:8080/api/v1/auth/me')
    expect(new Headers(profileInit?.headers).get('Authorization')).toBe(`Bearer ${tokens.accessToken}`)
    expect(profileResponse).toEqual(profile)
  })
})
