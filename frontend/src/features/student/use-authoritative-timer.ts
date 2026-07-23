import { useEffect, useMemo, useState } from 'react'

export const authoritativeRemainingSeconds = (
  serverTime: string,
  expiresAt: string,
  clientNow = Date.now(),
) => {
  const offset = Date.parse(serverTime) - clientNow
  return Math.max(0, Math.ceil((Date.parse(expiresAt) - (clientNow + offset)) / 1000))
}

export const useAuthoritativeTimer = (
  serverTime: string,
  expiresAt: string,
  onExpire: () => void,
) => {
  const authoritativeSeconds = useMemo(
    () => Math.max(0, Math.ceil((Date.parse(expiresAt) - Date.parse(serverTime)) / 1000)),
    [expiresAt, serverTime],
  )
  const [clock, setClock] = useState({ sourceSeconds: authoritativeSeconds, remaining: authoritativeSeconds })
  const remaining = clock.sourceSeconds === authoritativeSeconds ? clock.remaining : authoritativeSeconds

  useEffect(() => {
    const clientStartedAt = Date.now()
    const timer = window.setInterval(() => {
      const elapsed = Math.floor((Date.now() - clientStartedAt) / 1000)
      setClock({ sourceSeconds: authoritativeSeconds, remaining: Math.max(0, authoritativeSeconds - elapsed) })
    }, 1000)
    return () => window.clearInterval(timer)
  }, [authoritativeSeconds])

  useEffect(() => {
    if (remaining === 0) onExpire()
  }, [onExpire, remaining])

  return remaining
}

export const formatCountdown = (seconds: number) => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainder = seconds % 60
  return [hours, minutes, remainder].map((value) => String(value).padStart(2, '0')).join(':')
}
