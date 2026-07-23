import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { browserSessionStore } from '../../lib/storage/session-storage'
import type { CurrentUser, TokenResponse } from '../../types/auth'
import { authApi } from './auth-api'
import { AuthProvider } from './auth-provider'
import { useAuth } from './use-auth'

vi.mock('./auth-api', () => ({ authApi: { login: vi.fn(), me: vi.fn(), logout: vi.fn() } }))

const tokens: TokenResponse = { accessToken: 'access', refreshToken: 'refresh', tokenType: 'Bearer', expiresAt: '2030-01-01T10:00:00Z' }
const profile: CurrentUser = { id: 'user-1', email: 'examiner@example.edu', institutionId: 'institution-1', roles: ['EXAMINER'] }

const Probe = () => {
  const { status, user, login, logout } = useAuth()
  const location = useLocation()
  return <div><span>{status}</span><span>{user?.email}</span><span>{location.pathname}</span><button onClick={() => void login({ email: 'admin@example.edu', password: 'CorrectPassword1!' })}>Log in now</button><button onClick={() => void logout()}>Log out now</button></div>
}

describe('AuthProvider', () => {
  beforeEach(() => {
    browserSessionStore.clear()
    vi.mocked(authApi.login).mockReset()
    vi.mocked(authApi.me).mockReset()
    vi.mocked(authApi.logout).mockReset()
  })

  it('stores successful login tokens and loads the authenticated profile', async () => {
    vi.mocked(authApi.login).mockResolvedValue(tokens)
    vi.mocked(authApi.me).mockResolvedValue(profile)
    render(<MemoryRouter><AuthProvider><Probe /></AuthProvider></MemoryRouter>)

    await userEvent.click(screen.getByRole('button', { name: 'Log in now' }))

    expect(await screen.findByText(profile.email)).toBeInTheDocument()
    expect(screen.getByText('authenticated')).toBeInTheDocument()
    expect(authApi.login).toHaveBeenCalledWith({ email: 'admin@example.edu', password: 'CorrectPassword1!' })
    expect(authApi.me).toHaveBeenCalledOnce()
    expect(browserSessionStore.read()).toEqual(tokens)
  })

  it('restores a stored session through the authenticated /me profile', async () => {
    browserSessionStore.write(tokens)
    vi.mocked(authApi.me).mockResolvedValue(profile)
    render(<MemoryRouter><AuthProvider><Probe /></AuthProvider></MemoryRouter>)
    expect(screen.getByText('restoring')).toBeInTheDocument()
    expect(await screen.findByText(profile.email)).toBeInTheDocument()
    expect(screen.getByText('authenticated')).toBeInTheDocument()
  })

  it('revokes where possible and always clears local state during logout', async () => {
    browserSessionStore.write(tokens)
    vi.mocked(authApi.me).mockResolvedValue(profile)
    vi.mocked(authApi.logout).mockRejectedValue(new Error('offline'))
    render(<MemoryRouter initialEntries={['/institution']}><AuthProvider><Probe /></AuthProvider></MemoryRouter>)
    await screen.findByText(profile.email)
    await userEvent.click(screen.getByRole('button', { name: 'Log out now' }))
    await waitFor(() => expect(browserSessionStore.read()).toBeNull())
    expect(authApi.logout).toHaveBeenCalledWith({ refreshToken: 'refresh' })
    expect(screen.getByText('/login')).toBeInTheDocument()
    expect(screen.getByText('anonymous')).toBeInTheDocument()
  })
})
