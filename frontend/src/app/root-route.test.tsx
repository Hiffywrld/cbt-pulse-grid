import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../features/auth/auth-context'
import { ThemeProvider } from '../features/theme/theme-provider'
import { AppRoutes } from './app-routes'

describe('root homepage route', () => {
  it('renders the public homepage for an unauthenticated visitor', () => {
    render(
      <ThemeProvider>
        <MemoryRouter initialEntries={['/']}>
          <AuthContext.Provider value={{
            status: 'anonymous',
            user: null,
            login: vi.fn(),
            logout: vi.fn(),
            refreshProfile: vi.fn(),
          }}>
            <AppRoutes />
          </AuthContext.Provider>
        </MemoryRouter>
      </ThemeProvider>,
    )

    expect(screen.getByRole('heading', { name: /examinations that keep working/i })).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /sign in/i })[0]).toHaveAttribute('href', '/login')
  })
})
