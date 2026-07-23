import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { UserStatus } from '../../types/management'
import { usersApi, type CreateUserBody, type UpdateUserBody, type UserListParams } from './users-api'

export const userKeys = { all: ['users'] as const, list: (params: UserListParams) => ['users', 'list', params] as const }
export const useUsers = (params: UserListParams, enabled = true) => useQuery({ queryKey: userKeys.list(params), queryFn: () => usersApi.list(params), enabled })
export const useUserMutations = () => {
  const client = useQueryClient()
  const refresh = () => client.invalidateQueries({ queryKey: userKeys.all })
  return {
    create: useMutation({ mutationFn: (body: CreateUserBody) => usersApi.create(body), onSuccess: refresh }),
    update: useMutation({ mutationFn: ({ id, body }: { id: string; body: UpdateUserBody }) => usersApi.update(id, body), onSuccess: refresh }),
    status: useMutation({ mutationFn: ({ id, status }: { id: string; status: UserStatus }) => usersApi.changeStatus(id, status), onSuccess: refresh }),
  }
}
