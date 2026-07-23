import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuth } from '../auth/use-auth'
import { useSubjectMutations, useSubjects } from './subject-hooks'
import { SubjectsPage } from './subjects-page'

vi.mock('../auth/use-auth', () => ({ useAuth: vi.fn() }))
vi.mock('./subject-hooks', () => ({ useSubjects: vi.fn(), useSubjectMutations: vi.fn() }))

const subject = {
  id: 'subject-1', institutionId: 'institution-1', code: 'CSC-101', name: 'Computer Science',
  description: 'Foundations', status: 'ACTIVE' as const, createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-02T00:00:00Z', version: 0,
}

describe('SubjectsPage', () => {
  const create = { mutateAsync: vi.fn().mockResolvedValue(subject) }
  const update = { mutateAsync: vi.fn().mockResolvedValue(subject) }
  const status = { mutateAsync: vi.fn().mockResolvedValue({ ...subject, status: 'INACTIVE' }), isPending: false }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useAuth).mockReturnValue({ user: { id: 'admin', email: 'admin@example.edu', institutionId: 'institution-1', roles: ['INSTITUTION_ADMIN'] } } as ReturnType<typeof useAuth>)
    vi.mocked(useSubjects).mockReturnValue({ isPending: false, isError: false, data: { content: [subject], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true } } as ReturnType<typeof useSubjects>)
    vi.mocked(useSubjectMutations).mockReturnValue({ create, update, status } as unknown as ReturnType<typeof useSubjectMutations>)
  })

  it('creates, edits and changes subject status through working controls', async () => {
    render(<SubjectsPage />)
    await userEvent.click(screen.getByRole('button', { name: 'New subject' }))
    await userEvent.type(screen.getByLabelText('Subject code'), 'mat-101')
    await userEvent.type(screen.getByLabelText('Subject name'), 'Mathematics')
    await userEvent.click(screen.getByRole('button', { name: 'Create subject' }))
    await waitFor(() => expect(create.mutateAsync).toHaveBeenCalledWith({ code: 'mat-101', name: 'Mathematics', description: null }))

    await userEvent.click(screen.getByRole('button', { name: 'Edit' }))
    await userEvent.clear(screen.getByLabelText('Subject name'))
    await userEvent.type(screen.getByLabelText('Subject name'), 'Applied Computing')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    await waitFor(() => expect(update.mutateAsync).toHaveBeenCalledWith(expect.objectContaining({ id: 'subject-1', body: expect.objectContaining({ name: 'Applied Computing' }) })))

    await userEvent.click(screen.getByRole('button', { name: 'Deactivate' }))
    await userEvent.click(screen.getByRole('dialog', { name: 'Deactivate subject?' }).querySelector('.button--danger') as HTMLButtonElement)
    expect(status.mutateAsync).toHaveBeenCalledWith({ id: 'subject-1', status: 'INACTIVE' })
  })

  it('gives examiners read-only subject access', () => {
    vi.mocked(useAuth).mockReturnValue({ user: { id: 'examiner', email: 'examiner@example.edu', institutionId: 'institution-1', roles: ['EXAMINER'] } } as ReturnType<typeof useAuth>)
    render(<SubjectsPage />)
    expect(screen.getByText('Read-only access')).toBeVisible()
    expect(screen.queryByRole('button', { name: 'New subject' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'View' })).toBeVisible()
  })
})
