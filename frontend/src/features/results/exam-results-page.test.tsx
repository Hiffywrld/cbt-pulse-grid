import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useCandidateResults, useExamResultSummary } from './result-hooks'
import { resultsApi } from './results-api'
import { ExamResultsPage } from './exam-results-page'

vi.mock('./result-hooks', () => ({ useExamResultSummary: vi.fn(), useCandidateResults: vi.fn() }))
vi.mock('./results-api', () => ({ resultsApi: { exportCsv: vi.fn() } }))

const summary = {
  examId: 'exam-1', examCode: 'WEBYY', examTitle: 'webyy', assignedCandidates: 3,
  notStarted: 1, inProgress: 0, submitted: 1, autoSubmitted: 1, passed: 1, failed: 1,
  averagePercentage: 60, minimumPercentage: 40, maximumPercentage: 80, passRate: 50, totalObtainableMarks: 20,
}
const candidates = {
  content: [{ candidateId: 'user-1', firstName: 'Tobi', lastName: 'Ade', email: 'tobi@example.edu', registrationNumber: 'STU-1', attemptId: 'attempt-1', attemptStatus: 'SUBMITTED', score: 16, maximumScore: 20, percentage: 80, passed: true, startedAt: '', submittedAt: '' }],
  page: 0, size: 20, totalElements: 1, totalPages: 1,
}

describe('ExamResultsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(useExamResultSummary).mockReturnValue({ isPending: false, isError: false, data: summary } as unknown as ReturnType<typeof useExamResultSummary>)
    vi.mocked(useCandidateResults).mockReturnValue({ isPending: false, isError: false, data: candidates } as unknown as ReturnType<typeof useCandidateResults>)
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn(() => 'blob:csv') })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() })
  })

  it('renders real aggregate and candidate result fields', () => {
    render(<MemoryRouter initialEntries={['/institution/results/exams/exam-1']}><Routes><Route path="/institution/results/exams/:examId" element={<ExamResultsPage />} /></Routes></MemoryRouter>)
    expect(screen.getByRole('heading', { name: 'webyy' })).toBeVisible()
    expect(screen.getByText('3')).toBeVisible()
    expect(screen.getByText('Tobi Ade')).toBeVisible()
    expect(screen.getByText('16 / 20')).toBeVisible()
  })

  it('downloads the real CSV blob', async () => {
    vi.mocked(resultsApi.exportCsv).mockResolvedValue(new Blob(['csv'], { type: 'text/csv' }))
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
    render(<MemoryRouter initialEntries={['/institution/results/exams/exam-1']}><Routes><Route path="/institution/results/exams/:examId" element={<ExamResultsPage />} /></Routes></MemoryRouter>)
    await userEvent.click(screen.getByRole('button', { name: 'Export CSV' }))
    expect(resultsApi.exportCsv).toHaveBeenCalledWith('exam-1', { search: '', status: '', passed: '' })
    expect(click).toHaveBeenCalled()
  })
})
