import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { useAuth } from '../auth/use-auth'
import { useInstitutions } from '../institutions/institution-hooks'
import { useUserMutations, useUsers } from './user-hooks'
import { UsersPage } from './users-page'

vi.mock('../auth/use-auth', () => ({ useAuth: vi.fn() }))
vi.mock('../institutions/institution-hooks', () => ({ useInstitutions: vi.fn() }))
vi.mock('./user-hooks', () => ({ useUsers: vi.fn(), useUserMutations: vi.fn() }))

const page = { content: [{ id: 'student-1', firstName: 'Tobi', lastName: 'Adeleke', email: 'tobi@example.edu', institutionId: 'institution-1', roles: ['STUDENT'] as const, registrationNumber: 'NIIT-STU-001', status: 'ACTIVE' as const, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', version: 0 }], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true }

describe('UsersPage tenant and role presentation', () => {
  it('opens and submits the platform administrator form using the backend contract', async () => {
    const createMutation = { mutateAsync: vi.fn().mockResolvedValue(page.content[0]) }
    vi.mocked(useAuth).mockReturnValue({ status: 'authenticated', user: { id: 'super-1', email: 'admin@cbtpulse.local', institutionId: null, roles: ['SUPER_ADMIN'] }, login: vi.fn(), logout: vi.fn() })
    vi.mocked(useInstitutions).mockReturnValue({ isPending: false, data: { content: [{ id: 'institution-1', name: 'NIIT Lagos Campus', code: 'NIIT-LAGOS', status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', version: 0 }], page: 0, size: 100, totalElements: 1, totalPages: 1, first: true, last: true } } as ReturnType<typeof useInstitutions>)
    vi.mocked(useUsers).mockReturnValue({ isPending: false, isError: false, data: page } as unknown as ReturnType<typeof useUsers>)
    vi.mocked(useUserMutations).mockReturnValue({ create: createMutation, update: { mutateAsync: vi.fn() }, status: { mutateAsync: vi.fn(), isPending: false } } as unknown as ReturnType<typeof useUserMutations>)
    render(<UsersPage mode="platform-admins" />)

    const newAccount = screen.getByRole('button', { name: 'New account' })
    expect(newAccount).toBeEnabled()
    await userEvent.click(newAccount)
    const dialog = screen.getByRole('dialog', { name: 'Create institution administrator' })
    expect(dialog).toBeVisible()
    await userEvent.type(within(dialog).getByLabelText('First name'), 'Amina')
    await userEvent.type(within(dialog).getByLabelText('Last name'), 'Okafor')
    await userEvent.type(within(dialog).getByLabelText('Email'), 'amina@niitlagos.local')
    await userEvent.type(within(dialog).getByLabelText('Temporary password'), 'Secure123!')
    await userEvent.selectOptions(within(dialog).getByLabelText('Institution'), 'institution-1')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create account' }))

    await waitFor(() => expect(createMutation.mutateAsync).toHaveBeenCalledWith({
      firstName: 'Amina',
      lastName: 'Okafor',
      email: 'amina@niitlagos.local',
      password: 'Secure123!',
      institutionId: 'institution-1',
      roles: ['INSTITUTION_ADMIN'],
      registrationNumber: null,
    }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Create institution administrator' })).not.toBeInTheDocument())
  })

  it('opens edit and status actions for an existing account', async () => {
    vi.mocked(useAuth).mockReturnValue({ status: 'authenticated', user: { id: 'admin-1', email: 'admin@example.edu', institutionId: 'institution-1', roles: ['INSTITUTION_ADMIN'] }, login: vi.fn(), logout: vi.fn() })
    vi.mocked(useInstitutions).mockReturnValue({ data: undefined } as ReturnType<typeof useInstitutions>)
    vi.mocked(useUsers).mockReturnValue({ isPending: false, isError: false, data: page } as unknown as ReturnType<typeof useUsers>)
    vi.mocked(useUserMutations).mockReturnValue({ create: { mutateAsync: vi.fn() }, update: { mutateAsync: vi.fn() }, status: { mutateAsync: vi.fn(), isPending: false } } as unknown as ReturnType<typeof useUserMutations>)
    render(<UsersPage mode="institution" />)

    await userEvent.click(screen.getByRole('button', { name: 'Edit' }))
    expect(screen.getByRole('dialog', { name: 'Edit user profile' })).toBeVisible()
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    await userEvent.click(screen.getByRole('button', { name: 'Suspend' }))
    expect(screen.getByRole('dialog', { name: 'Suspend account?' })).toBeVisible()
  })

  it('shows student registration numbers and no tenant selector to institution staff', () => {
    vi.mocked(useAuth).mockReturnValue({ status: 'authenticated', user: { id: 'admin-1', email: 'admin@example.edu', institutionId: 'institution-1', roles: ['INSTITUTION_ADMIN'] }, login: vi.fn(), logout: vi.fn() })
    vi.mocked(useInstitutions).mockReturnValue({ data: undefined } as ReturnType<typeof useInstitutions>)
    vi.mocked(useUsers).mockReturnValue({ isPending: false, isError: false, data: page } as unknown as ReturnType<typeof useUsers>)
    vi.mocked(useUserMutations).mockReturnValue({ create: { mutateAsync: vi.fn() }, update: { mutateAsync: vi.fn() }, status: { mutateAsync: vi.fn(), isPending: false } } as unknown as ReturnType<typeof useUserMutations>)
    render(<UsersPage mode="institution" />)

    expect(screen.getByText('NIIT-STU-001')).toBeInTheDocument()
    expect(screen.getByLabelText('Role')).toBeInTheDocument()
    expect(screen.queryByLabelText('Institution')).not.toBeInTheDocument()
  })
})
