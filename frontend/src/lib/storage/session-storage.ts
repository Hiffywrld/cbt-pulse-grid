import type { TokenResponse } from '../../types/auth'

const STORAGE_KEY = 'cbt-pulse-grid.session'

export type StoredSession = TokenResponse

export interface SessionStore {
  read(): StoredSession | null
  write(session: StoredSession): void
  clear(): void
}

const isStoredSession = (value: unknown): value is StoredSession => {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Record<string, unknown>
  return (
    typeof candidate.accessToken === 'string' &&
    typeof candidate.refreshToken === 'string' &&
    typeof candidate.tokenType === 'string' &&
    typeof candidate.expiresAt === 'string'
  )
}

export const browserSessionStore: SessionStore = {
  read() {
    try {
      const raw = window.sessionStorage.getItem(STORAGE_KEY)
      if (!raw) return null
      const parsed: unknown = JSON.parse(raw)
      if (!isStoredSession(parsed)) {
        this.clear()
        return null
      }
      return parsed
    } catch {
      this.clear()
      return null
    }
  },
  write(session) {
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  },
  clear() {
    window.sessionStorage.removeItem(STORAGE_KEY)
  },
}

// sessionStorage matches the backend's current JSON token contract and limits persistence
// to one browser tab. It still exposes tokens to same-origin JavaScript if an XSS flaw exists,
// so the application avoids inline scripts, never logs tokens, and centralizes all access here.
