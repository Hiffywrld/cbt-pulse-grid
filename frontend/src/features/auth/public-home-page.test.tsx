import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from './auth-context'
import { PublicHomePage } from './public-home-page'
import { ThemeProvider } from '../theme/theme-provider'

const renderHome = (authenticated = false) => render(<ThemeProvider><MemoryRouter><AuthContext.Provider value={{
  status: authenticated ? 'authenticated' : 'anonymous',
  user: authenticated ? { id: 'u1', email: 'admin@example.test', institutionId: null, roles: ['SUPER_ADMIN'] } : null,
  login: vi.fn(), logout: vi.fn(),
}}><PublicHomePage /></AuthContext.Provider></MemoryRouter></ThemeProvider>)

describe('public homepage', () => {
  it('offers sign in without public registration', () => {
    renderHome()
    expect(screen.getAllByRole('link', { name: /sign in/i })[0]).toHaveAttribute('href', '/login')
    expect(screen.queryByText(/register|sign up/i)).not.toBeInTheDocument()
  })
  it('offers the authenticated role dashboard', () => {
    renderHome(true)
    expect(screen.getAllByRole('link', { name: /open dashboard/i })[0]).toHaveAttribute('href', '/platform')
  })
})
