import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../../lib/api/client'
import { attemptApi } from './attempt-api'

vi.mock('../../lib/api/client', () => ({ apiClient: { request: vi.fn() } }))

describe('student attempt API contract', () => {
  beforeEach(() => vi.clearAllMocks())

  it('starts with the exact write-only PIN and device contract', async () => {
    vi.mocked(apiClient.request).mockResolvedValue({ attemptId: 'attempt-1' })
    await attemptApi.start('exam-1', '123456', 'local-device')
    expect(apiClient.request).toHaveBeenCalledWith('/api/v1/student/exams/exam-1/attempts', {
      method: 'POST',
      body: { accessPin: '123456', deviceId: 'local-device' },
    })
  })

  it('uses idempotent answer and monitoring batch contracts', async () => {
    vi.mocked(apiClient.request).mockResolvedValue({})
    await attemptApi.syncAnswers('attempt-1', 'sync-1', [{ attemptQuestionId: 'q1', selectedOptionIds: ['o1'], clientSequence: 4 }])
    await attemptApi.monitoringEvents('attempt-1', 'sync-2', [{ eventId: 'event-1', eventType: 'TAB_HIDDEN', occurredAt: '2026-01-01T00:00:00Z', metadata: {} }])
    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/student/attempts/attempt-1/answers', {
      method: 'PUT',
      body: { syncId: 'sync-1', answers: [{ attemptQuestionId: 'q1', selectedOptionIds: ['o1'], clientSequence: 4 }] },
    })
    expect(apiClient.request).toHaveBeenNthCalledWith(2, '/api/v1/student/attempts/attempt-1/monitoring-events', expect.objectContaining({ method: 'POST' }))
  })
})

