import type { ApiClient } from '../../lib/api/api-client'
import { apiClient } from '../../lib/api/client'
import type {
  CurrentUser,
  LoginRequest,
  LogoutRequest,
  TokenResponse,
  UpdateProfileRequest,
  ChangePasswordRequest,
} from '../../types/auth'

export const createAuthApi = (client: ApiClient) => ({
  login(request: LoginRequest) {
    return client.request<TokenResponse>('/api/v1/auth/login', {
      method: 'POST',
      auth: false,
      body: request,
    })
  },
  me() {
    return client.request<CurrentUser>('/api/v1/auth/me')
  },
  updateProfile(request: UpdateProfileRequest) {
    return client.request<CurrentUser>('/api/v1/auth/profile', { method: 'PUT', body: request })
  },
  changePassword(request: ChangePasswordRequest) {
    return client.request<void>('/api/v1/auth/change-password', { method: 'POST', body: request })
  },
  logout(request: LogoutRequest) {
    return client.request<void>('/api/v1/auth/logout', {
      method: 'POST',
      auth: false,
      body: request,
    })
  },
})

export const authApi = createAuthApi(apiClient)
