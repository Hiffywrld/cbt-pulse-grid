import { describe, expect, it } from 'vitest'
import type { LiveMonitoringUpdate, MonitoringRow, OperationsPage } from '../../types/operations'
import { applyLiveUpdate } from './use-live-monitoring'

describe('live monitoring reconciliation', () => {
  it('replaces the matching REST row without changing pagination', () => {
    const row = { attemptId: 'a', candidateId: 'c', firstName: 'Old', lastName: 'Name', registrationNumber: null, candidateStatus: 'ACTIVE', attemptStatus: 'IN_PROGRESS', lastHeartbeatAt: null, connectivity: 'UNKNOWN', focused: null, fullscreen: null, eventCount: 0, riskScore: 0 } as MonitoringRow
    const page = { content: [row], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true } as OperationsPage<MonitoringRow>
    const update = { attemptId: 'a', candidateId: 'c', candidateFirstName: 'Amina', candidateLastName: 'Okafor', registrationNumber: 'ST-1', candidateStatus: 'ACTIVE', attemptStatus: 'IN_PROGRESS', lastHeartbeat: '2026-01-01T00:00:00Z', connectivity: 'ONLINE', focused: true, fullscreen: true, eventCount: 2, riskScore: 10, updateType: 'HEARTBEAT', serverTimestamp: '2026-01-01T00:00:01Z' } as LiveMonitoringUpdate
    const result = applyLiveUpdate(page, update)!
    expect(result.content[0]).toMatchObject({ firstName: 'Amina', connectivity: 'ONLINE', riskScore: 10 })
    expect(result.totalElements).toBe(1)
  })
})
