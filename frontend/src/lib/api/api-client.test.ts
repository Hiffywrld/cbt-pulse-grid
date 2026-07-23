import { describe, expect, it, vi } from 'vitest'
import type { SessionStore, StoredSession } from '../storage/session-storage'
import { ApiClient } from './api-client'

const oldSession: StoredSession = { accessToken: 'old-access', refreshToken: 'old-refresh', tokenType: 'Bearer', expiresAt: '2030-01-01T10:00:00Z' }
const newSession: StoredSession = { accessToken: 'new-access', refreshToken: 'new-refresh', tokenType: 'Bearer', expiresAt: '2030-01-01T10:15:00Z' }

const memoryStore = (initial: StoredSession | null = oldSession) => {
  let value = initial
  const store: SessionStore = {
    read: vi.fn(() => value),
    write: vi.fn((session) => { value = session }),
    clear: vi.fn(() => { value = null }),
  }
  return { store, current: () => value }
}

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })

describe('ApiClient authentication recovery', () => {
  it('sends bodyless POST operations without inventing JSON content', async () => {
    const { store } = memoryStore()
    const fetcher = vi.fn().mockResolvedValue(json({ status: 'PUBLISHED' })) as unknown as typeof fetch
    const client = new ApiClient('http://localhost:8080', store, fetcher)

    await client.request('/api/v1/exams/exam-1/publish', { method: 'POST' })

    expect(fetcher).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/exams/exam-1/publish',
      expect.objectContaining({ method: 'POST', body: undefined }),
    )
    const headers = (fetcher as ReturnType<typeof vi.fn>).mock.calls[0][1]?.headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer old-access')
    expect(headers.get('Content-Type')).toBeNull()
  })

  it('preserves backend authentication errors instead of reporting a network failure', async () => {
    const { store } = memoryStore(null)
    const fetcher = vi.fn().mockResolvedValue(json({
      status: 401,
      error: 'Unauthorized',
      message: 'Invalid email or password',
      validationErrors: {},
    }, 401)) as unknown as typeof fetch
    const client = new ApiClient('http://localhost:8080', store, fetcher)

    await expect(client.request('/api/v1/auth/login', {
      method: 'POST',
      auth: false,
      body: { email: 'admin@example.edu', password: 'wrong' },
    })).rejects.toMatchObject({ status: 401, message: 'Invalid email or password' })
  })

  it('reports a genuine fetch rejection as a network failure', async () => {
    const { store } = memoryStore(null)
    const fetcher = vi.fn().mockRejectedValue(new TypeError('Failed to fetch')) as unknown as typeof fetch
    const client = new ApiClient('http://localhost:8080', store, fetcher)

    await expect(client.request('/api/v1/auth/login', {
      method: 'POST',
      auth: false,
      body: { email: 'admin@example.edu', password: 'CorrectPassword1!' },
    })).rejects.toMatchObject({ status: 0, message: 'Unable to reach the server' })
  })

  it('refreshes once and retries the original request with the rotated access token', async () => {
    const { store, current } = memoryStore()
    const fetcher = vi.fn()
      .mockResolvedValueOnce(json({ status: 401, error: 'Unauthorized', message: 'Authentication is required' }, 401))
      .mockResolvedValueOnce(json(newSession))
      .mockResolvedValueOnce(json({ value: 'recovered' })) as unknown as typeof fetch
    const client = new ApiClient('http://localhost:8080', store, fetcher)

    await expect(client.request<{ value: string }>('/api/v1/example')).resolves.toEqual({ value: 'recovered' })
    expect(fetcher).toHaveBeenCalledTimes(3)
    expect((fetcher as ReturnType<typeof vi.fn>).mock.calls[1][0]).toContain('/api/v1/auth/refresh')
    const retryHeaders = (fetcher as ReturnType<typeof vi.fn>).mock.calls[2][1]?.headers as Headers
    expect(retryHeaders.get('Authorization')).toBe('Bearer new-access')
    expect(current()).toEqual(newSession)
  })

  it('locks simultaneous 401 responses behind one refresh request', async () => {
    const { store } = memoryStore()
    let refreshCount = 0
    const fetcher = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = input.toString()
      if (url.endsWith('/api/v1/auth/refresh')) {
        refreshCount += 1
        await new Promise((resolve) => setTimeout(resolve, 5))
        return json(newSession)
      }
      const authorization = new Headers(init?.headers).get('Authorization')
      return authorization === 'Bearer old-access'
        ? json({ status: 401, error: 'Unauthorized', message: 'Authentication is required' }, 401)
        : json({ ok: true })
    }) as unknown as typeof fetch
    const client = new ApiClient('http://localhost:8080', store, fetcher)

    await Promise.all([client.request('/api/v1/one'), client.request('/api/v1/two')])
    expect(refreshCount).toBe(1)
    expect(fetcher).toHaveBeenCalledTimes(5)
  })

  it('clears the session and signals expiry when refresh fails', async () => {
    const { store, current } = memoryStore()
    const onExpired = vi.fn()
    const fetcher = vi.fn()
      .mockResolvedValueOnce(json({ status: 401, error: 'Unauthorized', message: 'Authentication is required' }, 401))
      .mockResolvedValueOnce(json({ status: 401, error: 'Unauthorized', message: 'Invalid refresh token' }, 401)) as unknown as typeof fetch
    const client = new ApiClient('http://localhost:8080', store, fetcher)
    client.setSessionExpiredHandler(onExpired)

    await expect(client.request('/api/v1/example')).rejects.toMatchObject({ status: 401 })
    expect(current()).toBeNull()
    expect(store.clear).toHaveBeenCalledOnce()
    expect(onExpired).toHaveBeenCalledOnce()
  })
})
