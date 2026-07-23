import { useQuery } from '@tanstack/react-query'
import { monitoringApi } from './monitoring-api'

export const monitoringKeys = {
  all: ['monitoring'] as const,
  dashboard: (examId: string, page: number) => ['monitoring', 'dashboard', examId, page] as const,
  events: (attemptId: string, page: number) => ['monitoring', 'events', attemptId, page] as const,
}
export const useMonitoringDashboard = (examId: string, page: number) =>
  useQuery({ queryKey: monitoringKeys.dashboard(examId, page), queryFn: () => monitoringApi.dashboard(examId, page), enabled: Boolean(examId), refetchInterval: 30_000 })
export const useMonitoringEvents = (attemptId: string, page: number) =>
  useQuery({ queryKey: monitoringKeys.events(attemptId, page), queryFn: () => monitoringApi.events(attemptId, page), enabled: Boolean(attemptId) })
