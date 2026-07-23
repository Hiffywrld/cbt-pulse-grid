import { apiClient } from '../../lib/api/client'
import { queryString } from '../../lib/api/query-string'
import type { Role } from '../../types/auth'
import type { ManagedUser, PageResponse, UserStatus } from '../../types/management'

export type UserListParams = { search?: string; institutionId?: string; role?: Role | ''; status?: UserStatus | ''; page: number; size: number }
export type CreateUserBody = { firstName: string; lastName: string; email: string; password: string; institutionId?: string; roles: Role[]; registrationNumber?: string | null }
export type UpdateUserBody = { firstName: string; lastName: string; registrationNumber?: string | null }

export const usersApi = {
  list: (params: UserListParams) => apiClient.request<PageResponse<ManagedUser>>(`/api/v1/users${queryString(params)}`),
  get: (id: string) => apiClient.request<ManagedUser>(`/api/v1/users/${id}`),
  create: (body: CreateUserBody) => apiClient.request<ManagedUser>('/api/v1/users', { method: 'POST', body }),
  update: (id: string, body: UpdateUserBody) => apiClient.request<ManagedUser>(`/api/v1/users/${id}`, { method: 'PUT', body }),
  changeStatus: (id: string, status: UserStatus) => apiClient.request<ManagedUser>(`/api/v1/users/${id}/status`, { method: 'PATCH', body: { status } }),
}
