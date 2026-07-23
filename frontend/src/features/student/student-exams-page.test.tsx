import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { StudentExamsPage } from './student-exams-page'
import { useStudentExams } from './student-exam-hooks'

vi.mock('./student-exam-hooks', () => ({ useStudentExams: vi.fn() }))

describe('StudentExamsPage', () => {
  beforeEach(() => vi.mocked(useStudentExams).mockReset())

  it('renders a genuine assigned exam and availability', () => {
    vi.mocked(useStudentExams).mockReturnValue({ isPending: false, isError: false, data: [{ id: 'exam-1', code: 'MTH-101', title: 'Mathematics I', durationMinutes: 60, startsAt: '2026-07-23T10:00:00Z', endsAt: '2026-07-23T12:00:00Z', availability: 'ACTIVE' }] } as ReturnType<typeof useStudentExams>)
    render(<MemoryRouter><StudentExamsPage /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: 'Mathematics I' })).toBeInTheDocument()
    expect(screen.getByText('ACTIVE')).toBeInTheDocument()
    expect(screen.getByText('60 minutes')).toBeInTheDocument()
  })

  it('renders an honest empty state', () => {
    vi.mocked(useStudentExams).mockReturnValue({ isPending: false, isError: false, data: [] } as unknown as ReturnType<typeof useStudentExams>)
    render(<MemoryRouter><StudentExamsPage /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: 'No assigned examinations' })).toBeInTheDocument()
  })

  it('renders a recoverable error state', () => {
    vi.mocked(useStudentExams).mockReturnValue({ isPending: false, isError: true, error: new Error('offline'), refetch: vi.fn() } as unknown as ReturnType<typeof useStudentExams>)
    render(<MemoryRouter><StudentExamsPage /></MemoryRouter>)
    expect(screen.getByRole('heading', { name: 'Examinations could not be loaded' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument()
  })
})
