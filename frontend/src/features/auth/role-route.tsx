import { Navigate, Outlet } from 'react-router-dom'
import type { Role } from '../../types/auth'
import { hasAnyRole } from './role-routing'
import { useAuth } from './use-auth'

export const RoleRoute = ({ roles }: { roles: readonly Role[] }) => {
  const { user } = useAuth()
  if (!user || !hasAnyRole(user, roles)) return <Navigate to="/unauthorized" replace />
  return <Outlet />
}
