import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { InstitutionsPage } from './institutions-page'
import { useInstitutionMutations, useInstitutions } from './institution-hooks'

vi.mock('./institution-hooks', () => ({ useInstitutions: vi.fn(), useInstitutionMutations: vi.fn() }))

const institution = { id: 'institution-1', name: 'NIIT Lagos Campus', code: 'NIIT-LAGOS', status: 'ACTIVE' as const, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-02T00:00:00Z', version: 0 }

describe('InstitutionsPage', () => {
  it('opens and submits the real create form, then closes the modal', async () => {
    const createMutation = { mutateAsync: vi.fn().mockResolvedValue(institution) }
    vi.mocked(useInstitutions).mockReturnValue({ isPending: false, isError: false, data: { content: [institution], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true } } as ReturnType<typeof useInstitutions>)
    vi.mocked(useInstitutionMutations).mockReturnValue({ create: createMutation, update: { mutateAsync: vi.fn() }, status: { mutateAsync: vi.fn(), isPending: false } } as unknown as ReturnType<typeof useInstitutionMutations>)
    render(<InstitutionsPage />)

    await userEvent.click(screen.getByRole('button', { name: 'New institution' }))
    expect(screen.getByRole('dialog', { name: 'Create institution' })).toBeVisible()
    await userEvent.type(screen.getByLabelText('Institution name'), 'Abuja Learning Centre')
    await userEvent.type(screen.getByLabelText('Institution code'), 'alc-abuja')
    await userEvent.click(screen.getByRole('button', { name: 'Create institution' }))

    await waitFor(() => expect(createMutation.mutateAsync).toHaveBeenCalledWith({
      name: 'Abuja Learning Centre',
      code: 'alc-abuja',
    }))
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Create institution' })).not.toBeInTheDocument())
  })

  it('opens view and edit actions and allows modal cancellation', async () => {
    vi.mocked(useInstitutions).mockReturnValue({ isPending: false, isError: false, data: { content: [institution], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true } } as ReturnType<typeof useInstitutions>)
    vi.mocked(useInstitutionMutations).mockReturnValue({ create: { mutateAsync: vi.fn() }, update: { mutateAsync: vi.fn() }, status: { mutateAsync: vi.fn(), isPending: false } } as unknown as ReturnType<typeof useInstitutionMutations>)
    render(<InstitutionsPage />)

    await userEvent.click(screen.getByRole('button', { name: 'View' }))
    expect(screen.getByRole('dialog', { name: 'Institution details' })).toBeVisible()
    await userEvent.click(screen.getByRole('button', { name: 'Close dialog' }))
    await userEvent.click(screen.getByRole('button', { name: 'Edit' }))
    expect(screen.getByRole('dialog', { name: 'Edit institution' })).toBeVisible()
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog', { name: 'Edit institution' })).not.toBeInTheDocument()
  })

  it('renders desktop table semantics, mobile card labels, and status confirmation', async () => {
    const statusMutation = { mutateAsync: vi.fn().mockResolvedValue({ ...institution, status: 'SUSPENDED' }), isPending: false }
    vi.mocked(useInstitutions).mockReturnValue({ isPending: false, isError: false, data: { content: [institution], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true } } as ReturnType<typeof useInstitutions>)
    vi.mocked(useInstitutionMutations).mockReturnValue({ create: { mutateAsync: vi.fn() }, update: { mutateAsync: vi.fn() }, status: statusMutation } as unknown as ReturnType<typeof useInstitutionMutations>)
    render(<InstitutionsPage />)

    expect(screen.getByRole('table')).toBeInTheDocument()
    expect(screen.getByText('NIIT Lagos Campus').closest('td')).toHaveAttribute('data-label', 'Institution')
    await userEvent.click(screen.getAllByRole('button', { name: 'Suspend' })[0])
    expect(screen.getByRole('dialog', { name: 'Suspend institution?' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('dialog', { name: 'Suspend institution?' }).querySelector('.button--danger') as HTMLButtonElement)
    expect(statusMutation.mutateAsync).toHaveBeenCalledWith({ id: 'institution-1', status: 'SUSPENDED' })
  })
})
