import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type { Institution, InstitutionStatus, PageResponse } from '../../types/management'

export type InstitutionListParams = { search?: string; status?: InstitutionStatus | ''; page: number; size: number }

export const institutionsApi = {
  list: (params: InstitutionListParams) => apiClient.request<PageResponse<Institution>>(`/api/v1/institutions${queryString(params)}`),
  get: (id: string) => apiClient.request<Institution>(`/api/v1/institutions/${id}`),
  create: (body: { name: string; code: string }) => apiClient.request<Institution>('/api/v1/institutions', { method: 'POST', body }),
  update: (id: string, body: { name: string }) => apiClient.request<Institution>(`/api/v1/institutions/${id}`, { method: 'PUT', body }),
  changeStatus: (id: string, status: InstitutionStatus) => apiClient.request<Institution>(`/api/v1/institutions/${id}/status`, { method: 'PATCH', body: { status } }),
}
