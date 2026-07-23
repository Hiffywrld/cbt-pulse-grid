import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { studentExamKeys } from '../student/student-exam-hooks'
import { examKeys, useExamMutations } from './exam-hooks'
import { examsApi } from './exams-api'

vi.mock('./exams-api', () => ({
  examsApi: {
    publish: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    cancel: vi.fn(),
    close: vi.fn(),
    assignCandidates: vi.fn(),
    removeCandidate: vi.fn(),
  },
}))

describe('exam mutation refresh', () => {
  beforeEach(() => vi.clearAllMocks())

  it('publishes by ID and invalidates staff and student exam queries', async () => {
    vi.mocked(examsApi.publish).mockResolvedValue({} as never)
    const client = new QueryClient()
    const invalidate = vi.spyOn(client, 'invalidateQueries')
    const wrapper = ({ children }: { children: ReactNode }) =>
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    const { result } = renderHook(() => useExamMutations(), { wrapper })

    await act(() => result.current.publish.mutateAsync('exam-1'))

    expect(examsApi.publish).toHaveBeenCalledWith('exam-1')
    expect(invalidate).toHaveBeenCalledWith({ queryKey: examKeys.all })
    expect(invalidate).toHaveBeenCalledWith({ queryKey: studentExamKeys.all })
  })
})
