import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../lib/api/client'
import { examsApi } from './exams/exams-api'
import { questionsApi } from './questions/questions-api'
import { subjectsApi } from './subjects/subjects-api'

vi.mock('../lib/api/client', () => ({ apiClient: { request: vi.fn() } }))

describe('academic API contracts', () => {
  beforeEach(() => vi.mocked(apiClient.request).mockReset())

  it('uses exact subject endpoints and omits absent filters', async () => {
    await subjectsApi.list({ page: 0, size: 20 })
    await subjectsApi.create({ code: 'CSC-101', name: 'Computer Science', description: null })
    await subjectsApi.update('subject-1', { code: 'CSC-101', name: 'Computing', description: 'Updated' })
    await subjectsApi.changeStatus('subject-1', 'INACTIVE')

    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/subjects?page=0&size=20')
    expect(apiClient.request).toHaveBeenNthCalledWith(2, '/api/v1/subjects', {
      method: 'POST', body: { code: 'CSC-101', name: 'Computer Science', description: null },
    })
    expect(apiClient.request).toHaveBeenNthCalledWith(3, '/api/v1/subjects/subject-1', {
      method: 'PUT', body: { code: 'CSC-101', name: 'Computing', description: 'Updated' },
    })
    expect(apiClient.request).toHaveBeenNthCalledWith(4, '/api/v1/subjects/subject-1/status', {
      method: 'PATCH', body: { status: 'INACTIVE' },
    })
  })

  it('sends all question filters and preserves answer option contracts', async () => {
    const body = {
      subjectId: 'subject-1', questionText: 'Choose primes', type: 'MULTIPLE_CHOICE' as const,
      difficulty: 'HARD' as const, marks: 2,
      options: [
        { optionText: '2', correct: true, displayOrder: 1 },
        { optionText: '3', correct: true, displayOrder: 2 },
      ],
    }
    await questionsApi.list({ subjectId: 'subject-1', type: 'MULTIPLE_CHOICE', difficulty: 'HARD', status: 'PUBLISHED', search: 'PRIME', page: 1, size: 20 })
    await questionsApi.create(body)
    await questionsApi.changeStatus('question-1', 'PUBLISHED')

    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/questions?subjectId=subject-1&type=MULTIPLE_CHOICE&difficulty=HARD&status=PUBLISHED&search=PRIME&page=1&size=20')
    expect(apiClient.request).toHaveBeenNthCalledWith(2, '/api/v1/questions', { method: 'POST', body })
    expect(apiClient.request).toHaveBeenNthCalledWith(3, '/api/v1/questions/question-1/status', { method: 'PATCH', body: { status: 'PUBLISHED' } })
  })

  it('uses exact exam lifecycle, PIN and candidate endpoints', async () => {
    await examsApi.list({ page: 0, size: 20 })
    await examsApi.publish('exam-1')
    await examsApi.cancel('exam-1')
    await examsApi.close('exam-1')
    await examsApi.rotatePin('exam-1', '123456')
    await examsApi.assignCandidates('exam-1', ['student-1'])
    await examsApi.candidates('exam-1', 0, 20)
    await examsApi.removeCandidate('exam-1', 'student-1')

    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/exams?page=0&size=20')
    expect(apiClient.request).toHaveBeenNthCalledWith(2, '/api/v1/exams/exam-1/publish', { method: 'POST' })
    expect(apiClient.request).toHaveBeenNthCalledWith(3, '/api/v1/exams/exam-1/cancel', { method: 'POST' })
    expect(apiClient.request).toHaveBeenNthCalledWith(4, '/api/v1/exams/exam-1/close', { method: 'POST' })
    expect(apiClient.request).toHaveBeenNthCalledWith(5, '/api/v1/exams/exam-1/access-pin', { method: 'PUT', body: { accessPin: '123456' } })
    expect(apiClient.request).toHaveBeenNthCalledWith(6, '/api/v1/exams/exam-1/candidates', { method: 'POST', body: { userIds: ['student-1'] } })
    expect(apiClient.request).toHaveBeenNthCalledWith(7, '/api/v1/exams/exam-1/candidates?page=0&size=20')
    expect(apiClient.request).toHaveBeenNthCalledWith(8, '/api/v1/exams/exam-1/candidates/student-1', { method: 'DELETE' })
  })
})
