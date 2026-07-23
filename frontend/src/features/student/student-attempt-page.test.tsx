import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { attemptStorage } from '../../lib/storage/attempt-storage'
import { attemptApi } from './attempt-api'
import { StudentAttemptPage } from './student-attempt-page'

vi.mock('./attempt-api', () => ({ attemptApi: { get: vi.fn(), syncAnswers: vi.fn(), submit: vi.fn(), result: vi.fn() } }))
vi.mock('./use-attempt-monitoring', () => ({ useAttemptMonitoring: vi.fn() }))
vi.mock('../../lib/storage/attempt-storage', () => ({ attemptStorage: {
  rememberAttempt: vi.fn(), answers: vi.fn(), answerBatch: vi.fn(), queueAnswer: vi.fn(), acknowledgeAnswers: vi.fn(), acknowledgeAnswerBatch: vi.fn(), clearAttempt: vi.fn(),
} }))

const serverTime = new Date()
const attempt = {
  attemptId: 'attempt-1',
  examId: 'exam-1',
  examCode: 'WEBYY',
  title: 'webyy',
  instructions: null,
  status: 'IN_PROGRESS' as const,
  serverTime: serverTime.toISOString(),
  expiresAt: new Date(serverTime.getTime() + 3_600_000).toISOString(),
  remainingSeconds: 3600,
  questions: [
    { id: 'q1', position: 0, questionText: 'Which protocol secures HTTP?', questionType: 'SINGLE_CHOICE' as const, options: [
      { id: 'o1', optionText: 'HTTPS', displayOrder: 1, correct: true },
      { id: 'o2', optionText: 'FTP', displayOrder: 2, correct: false },
    ] },
    { id: 'q2', position: 1, questionText: 'Select browser APIs', questionType: 'MULTIPLE_CHOICE' as const, options: [
      { id: 'o3', optionText: 'Fetch', displayOrder: 1 },
      { id: 'o4', optionText: 'DOM', displayOrder: 2 },
    ] },
  ],
  answers: [],
}

const renderRunner = () => {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={['/student/attempts/attempt-1']}><Routes>
    <Route path="/student/attempts/:attemptId" element={<StudentAttemptPage />} />
    <Route path="/student/attempts/:attemptId/result" element={<div>Result opened</div>} />
  </Routes></MemoryRouter></QueryClientProvider>)
}

describe('StudentAttemptPage', () => {
  let queued: Array<{ attemptId: string; attemptQuestionId: string; selectedOptionIds: string[]; clientSequence: number; updatedAt: string }>
  beforeEach(() => {
    vi.clearAllMocks()
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true })
    queued = []
    vi.mocked(attemptApi.get).mockResolvedValue(attempt as never)
    vi.mocked(attemptStorage.rememberAttempt).mockResolvedValue(undefined)
    vi.mocked(attemptStorage.answers).mockImplementation(async () => queued)
    vi.mocked(attemptStorage.answerBatch).mockImplementation(async () => queued.length ? {
      syncId: 'sync-stable',
      attemptId: 'attempt-1',
      answers: [...queued],
      createdAt: new Date().toISOString(),
    } : null)
    vi.mocked(attemptStorage.queueAnswer).mockImplementation(async (answer) => { queued = [answer]; return true })
    vi.mocked(attemptStorage.acknowledgeAnswers).mockImplementation(async () => { queued = [] })
    vi.mocked(attemptStorage.acknowledgeAnswerBatch).mockResolvedValue(undefined)
    vi.mocked(attemptApi.syncAnswers).mockImplementation(async (_id, syncId, answers) => ({
      acknowledgedSyncId: syncId,
      savedAnswers: answers.map((answer) => ({ ...answer, answeredAt: new Date().toISOString() })),
      lastSavedAt: new Date().toISOString(),
      status: 'IN_PROGRESS',
    }))
  })

  it('navigates questions, queues an answer, and never renders correctness flags', async () => {
    renderRunner()
    expect(await screen.findByRole('heading', { name: 'Which protocol secures HTTP?' })).toBeVisible()
    expect(document.body.textContent).not.toContain('correct')
    await userEvent.click(screen.getByText('HTTPS'))
    expect(attemptStorage.queueAnswer).toHaveBeenCalledWith(expect.objectContaining({
      attemptQuestionId: 'q1', selectedOptionIds: ['o1'], clientSequence: 0,
    }))
    await userEvent.click(screen.getByRole('button', { name: 'Next question' }))
    expect(screen.getByRole('heading', { name: 'Select browser APIs' })).toBeVisible()
    expect(screen.getByRole('button', { name: 'Question 1, answered' })).toBeVisible()
  })

  it('keeps an offline answer queued and synchronizes it after reconnection', async () => {
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: false })
    renderRunner()
    await userEvent.click(await screen.findByText('HTTPS'))
    expect(screen.getAllByText('Offline')).toHaveLength(2)
    expect(attemptApi.syncAnswers).not.toHaveBeenCalled()
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true })
    window.dispatchEvent(new Event('online'))
    await waitFor(() => expect(attemptApi.syncAnswers).toHaveBeenCalledWith(
      'attempt-1',
      'sync-stable',
      [expect.objectContaining({ attemptQuestionId: 'q1', clientSequence: 0 })],
    ))
  })

  it('confirms, flushes, and submits idempotently through the backend result', async () => {
    vi.mocked(attemptApi.submit).mockResolvedValue({ attemptId: 'attempt-1', status: 'SUBMITTED', submittedAt: '', score: 1, maximumScore: 2, percentage: 50, passed: true })
    renderRunner()
    await screen.findByText('Which protocol secures HTTP?')
    await userEvent.click(screen.getByRole('button', { name: 'Question 2, unanswered' }))
    await userEvent.click(screen.getByRole('button', { name: 'Review and submit' }))
    const dialog = screen.getByRole('dialog', { name: 'Submit examination?' })
    expect(within(dialog).getByText(/0 answered and 2 unanswered/)).toBeVisible()
    await userEvent.click(within(dialog).getByRole('button', { name: 'Submit examination' }))
    await waitFor(() => expect(attemptApi.submit).toHaveBeenCalledTimes(1))
    expect(attemptStorage.clearAttempt).toHaveBeenCalledWith('attempt-1')
    expect(await screen.findByText('Result opened')).toBeVisible()
  })

  it('automatically submits once when the authoritative expiry has passed', async () => {
    vi.mocked(attemptApi.get).mockResolvedValue({
      ...attempt,
      serverTime: new Date().toISOString(),
      expiresAt: new Date(Date.now() - 1_000).toISOString(),
      remainingSeconds: 0,
    } as never)
    vi.mocked(attemptApi.submit).mockResolvedValue({ attemptId: 'attempt-1', status: 'AUTO_SUBMITTED', submittedAt: '', score: 0, maximumScore: 2, percentage: 0, passed: false })
    renderRunner()
    await waitFor(() => expect(attemptApi.submit).toHaveBeenCalledTimes(1))
    expect(await screen.findByText('Result opened')).toBeVisible()
  })

  it.each(['light', 'dark'])('keeps the complete runner accessible in %s mode at mobile width', async (theme) => {
    document.documentElement.dataset.theme = theme
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 390 })
    renderRunner()
    expect(await screen.findByRole('main')).toHaveClass('exam-runner')
    expect(screen.getByLabelText('Question navigation')).toBeVisible()
    expect(screen.getByRole('button', { name: 'Next question' })).toBeVisible()
  })
})
