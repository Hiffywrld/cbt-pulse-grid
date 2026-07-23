import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type { MonitoringEventType, OperationsPage, WebhookDelivery, WebhookDeliveryStatus, WebhookSecret, WebhookSubscription } from '../../types/operations'

export const webhooksApi = {
  subscriptions: (page: number, size = 20) => apiClient.request<OperationsPage<WebhookSubscription>>(`/api/v1/webhooks/subscriptions${queryString({ page, size })}`),
  create: (body: { name: string; destinationUrl: string; eventTypes: MonitoringEventType[] }) => apiClient.request<WebhookSecret>('/api/v1/webhooks/subscriptions', { method: 'POST', body }),
  status: (id: string, status: 'ACTIVE' | 'PAUSED') => apiClient.request<WebhookSubscription>(`/api/v1/webhooks/subscriptions/${id}/status`, { method: 'PATCH', body: { status } }),
  rotate: (id: string) => apiClient.request<WebhookSecret>(`/api/v1/webhooks/subscriptions/${id}/rotate-secret`, { method: 'POST' }),
  deliveries: (status: WebhookDeliveryStatus | '', page: number, size = 20) => apiClient.request<OperationsPage<WebhookDelivery>>(`/api/v1/webhooks/deliveries${queryString({ status, page, size })}`),
  retry: (id: string) => apiClient.request<WebhookDelivery>(`/api/v1/webhooks/deliveries/${id}/retry`, { method: 'POST' }),
}
