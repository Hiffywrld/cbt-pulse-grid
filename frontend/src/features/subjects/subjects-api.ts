import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type { AcademicPage, Subject, SubjectInput, SubjectStatus } from '../../types/academic'

export type SubjectListParams = { search?: string; status?: SubjectStatus | ''; page: number; size: number }

export const subjectsApi = {
  list: (params: SubjectListParams) =>
    apiClient.request<AcademicPage<Subject>>(`/api/v1/subjects${queryString(params)}`),
  get: (id: string) => apiClient.request<Subject>(`/api/v1/subjects/${id}`),
  create: (body: SubjectInput) =>
    apiClient.request<Subject>('/api/v1/subjects', { method: 'POST', body }),
  update: (id: string, body: SubjectInput) =>
    apiClient.request<Subject>(`/api/v1/subjects/${id}`, { method: 'PUT', body }),
  changeStatus: (id: string, status: SubjectStatus) =>
    apiClient.request<Subject>(`/api/v1/subjects/${id}/status`, { method: 'PATCH', body: { status } }),
}
