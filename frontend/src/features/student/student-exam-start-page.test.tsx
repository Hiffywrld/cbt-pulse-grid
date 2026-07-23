import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { attemptStorage } from '../../lib/storage/attempt-storage'
import { getDeviceId } from '../../lib/storage/device-identity'
import { attemptApi } from './attempt-api'
import { useStudentExam } from './student-exam-hooks'
import { StudentExamStartPage } from './student-exam-start-page'

vi.mock('./student-exam-hooks', () => ({ useStudentExam: vi.fn() }))
vi.mock('./attempt-api', () => ({ attemptApi: { start: vi.fn(), get: vi.fn() } }))
vi.mock('../../lib/storage/attempt-storage', () => ({ attemptStorage: { attemptForExam: vi.fn(), rememberAttempt: vi.fn() } }))
vi.mock('../../lib/storage/device-identity', () => ({ getDeviceId: vi.fn() }))

const exam = { id: 'exam-1', code: 'WEBYY', title: 'webyy', durationMinutes: 30, startsAt: '', endsAt: '', availability: 'ACTIVE', instructions: null }
const renderPage = () => render(<MemoryRouter initialEntries={['/student/exams/exam-1/start']}><Routes>
  <Route path="/student/exams/:examId/start" element={<StudentExamStartPage />} />
  <Route path="/student/attempts/:attemptId" element={<div>Runner restored</div>} />
</Routes></MemoryRouter>)

describe('StudentExamStartPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useStudentExam).mockReturnValue({ isPending: false, isError: false, data: exam } as unknown as ReturnType<typeof useStudentExam>)
    vi.mocked(attemptStorage.attemptForExam).mockResolvedValue(null)
    vi.mocked(attemptStorage.rememberAttempt).mockResolvedValue(undefined)
    vi.mocked(getDeviceId).mockReturnValue('device-local')
  })

  it('validates six digits and starts only once with no persisted PIN', async () => {
    vi.mocked(attemptApi.start).mockResolvedValue({ attemptId: 'attempt-1', examId: 'exam-1', status: 'IN_PROGRESS' } as never)
    renderPage()
    const input = await screen.findByLabelText('Six-digit access PIN')
    await userEvent.type(input, '123')
    await userEvent.click(screen.getByRole('button', { name: 'Validate PIN and begin' }))
    expect(screen.getByText('Enter exactly six digits')).toBeVisible()
    await userEvent.clear(input)
    await userEvent.type(input, '123456')
    await userEvent.click(screen.getByRole('button', { name: 'Validate PIN and begin' }))
    await waitFor(() => expect(attemptApi.start).toHaveBeenCalledWith('exam-1', '123456', 'device-local'))
    expect(attemptApi.start).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem('123456')).toBeNull()
    expect(await screen.findByText('Runner restored')).toBeVisible()
  })

  it('restores a remembered in-progress attempt without requesting the PIN', async () => {
    vi.mocked(attemptStorage.attemptForExam).mockResolvedValue('attempt-1')
    vi.mocked(attemptApi.get).mockResolvedValue({ attemptId: 'attempt-1', status: 'IN_PROGRESS' } as never)
    renderPage()
    expect(await screen.findByText('Runner restored')).toBeVisible()
    expect(attemptApi.start).not.toHaveBeenCalled()
  })
})

