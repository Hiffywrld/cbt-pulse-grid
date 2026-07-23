import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type { MonitoringEvent, MonitoringRow, OperationsPage } from '../../types/operations'

export const monitoringApi = {
  dashboard: (examId: string, page: number, size = 20) =>
    apiClient.request<OperationsPage<MonitoringRow>>(`/api/v1/monitoring/exams/${examId}/dashboard${queryString({ page, size })}`),
  events: (attemptId: string, page: number, size = 20) =>
    apiClient.request<OperationsPage<MonitoringEvent>>(`/api/v1/monitoring/attempts/${attemptId}/events${queryString({ page, size })}`),
}
