import { ArrowLeft, Home, ShieldX } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from '../components/ui/button'
import { homeForUser } from '../features/auth/role-routing'
import { useAuth } from '../features/auth/use-auth'

export const UnauthorizedPage = () => {
  const { user } = useAuth()
  return <main className="error-page"><span className="error-page__code">403</span><ShieldX size={44} aria-hidden="true" /><h1>This workspace is outside your role</h1><p>Your account is signed in, but it does not have access to this area.</p><Button icon={<ArrowLeft size={17} />} onClick={() => window.history.back()}>Go back</Button>{user ? <Link to={homeForUser(user)}>Return to your dashboard</Link> : null}</main>
}

export const NotFoundPage = () => {
  const { user } = useAuth()
  return <main className="error-page"><span className="error-page__code">404</span><Home size={42} aria-hidden="true" /><h1>Page not found</h1><p>The requested page does not exist in this workspace.</p><Link className="button button--primary button--md" to={user ? homeForUser(user) : '/login'}>Return to safety</Link></main>
}
