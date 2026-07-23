import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import userEvent from '@testing-library/user-event'
import { AuthContext } from '../../features/auth/auth-context'
import { ThemeProvider } from '../../features/theme/theme-provider'
import type { CurrentUser } from '../../types/auth'
import { AppShell } from './app-shell'

const institutionAdmin: CurrentUser = {
  id: 'user-1',
  email: 'amina@niitlagos.local',
  firstName: 'Amina',
  lastName: 'Okafor',
  institutionId: '70d87f4e-4b94-45ef-84eb-4270c2337fa1',
  institutionName: 'NIIT Lagos Campus',
  institutionCode: 'NIIT-LAGOS',
  roles: ['INSTITUTION_ADMIN'],
}

describe('AppShell authenticated identity', () => {
  it('shows the full name, business role, and institution without a raw UUID', async () => {
    const user = userEvent.setup()
    render(
      <ThemeProvider>
        <AuthContext.Provider value={{ status: 'authenticated', user: institutionAdmin, login: vi.fn(), logout: vi.fn() }}>
          <MemoryRouter initialEntries={['/institution']}>
            <Routes>
              <Route element={<AppShell />}><Route path="/institution" element={<p>Dashboard content</p>} /></Route>
            </Routes>
          </MemoryRouter>
        </AuthContext.Provider>
      </ThemeProvider>,
    )

    expect(screen.getByText('Amina Okafor')).toBeInTheDocument()
    expect(screen.getByText('Institution Administrator')).toBeInTheDocument()
    expect(screen.getByText('NIIT Lagos Campus')).toBeInTheDocument()
    expect(screen.getByText('NIIT-LAGOS')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Open user menu' }))
    expect(screen.getByRole('combobox', { name: 'Color theme' })).toBeInTheDocument()
    expect(screen.queryByText(institutionAdmin.institutionId as string)).not.toBeInTheDocument()
  })
})
