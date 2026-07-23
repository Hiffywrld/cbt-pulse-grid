import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiClientError } from '../../lib/api/api-error'
import type { CurrentUser } from '../../types/auth'
import { AuthContext, type AuthContextValue } from './auth-context'
import { LoginPage } from './login-page'

const superAdmin: CurrentUser = { id: 'user-1', email: 'admin@example.edu', institutionId: null, roles: ['SUPER_ADMIN'] }

const renderLogin = (login: AuthContextValue['login']) => render(
  <AuthContext.Provider value={{ status: 'anonymous', user: null, login, logout: vi.fn() }}>
    <MemoryRouter initialEntries={['/login']}>
      <Routes><Route path="/login" element={<LoginPage />} /><Route path="/platform" element={<h1>Platform dashboard</h1>} /></Routes>
    </MemoryRouter>
  </AuthContext.Provider>,
)

describe('LoginPage', () => {
  it('logs in successfully and redirects a super administrator to the platform dashboard', async () => {
    const login = vi.fn().mockResolvedValue(superAdmin)
    renderLogin(login)
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'admin@example.edu' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'CorrectPassword1!' } })
    fireEvent.click(screen.getByRole('button', { name: /sign in securely/i }))
    expect(await screen.findByRole('heading', { name: 'Platform dashboard' })).toBeInTheDocument()
    expect(login).toHaveBeenCalledWith({ email: 'admin@example.edu', password: 'CorrectPassword1!' })
  })

  it('shows the backend invalid-login error and does not navigate', async () => {
    const login = vi.fn().mockRejectedValue(new ApiClientError({ status: 401, error: 'Unauthorized', message: 'Invalid email or password', validationErrors: {} }))
    renderLogin(login)
    fireEvent.change(screen.getByLabelText('Email address'), { target: { value: 'admin@example.edu' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong' } })
    fireEvent.click(screen.getByRole('button', { name: /sign in securely/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(screen.getByRole('heading', { name: 'Sign in to your workspace' })).toBeInTheDocument()
  })
})
