import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type {
  AcademicPage, QuestionDifficulty, QuestionInput, QuestionStatus, QuestionSummary,
  QuestionType, StaffQuestion,
} from '../../types/academic'

export type QuestionListParams = {
  subjectId?: string
  type?: QuestionType | ''
  difficulty?: QuestionDifficulty | ''
  status?: QuestionStatus | ''
  search?: string
  page: number
  size: number
}

export const questionsApi = {
  list: (params: QuestionListParams) =>
    apiClient.request<AcademicPage<QuestionSummary>>(`/api/v1/questions${queryString(params)}`),
  get: (id: string) => apiClient.request<StaffQuestion>(`/api/v1/questions/${id}`),
  create: (body: QuestionInput) =>
    apiClient.request<StaffQuestion>('/api/v1/questions', { method: 'POST', body }),
  update: (id: string, body: QuestionInput) =>
    apiClient.request<StaffQuestion>(`/api/v1/questions/${id}`, { method: 'PUT', body }),
  changeStatus: (id: string, status: QuestionStatus) =>
    apiClient.request<StaffQuestion>(`/api/v1/questions/${id}/status`, { method: 'PATCH', body: { status } }),
}
