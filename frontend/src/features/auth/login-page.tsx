import { zodResolver } from '@hookform/resolvers/zod'
import { ArrowRight, CheckCircle2, ShieldCheck, WifiOff } from 'lucide-react'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Alert } from '../../components/feedback/alert'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { PasswordInput } from '../../components/ui/password-input'
import { isApiClientError } from '../../lib/api/api-error'
import { homeForUser, postLoginDestination } from './role-routing'
import { useAuth } from './use-auth'

const loginSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email address').max(254),
  password: z.string().min(1, 'Password is required').max(200),
})

type LoginValues = z.infer<typeof loginSchema>

export const LoginPage = () => {
  const { login, status, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
  })

  if (status === 'authenticated' && user) return <Navigate to={homeForUser(user)} replace />

  const onSubmit = handleSubmit(async (values) => {
    setError(null)
    try {
      const profile = await login(values)
      const from = (location.state as { from?: unknown } | null)?.from
      navigate(postLoginDestination(profile, from), { replace: true })
    } catch (failure) {
      setError(isApiClientError(failure) ? failure.message : 'Sign in could not be completed. Please try again.')
    }
  })

  return (
    <main className="login-page">
      <section className="login-story" aria-label="Platform introduction">
        <div className="login-story__glow" />
        <a className="brand brand--light" href="/login" aria-label="CBT-Pulse Grid login">
          <span className="brand-mark" aria-hidden="true"><span /></span>
          <span><strong>CBT-Pulse</strong><small>GRID</small></span>
        </a>
        <div className="login-story__content">
          <p className="eyebrow eyebrow--cyan">Institutional examination infrastructure</p>
          <h1>Every exam signal.<br />One secure grid.</h1>
          <p>Coordinate high-concurrency assessments across connected or offline-first institutional networks.</p>
          <ul className="trust-list">
            <li><ShieldCheck aria-hidden="true" /> Role-bound access and secure sessions</li>
            <li><WifiOff aria-hidden="true" /> Built for resilient local-area operation</li>
            <li><CheckCircle2 aria-hidden="true" /> Server-authoritative exam workflows</li>
          </ul>
        </div>
        <div className="pulse-visual" aria-hidden="true"><span /><span /><span /><i /></div>
        <p className="login-story__foot">Secure assessment operations · CBT-Pulse Grid</p>
      </section>
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-card">
          <div className="login-card__heading">
            <span className="login-card__mobile-brand brand-mark" aria-hidden="true"><span /></span>
            <p className="eyebrow">Welcome back</p>
            <h2 id="login-title">Sign in to your workspace</h2>
            <p>Use the institutional account assigned to you.</p>
          </div>
          {error ? <Alert tone="error" title="Sign in failed">{error}</Alert> : null}
          <form className="login-form" onSubmit={onSubmit} noValidate>
            <Input label="Email address" type="email" autoComplete="username" placeholder="name@institution.edu" error={errors.email?.message} {...register('email')} />
            <PasswordInput label="Password" autoComplete="current-password" placeholder="Enter your password" error={errors.password?.message} {...register('password')} />
            <Button type="submit" size="lg" loading={isSubmitting} disabled={isSubmitting}>Sign in securely <ArrowRight size={18} aria-hidden="true" /></Button>
          </form>
          <p className="login-help">Account access is managed by your institution. Contact an administrator if you cannot sign in.</p>
        </div>
      </section>
    </main>
  )
}
