import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type { AuditEvent, OperationsPage } from '../../types/operations'

export type AuditFilters = {
  action?: string; resourceType?: string; actorId?: string; from?: string; to?: string
  page: number; size: number
}
export const auditApi = {
  list: (filters: AuditFilters) =>
    apiClient.request<OperationsPage<AuditEvent>>(`/api/v1/audit/events${queryString(filters)}`),
}
