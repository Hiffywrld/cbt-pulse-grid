import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { institutionsApi, type InstitutionListParams } from './institutions-api'
import type { InstitutionStatus } from '../../types/management'

export const institutionKeys = { all: ['institutions'] as const, list: (params: InstitutionListParams) => ['institutions', 'list', params] as const }

export const useInstitutions = (params: InstitutionListParams, enabled = true) => useQuery({ queryKey: institutionKeys.list(params), queryFn: () => institutionsApi.list(params), enabled })

export const useInstitutionMutations = () => {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: institutionKeys.all })
  return {
    create: useMutation({ mutationFn: institutionsApi.create, onSuccess: refresh }),
    update: useMutation({ mutationFn: ({ id, name }: { id: string; name: string }) => institutionsApi.update(id, { name }), onSuccess: refresh }),
    status: useMutation({ mutationFn: ({ id, status }: { id: string; status: InstitutionStatus }) => institutionsApi.changeStatus(id, status), onSuccess: refresh }),
  }
}
