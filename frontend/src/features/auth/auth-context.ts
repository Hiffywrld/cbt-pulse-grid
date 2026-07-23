import { createContext } from 'react'
import type { CurrentUser, LoginRequest } from '../../types/auth'

export type AuthStatus = 'restoring' | 'authenticated' | 'anonymous'

export type AuthContextValue = {
  status: AuthStatus
  user: CurrentUser | null
  login(request: LoginRequest): Promise<CurrentUser>
  logout(): Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
