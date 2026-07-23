import type { QuestionType } from './academic'

export type CandidateResultStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'SUBMITTED' | 'AUTO_SUBMITTED'

export type ExamResultSummary = {
  examId: string
  examCode: string
  examTitle: string
  assignedCandidates: number
  notStarted: number
  inProgress: number
  submitted: number
  autoSubmitted: number
  passed: number
  failed: number
  averagePercentage: number | null
  minimumPercentage: number | null
  maximumPercentage: number | null
  passRate: number | null
  totalObtainableMarks: number
}

export type CandidateResult = {
  candidateId: string
  firstName: string
  lastName: string
  email: string
  registrationNumber: string | null
  attemptId: string | null
  attemptStatus: CandidateResultStatus
  score: number | null
  maximumScore: number | null
  percentage: number | null
  passed: boolean | null
  startedAt: string | null
  submittedAt: string | null
}

export type ResultPage<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type StaffOptionReview = {
  optionId: string
  optionText: string
  displayOrder: number
  selected: boolean
  correct: boolean
}

export type StaffQuestionReview = {
  attemptQuestionId: string
  position: number
  questionText: string
  questionType: QuestionType
  marks: number
  awardedMarks: number
  options: StaffOptionReview[]
}

export type StaffAttemptResult = {
  attemptId: string
  examId: string
  examCode: string
  examTitle: string
  candidateId: string
  firstName: string
  lastName: string
  email: string
  registrationNumber: string | null
  status: CandidateResultStatus
  score: number | null
  maximumScore: number | null
  percentage: number | null
  passed: boolean | null
  startedAt: string | null
  submittedAt: string | null
  reviewAvailable: boolean
  questions: StaffQuestionReview[]
}

