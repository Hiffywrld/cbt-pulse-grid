import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiClientError } from '../../lib/api/api-error'
import { useAuth } from '../auth/use-auth'
import { useSubjects } from '../subjects/subject-hooks'
import { useUsers } from '../users/user-hooks'
import { ExamDetailPage } from './exam-detail-page'
import { useExam, useExamCandidates, useExamMutations } from './exam-hooks'

vi.mock('../auth/use-auth', () => ({ useAuth: vi.fn() }))
vi.mock('../subjects/subject-hooks', () => ({ useSubjects: vi.fn() }))
vi.mock('../users/user-hooks', () => ({ useUsers: vi.fn() }))
vi.mock('./exam-hooks', () => ({
  examKeys: { all: ['exams'] },
  useExam: vi.fn(),
  useExamCandidates: vi.fn(),
  useExamMutations: vi.fn(),
}))

const exam = {
  id: 'exam-1', institutionId: 'institution-1', subjectId: 'subject-1', createdBy: 'admin-1',
  code: 'WEB-101', title: 'Web Development', instructions: null, durationMinutes: 60,
  startsAt: '2026-08-01T09:00:00Z', endsAt: '2026-08-01T11:00:00Z',
  accessPinConfigured: true, shuffleQuestions: true, shuffleOptions: true, status: 'DRAFT' as const,
  poolRules: [{ id: 'rule-1', difficulty: 'EASY' as const, questionCount: 1, marksPerQuestion: 2 }],
  createdAt: '', updatedAt: '', version: 0, passMarkPercentage: 50,
}

const renderPage = () => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/institution/exams/exam-1']}><Routes>
    <Route path="/institution/exams/:examId" element={<ExamDetailPage />} />
  </Routes></MemoryRouter></QueryClientProvider>)
}

describe('ExamDetailPage publication', () => {
  const publish = { mutateAsync: vi.fn(), isPending: false }

  beforeEach(() => {
    vi.clearAllMocks()
    publish.mutateAsync.mockReset()
    vi.mocked(useAuth).mockReturnValue({ user: { id: 'admin-1', email: 'admin@example.edu', institutionId: 'institution-1', roles: ['INSTITUTION_ADMIN'] } } as ReturnType<typeof useAuth>)
    vi.mocked(useExam).mockReturnValue({ isPending: false, isError: false, data: exam } as ReturnType<typeof useExam>)
    vi.mocked(useExamCandidates).mockReturnValue({ isPending: false, isError: false, data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true } } as unknown as ReturnType<typeof useExamCandidates>)
    vi.mocked(useSubjects).mockReturnValue({ data: { content: [] } } as unknown as ReturnType<typeof useSubjects>)
    vi.mocked(useUsers).mockReturnValue({} as ReturnType<typeof useUsers>)
    vi.mocked(useExamMutations).mockReturnValue({
      publish,
      cancel: { mutateAsync: vi.fn(), isPending: false },
      close: { mutateAsync: vi.fn(), isPending: false },
      update: { mutateAsync: vi.fn() },
      assign: { mutateAsync: vi.fn() },
      remove: { mutateAsync: vi.fn(), isPending: false },
    } as unknown as ReturnType<typeof useExamMutations>)
  })

  it('keeps publishing behind confirmation and sends only the exam ID', async () => {
    publish.mutateAsync.mockResolvedValue(exam)
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: 'Publish' }))
    const dialog = screen.getByRole('dialog', { name: 'Publish exam?' })
    expect(publish.mutateAsync).not.toHaveBeenCalled()
    await userEvent.click(within(dialog).getByRole('button', { name: 'Publish' }))
    await waitFor(() => expect(publish.mutateAsync).toHaveBeenCalledWith('exam-1'))
  })

  it('displays the safe publication prerequisite returned by the backend', async () => {
    publish.mutateAsync.mockRejectedValue(new ApiClientError({
      status: 400,
      error: 'Bad Request',
      message: 'Not enough published questions for difficulty EASY',
      validationErrors: {},
    }))
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: 'Publish' }))
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Publish exam?' })).getByRole('button', { name: 'Publish' }))
    expect(await screen.findByText('Not enough published questions for difficulty EASY')).toBeVisible()
  })

  it.each([
    [403, 'Forbidden', 'Role or institution access is denied', 'You do not have permission to perform this action.'],
    [404, 'Not Found', 'Exam not found', 'The requested record could not be found.'],
    [409, 'Conflict', 'Exam was changed by another request; refresh and try again', 'Exam was changed by another request; refresh and try again'],
  ])('maps a %s lifecycle failure to a safe actionable message', async (status, error, message, expected) => {
    publish.mutateAsync.mockRejectedValue(new ApiClientError({
      status,
      error,
      message,
      validationErrors: {},
    }))
    renderPage()
    await userEvent.click(screen.getByRole('button', { name: 'Publish' }))
    await userEvent.click(within(screen.getByRole('dialog', { name: 'Publish exam?' })).getByRole('button', { name: 'Publish' }))
    expect(await screen.findByText(expected)).toBeVisible()
  })
})
