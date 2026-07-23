import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useAuth } from '../auth/use-auth'
import { useSubjects } from '../subjects/subject-hooks'
import { useExamMutations, useExams } from './exam-hooks'
import { ExamsPage } from './exams-page'

vi.mock('../auth/use-auth', () => ({ useAuth: vi.fn() }))
vi.mock('../subjects/subject-hooks', () => ({ useSubjects: vi.fn() }))
vi.mock('./exam-hooks', () => ({ useExams: vi.fn(), useExamMutations: vi.fn() }))

const subject = { id: 'subject-1', institutionId: 'institution-1', code: 'CSC-101', name: 'Computing', description: null, status: 'ACTIVE' as const, createdAt: '', updatedAt: '', version: 0 }
const exam = { id: 'exam-1', institutionId: 'institution-1', subjectId: 'subject-1', code: 'CSC-MID', title: 'Computing Midterm', durationMinutes: 60, startsAt: '2026-08-01T09:00:00Z', endsAt: '2026-08-01T11:00:00Z', shuffleQuestions: true, shuffleOptions: true, status: 'PUBLISHED' as const, createdAt: '', updatedAt: '', version: 0, passMarkPercentage: 50 }

describe('ExamsPage role behaviour', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useSubjects).mockReturnValue({ data: { content: [subject] } } as ReturnType<typeof useSubjects>)
    vi.mocked(useExams).mockReturnValue({ isPending: false, isError: false, data: { content: [exam], page: 0, size: 20, totalElements: 1, totalPages: 1, first: true, last: true } } as ReturnType<typeof useExams>)
    vi.mocked(useExamMutations).mockReturnValue({ create: { mutateAsync: vi.fn() } } as unknown as ReturnType<typeof useExamMutations>)
  })

  it('keeps invigilators read-only and requests published exams', () => {
    vi.mocked(useAuth).mockReturnValue({ user: { id: 'invigilator', email: 'i@example.edu', institutionId: 'institution-1', roles: ['INVIGILATOR'] } } as ReturnType<typeof useAuth>)
    render(<MemoryRouter><ExamsPage /></MemoryRouter>)
    expect(screen.getByText('Invigilator view')).toBeVisible()
    expect(screen.queryByRole('button', { name: 'New exam' })).not.toBeInTheDocument()
    expect(useExams).toHaveBeenCalledWith(expect.objectContaining({ status: 'PUBLISHED' }))
    expect(screen.getByRole('button', { name: 'Open' })).toBeVisible()
  })

  it('lets institution managers open the real exam form', async () => {
    vi.mocked(useAuth).mockReturnValue({ user: { id: 'admin', email: 'a@example.edu', institutionId: 'institution-1', roles: ['INSTITUTION_ADMIN'] } } as ReturnType<typeof useAuth>)
    render(<MemoryRouter><ExamsPage /></MemoryRouter>)
    await userEvent.click(screen.getByRole('button', { name: 'New exam' }))
    expect(screen.getByRole('dialog', { name: 'Create examination' })).toBeVisible()
    expect(screen.getByLabelText('Six-digit access PIN')).toHaveAttribute('type', 'password')
  })
})
