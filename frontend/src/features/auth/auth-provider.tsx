import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '../../lib/api/client'
import { browserSessionStore } from '../../lib/storage/session-storage'
import type { CurrentUser, LoginRequest } from '../../types/auth'
import { authApi } from './auth-api'
import { AuthContext, type AuthStatus } from './auth-context'

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const navigate = useNavigate()
  const restorationStarted = useRef(false)
  const [initialSession] = useState(() => browserSessionStore.read())
  const [status, setStatus] = useState<AuthStatus>(initialSession ? 'restoring' : 'anonymous')
  const [user, setUser] = useState<CurrentUser | null>(null)

  const expireSession = useCallback(() => {
    browserSessionStore.clear()
    setUser(null)
    setStatus('anonymous')
    navigate('/login', { replace: true })
  }, [navigate])

  useEffect(() => {
    apiClient.setSessionExpiredHandler(expireSession)
  }, [expireSession])

  useEffect(() => {
    if (restorationStarted.current) return
    restorationStarted.current = true
    if (!initialSession) return
    void authApi.me().then(
      (profile) => {
        setUser(profile)
        setStatus('authenticated')
      },
      () => expireSession(),
    )
  }, [expireSession, initialSession])

  const login = useCallback(async (request: LoginRequest) => {
    const tokens = await authApi.login(request)
    browserSessionStore.write(tokens)
    try {
      const profile = await authApi.me()
      setUser(profile)
      setStatus('authenticated')
      return profile
    } catch (error) {
      browserSessionStore.clear()
      setUser(null)
      setStatus('anonymous')
      throw error
    }
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = browserSessionStore.read()?.refreshToken
    try {
      if (refreshToken) await authApi.logout({ refreshToken })
    } catch {
      // Local cleanup is mandatory even when the revocation endpoint is unreachable.
    } finally {
      browserSessionStore.clear()
      setUser(null)
      setStatus('anonymous')
      navigate('/login', { replace: true })
    }
  }, [navigate])

  const value = useMemo(
    () => ({ status, user, login, logout }),
    [status, user, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
