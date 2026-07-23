import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../../features/auth/auth-context'
import { ThemeProvider } from '../../features/theme/theme-provider'
import { UserMenu } from './user-menu'

const renderMenu = () => render(<ThemeProvider><MemoryRouter><AuthContext.Provider value={{
  status: 'authenticated',
  user: { id: 'u1', email: 'amina@example.test', firstName: 'Amina', lastName: 'Okafor', institutionId: 'i1', institutionName: 'NIIT Lagos Campus', roles: ['INSTITUTION_ADMIN'] },
  login: vi.fn(), logout: vi.fn(),
}}><UserMenu /></AuthContext.Provider></MemoryRouter></ThemeProvider>)

describe('user menu', () => {
  it('opens accessibly and Escape returns focus', async () => {
    const user = userEvent.setup(); renderMenu()
    const trigger = screen.getByRole('button', { name: 'Open user menu' })
    await user.click(trigger)
    expect(screen.getByRole('menu')).toBeInTheDocument()
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    expect(trigger).toHaveFocus()
  })
  it('closes when clicking outside', async () => {
    const user = userEvent.setup(); renderMenu()
    await user.click(screen.getByRole('button', { name: 'Open user menu' }))
    fireEvent.pointerDown(document.body)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })
})
