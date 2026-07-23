import { describe, expect, it } from 'vitest'
import { validateHistoricalRange } from './date-ranges'

describe('context-aware historical date ranges', () => {
  const now = new Date('2026-07-23T12:00:00Z').getTime()
  it('allows historical ranges', () => expect(validateHistoricalRange('2026-01-01T09:00', '2026-01-02T09:00', now)).toBeNull())
  it('rejects future dates', () => expect(validateHistoricalRange('2027-01-01T09:00', '', now)).toMatch(/future/))
  it('rejects reversed ranges', () => expect(validateHistoricalRange('2026-06-02T09:00', '2026-06-01T09:00', now)).toMatch(/From/))
})
