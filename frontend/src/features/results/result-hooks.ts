import { useQuery } from '@tanstack/react-query'
import { resultsApi, type CandidateResultParams } from './results-api'

export const resultKeys = {
  all: ['results'] as const,
  summary: (examId: string) => ['results', 'summary', examId] as const,
  candidates: (examId: string, params: CandidateResultParams) => ['results', 'candidates', examId, params] as const,
  attempt: (attemptId: string) => ['results', 'attempt', attemptId] as const,
}

export const useExamResultSummary = (examId: string) =>
  useQuery({ queryKey: resultKeys.summary(examId), queryFn: () => resultsApi.summary(examId), enabled: Boolean(examId) })

export const useCandidateResults = (examId: string, params: CandidateResultParams) =>
  useQuery({ queryKey: resultKeys.candidates(examId, params), queryFn: () => resultsApi.candidates(examId, params), enabled: Boolean(examId) })

export const useStaffAttemptResult = (attemptId: string) =>
  useQuery({ queryKey: resultKeys.attempt(attemptId), queryFn: () => resultsApi.attempt(attemptId), enabled: Boolean(attemptId) })

