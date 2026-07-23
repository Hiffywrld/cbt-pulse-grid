import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { AuthContextValue } from './auth-context'
import { AuthContext } from './auth-context'
import { ProtectedRoute } from './protected-route'
import { RoleRoute } from './role-route'

const renderWithAuth = (value: AuthContextValue, initialPath: string) => render(
  <AuthContext.Provider value={value}><MemoryRouter initialEntries={[initialPath]}><Routes>
    <Route path="/login" element={<h1>Login destination</h1>} /><Route path="/unauthorized" element={<h1>Unauthorized destination</h1>} />
    <Route element={<ProtectedRoute />}><Route element={<RoleRoute roles={['INSTITUTION_ADMIN']} />}><Route path="/secure" element={<h1>Secure page</h1>} /></Route></Route>
  </Routes></MemoryRouter></AuthContext.Provider>,
)

describe('route guards', () => {
  it('redirects anonymous users away from protected routes', () => {
    renderWithAuth({ status: 'anonymous', user: null, login: vi.fn(), logout: vi.fn() }, '/secure')
    expect(screen.getByRole('heading', { name: 'Login destination' })).toBeInTheDocument()
  })

  it('rejects an authenticated user without the required role', () => {
    renderWithAuth({ status: 'authenticated', user: { id: '1', email: 'student@example.edu', institutionId: 'i1', roles: ['STUDENT'] }, login: vi.fn(), logout: vi.fn() }, '/secure')
    expect(screen.getByRole('heading', { name: 'Unauthorized destination' })).toBeInTheDocument()
  })
})
