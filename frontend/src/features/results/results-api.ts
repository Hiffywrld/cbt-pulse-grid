import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type {
  CandidateResult, CandidateResultStatus, ExamResultSummary, ResultPage, StaffAttemptResult,
} from '../../types/results'

export type CandidateResultParams = {
  search?: string
  status?: CandidateResultStatus | ''
  passed?: boolean | ''
  page: number
  size: number
}

export const resultsApi = {
  summary: (examId: string) =>
    apiClient.request<ExamResultSummary>(`/api/v1/results/exams/${examId}/summary`),
  candidates: (examId: string, params: CandidateResultParams) =>
    apiClient.request<ResultPage<CandidateResult>>(
      `/api/v1/results/exams/${examId}/candidates${queryString({
        ...params,
        passed: params.passed === '' ? undefined : String(params.passed),
      })}`,
    ),
  attempt: (attemptId: string) =>
    apiClient.request<StaffAttemptResult>(`/api/v1/results/attempts/${attemptId}`),
  exportCsv: (examId: string, params: Omit<CandidateResultParams, 'page' | 'size'>) =>
    apiClient.request<Blob>(
      `/api/v1/results/exams/${examId}/export.csv${queryString({
        ...params,
        passed: params.passed === '' ? undefined : String(params.passed),
      })}`,
      { responseType: 'blob' },
    ),
}
