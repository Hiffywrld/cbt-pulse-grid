import { describe, expect, it } from 'vitest'
import { shouldReplaceQueuedAnswer } from './attempt-storage'

describe('offline answer sequence protection', () => {
  it('accepts only a sequence newer than the queued answer', () => {
    expect(shouldReplaceQueuedAnswer(undefined, 0)).toBe(true)
    expect(shouldReplaceQueuedAnswer(4, 5)).toBe(true)
    expect(shouldReplaceQueuedAnswer(4, 4)).toBe(false)
    expect(shouldReplaceQueuedAnswer(4, 3)).toBe(false)
  })
})

