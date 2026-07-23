import { describe, expect, it } from 'vitest'
import { authoritativeRemainingSeconds, formatCountdown } from './use-authoritative-timer'

describe('server-authoritative timer', () => {
  it('corrects client clock drift from backend server time', () => {
    const clientNow = Date.parse('2026-01-01T09:55:00Z')
    expect(authoritativeRemainingSeconds('2026-01-01T10:00:00Z', '2026-01-01T10:15:00Z', clientNow)).toBe(900)
  })

  it('never displays negative time', () => {
    expect(authoritativeRemainingSeconds('2026-01-01T10:00:00Z', '2026-01-01T09:59:00Z')).toBe(0)
    expect(formatCountdown(3661)).toBe('01:01:01')
  })
})

