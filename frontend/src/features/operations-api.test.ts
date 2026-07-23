import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../lib/api/client'
import { auditApi } from './audit/audit-api'
import { monitoringApi } from './monitoring/monitoring-api'
import { webhooksApi } from './webhooks/webhooks-api'

vi.mock('../lib/api/client', () => ({ apiClient: { request: vi.fn() } }))
const request = vi.mocked(apiClient.request)

describe('Phase 5 API contracts', () => {
  beforeEach(() => request.mockReset())
  it('uses the exact monitoring and audit pagination/filter contracts', async () => {
    request.mockResolvedValue({} as never)
    await monitoringApi.dashboard('exam-1', 2)
    expect(request).toHaveBeenCalledWith('/api/v1/monitoring/exams/exam-1/dashboard?page=2&size=20')
    await auditApi.list({ action: 'EXAM_PUBLISHED', actorId: 'actor', page: 0, size: 20 })
    expect(request).toHaveBeenLastCalledWith('/api/v1/audit/events?action=EXAM_PUBLISHED&actorId=actor&page=0&size=20')
  })
  it('creates, rotates and retries using the backend webhook contract', async () => {
    request.mockResolvedValue({} as never)
    const body = { name: 'Monitor', destinationUrl: 'https://receiver.example/events', eventTypes: [] }
    await webhooksApi.create(body)
    expect(request).toHaveBeenCalledWith('/api/v1/webhooks/subscriptions', { method: 'POST', body })
    await webhooksApi.rotate('sub-1')
    expect(request).toHaveBeenLastCalledWith('/api/v1/webhooks/subscriptions/sub-1/rotate-secret', { method: 'POST' })
    await webhooksApi.retry('delivery-1')
    expect(request).toHaveBeenLastCalledWith('/api/v1/webhooks/deliveries/delivery-1/retry', { method: 'POST' })
  })
})
