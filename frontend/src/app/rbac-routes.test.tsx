import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../features/auth/auth-context'
import { ThemeProvider } from '../features/theme/theme-provider'
import type { Role } from '../types/auth'
import { AppRoutes } from './app-routes'

const renderPath = (role: Role, path: string) => {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[path]}>
          <AuthContext.Provider value={{
            status: 'authenticated',
            user: {
              id: `user-${role}`,
              email: `${role.toLowerCase()}@example.test`,
              institutionId: role === 'SUPER_ADMIN' ? null : 'institution-1',
              roles: [role],
            },
            login: vi.fn(),
            logout: vi.fn(),
            refreshProfile: vi.fn(),
          }}>
            <AppRoutes />
          </AuthContext.Provider>
        </MemoryRouter>
      </QueryClientProvider>
    </ThemeProvider>,
  )
}

describe('application RBAC routes', () => {
  it.each([
    ['SUPER_ADMIN', '/institution/questions'],
    ['EXAMINER', '/institution/monitoring'],
    ['INVIGILATOR', '/institution/results'],
    ['INVIGILATOR', '/institution/results/exams/exam-1'],
    ['INVIGILATOR', '/institution/results/attempts/attempt-1'],
    ['STUDENT', '/institution/exams'],
  ] as const)('rejects %s from %s', (role, path) => {
    renderPath(role, path)
    expect(screen.getByText(/outside your role/i)).toBeInTheDocument()
  })
})
