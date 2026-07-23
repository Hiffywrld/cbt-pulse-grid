import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type {
  AcademicPage, ExamCandidate, ExamDetail, ExamInput, ExamStatus, ExamSummary,
} from '../../types/academic'

export type ExamListParams = {
  search?: string
  subjectId?: string
  status?: ExamStatus | ''
  page: number
  size: number
}

type UpdateExamBody = Omit<ExamInput, 'accessPin'>

export const examsApi = {
  list: (params: ExamListParams) =>
    apiClient.request<AcademicPage<ExamSummary>>(`/api/v1/exams${queryString(params)}`),
  get: (id: string) => apiClient.request<ExamDetail>(`/api/v1/exams/${id}`),
  create: (body: ExamInput & { accessPin: string }) =>
    apiClient.request<ExamDetail>('/api/v1/exams', { method: 'POST', body }),
  update: (id: string, body: UpdateExamBody) =>
    apiClient.request<ExamDetail>(`/api/v1/exams/${id}`, { method: 'PUT', body }),
  publish: (id: string) =>
    apiClient.request<ExamDetail>(`/api/v1/exams/${id}/publish`, { method: 'POST' }),
  cancel: (id: string) =>
    apiClient.request<ExamDetail>(`/api/v1/exams/${id}/cancel`, { method: 'POST' }),
  close: (id: string) =>
    apiClient.request<ExamDetail>(`/api/v1/exams/${id}/close`, { method: 'POST' }),
  rotatePin: (id: string, accessPin: string) =>
    apiClient.request<ExamDetail>(`/api/v1/exams/${id}/access-pin`, { method: 'PUT', body: { accessPin } }),
  candidates: (id: string, page: number, size: number) =>
    apiClient.request<AcademicPage<ExamCandidate>>(`/api/v1/exams/${id}/candidates${queryString({ page, size })}`),
  assignCandidates: (id: string, userIds: string[]) =>
    apiClient.request<ExamCandidate[]>(`/api/v1/exams/${id}/candidates`, { method: 'POST', body: { userIds } }),
  removeCandidate: (id: string, userId: string) =>
    apiClient.request<void>(`/api/v1/exams/${id}/candidates/${userId}`, { method: 'DELETE' }),
}
