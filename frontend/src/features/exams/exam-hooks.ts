import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ExamInput } from '../../types/academic'
import { studentExamKeys } from '../student/student-exam-hooks'
import { examsApi, type ExamListParams } from './exams-api'

export const examKeys = {
  all: ['exams'] as const,
  list: (params: ExamListParams) => ['exams', 'list', params] as const,
  detail: (id: string) => ['exams', 'detail', id] as const,
  candidates: (id: string, page: number) => ['exams', 'candidates', id, page] as const,
}
export const useExams = (params: ExamListParams) =>
  useQuery({ queryKey: examKeys.list(params), queryFn: () => examsApi.list(params) })
export const useExam = (id?: string) =>
  useQuery({ queryKey: examKeys.detail(id ?? ''), queryFn: () => examsApi.get(id!), enabled: Boolean(id) })
export const useExamCandidates = (id: string, page: number) =>
  useQuery({ queryKey: examKeys.candidates(id, page), queryFn: () => examsApi.candidates(id, page, 20), enabled: Boolean(id) })
export const useExamMutations = () => {
  const client = useQueryClient()
  const refresh = async () => {
    await Promise.all([
      client.invalidateQueries({ queryKey: examKeys.all }),
      client.invalidateQueries({ queryKey: studentExamKeys.all }),
    ])
  }
  return {
    create: useMutation({ mutationFn: (body: ExamInput & { accessPin: string }) => examsApi.create(body), onSuccess: refresh }),
    update: useMutation({
      mutationFn: ({ id, body }: { id: string; body: Omit<ExamInput, 'accessPin'> }) => examsApi.update(id, body),
      onSuccess: refresh,
    }),
    publish: useMutation({ mutationFn: (id: string) => examsApi.publish(id), onSuccess: refresh }),
    cancel: useMutation({ mutationFn: (id: string) => examsApi.cancel(id), onSuccess: refresh }),
    close: useMutation({ mutationFn: (id: string) => examsApi.close(id), onSuccess: refresh }),
    assign: useMutation({
      mutationFn: ({ id, userIds }: { id: string; userIds: string[] }) => examsApi.assignCandidates(id, userIds),
      onSuccess: refresh,
    }),
    remove: useMutation({
      mutationFn: ({ id, userId }: { id: string; userId: string }) => examsApi.removeCandidate(id, userId),
      onSuccess: refresh,
    }),
  }
}
