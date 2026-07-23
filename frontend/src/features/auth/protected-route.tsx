import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { FullPageSpinner } from '../../components/feedback/spinner'
import { useAuth } from './use-auth'

export const ProtectedRoute = () => {
  const { status } = useAuth()
  const location = useLocation()
  if (status === 'restoring') {
    return <FullPageSpinner label="Restoring your secure session" />
  }
  if (status !== 'authenticated') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}
