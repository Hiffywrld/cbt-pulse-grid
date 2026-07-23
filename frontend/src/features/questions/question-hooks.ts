import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { QuestionInput, QuestionStatus } from '../../types/academic'
import { questionsApi, type QuestionListParams } from './questions-api'

export const questionKeys = {
  all: ['questions'] as const,
  list: (params: QuestionListParams) => ['questions', 'list', params] as const,
  detail: (id: string) => ['questions', 'detail', id] as const,
}
export const useQuestions = (params: QuestionListParams) =>
  useQuery({ queryKey: questionKeys.list(params), queryFn: () => questionsApi.list(params) })
export const useQuestion = (id?: string) =>
  useQuery({ queryKey: questionKeys.detail(id ?? ''), queryFn: () => questionsApi.get(id!), enabled: Boolean(id) })
export const useQuestionMutations = () => {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: questionKeys.all })
  return {
    create: useMutation({ mutationFn: questionsApi.create, onSuccess: refresh }),
    update: useMutation({
      mutationFn: ({ id, body }: { id: string; body: QuestionInput }) => questionsApi.update(id, body),
      onSuccess: refresh,
    }),
    status: useMutation({
      mutationFn: ({ id, status }: { id: string; status: QuestionStatus }) => questionsApi.changeStatus(id, status),
      onSuccess: refresh,
    }),
  }
}
