import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../../lib/api/client'
import { resultsApi } from './results-api'

vi.mock('../../lib/api/client', () => ({ apiClient: { request: vi.fn() } }))

describe('staff result API contract', () => {
  beforeEach(() => vi.clearAllMocks())

  it('uses summary, filtered candidate and safe review endpoints', async () => {
    vi.mocked(apiClient.request).mockResolvedValue({})
    await resultsApi.summary('exam-1')
    await resultsApi.candidates('exam-1', { search: 'tobi', status: 'SUBMITTED', passed: true, page: 0, size: 20 })
    await resultsApi.attempt('attempt-1')
    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/results/exams/exam-1/summary')
    expect(apiClient.request).toHaveBeenNthCalledWith(2, '/api/v1/results/exams/exam-1/candidates?search=tobi&status=SUBMITTED&passed=true&page=0&size=20')
    expect(apiClient.request).toHaveBeenNthCalledWith(3, '/api/v1/results/attempts/attempt-1')
  })

  it('requests the real CSV as a blob', async () => {
    vi.mocked(apiClient.request).mockResolvedValue(new Blob())
    await resultsApi.exportCsv('exam-1', { status: 'AUTO_SUBMITTED', passed: false })
    expect(apiClient.request).toHaveBeenCalledWith(
      '/api/v1/results/exams/exam-1/export.csv?status=AUTO_SUBMITTED&passed=false',
      { responseType: 'blob' },
    )
  })
})

