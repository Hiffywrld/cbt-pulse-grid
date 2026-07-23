import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { SubjectInput, SubjectStatus } from '../../types/academic'
import { subjectsApi, type SubjectListParams } from './subjects-api'

export const subjectKeys = {
  all: ['subjects'] as const,
  list: (params: SubjectListParams) => ['subjects', 'list', params] as const,
  detail: (id: string) => ['subjects', 'detail', id] as const,
}

export const useSubjects = (params: SubjectListParams, enabled = true) =>
  useQuery({ queryKey: subjectKeys.list(params), queryFn: () => subjectsApi.list(params), enabled })

export const useSubject = (id?: string) =>
  useQuery({ queryKey: subjectKeys.detail(id ?? ''), queryFn: () => subjectsApi.get(id!), enabled: Boolean(id) })

export const useSubjectMutations = () => {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: subjectKeys.all })
  return {
    create: useMutation({ mutationFn: subjectsApi.create, onSuccess: refresh }),
    update: useMutation({
      mutationFn: ({ id, body }: { id: string; body: SubjectInput }) => subjectsApi.update(id, body),
      onSuccess: refresh,
    }),
    status: useMutation({
      mutationFn: ({ id, status }: { id: string; status: SubjectStatus }) => subjectsApi.changeStatus(id, status),
      onSuccess: refresh,
    }),
  }
}
