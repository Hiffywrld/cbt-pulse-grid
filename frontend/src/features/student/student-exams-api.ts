import { apiClient } from '../../lib/api/client'
import type { StudentExamDetail, StudentExamSummary } from '../../types/management'

export const studentExamsApi = {
  list: () => apiClient.request<StudentExamSummary[]>('/api/v1/student/exams'),
  get: (examId: string) => apiClient.request<StudentExamDetail>(`/api/v1/student/exams/${examId}`),
}
