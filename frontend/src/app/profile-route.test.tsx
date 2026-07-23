import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../features/auth/auth-context'
import { ThemeProvider } from '../features/theme/theme-provider'
import type { Role } from '../types/auth'
import { AppRoutes } from './app-routes'

const roles: Role[] = ['SUPER_ADMIN', 'INSTITUTION_ADMIN', 'EXAMINER', 'INVIGILATOR', 'STUDENT']

const renderRoute = (role?: Role) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<ThemeProvider><QueryClientProvider client={queryClient}><MemoryRouter initialEntries={['/profile']}><AuthContext.Provider value={{
    status: role ? 'authenticated' : 'anonymous',
    user: role ? { id: `user-${role}`, email: `${role.toLowerCase()}@example.test`, firstName: 'Profile', lastName: 'User', institutionId: role === 'SUPER_ADMIN' ? null : 'institution-1', roles: [role] } : null,
    login: vi.fn(), logout: vi.fn(), refreshProfile: vi.fn(),
  }}><AppRoutes /></AuthContext.Provider></MemoryRouter></QueryClientProvider></ThemeProvider>)
}

describe('authenticated profile route', () => {
  it.each(roles)('allows %s without a role-specific guard', (role) => {
    renderRoute(role)
    expect(screen.getByRole('heading', { name: 'Profile' })).toBeInTheDocument()
    expect(screen.queryByText(/outside your role/i)).not.toBeInTheDocument()
  })

  it('redirects an unauthenticated visitor to login', () => {
    renderRoute()
    expect(screen.getByRole('heading', { name: /sign in to your workspace/i })).toBeInTheDocument()
  })
})
