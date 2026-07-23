import type { PageResponse } from './management'

export type SubjectStatus = 'ACTIVE' | 'INACTIVE'
export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE'
export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type QuestionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'
export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'CLOSED'

export type Subject = {
  id: string
  institutionId: string
  code: string
  name: string
  description: string | null
  status: SubjectStatus
  createdAt: string
  updatedAt: string
  version: number
}

export type SubjectInput = {
  code: string
  name: string
  description?: string | null
}

export type QuestionOptionInput = {
  optionText: string
  correct: boolean
  displayOrder: number
}

export type StaffQuestionOption = QuestionOptionInput & { id: string }

export type QuestionSummary = {
  id: string
  institutionId: string
  subjectId: string
  createdBy: string
  questionText: string
  type: QuestionType
  difficulty: QuestionDifficulty
  marks: number
  status: QuestionStatus
  createdAt: string
  updatedAt: string
  version: number
}

export type StaffQuestion = QuestionSummary & { options: StaffQuestionOption[] }

export type QuestionInput = {
  subjectId: string
  questionText: string
  type: QuestionType
  difficulty: QuestionDifficulty
  marks: number
  options: QuestionOptionInput[]
}

export type ExamPoolRule = {
  id: string
  difficulty: QuestionDifficulty
  questionCount: number
  marksPerQuestion: number
}

export type ExamPoolRuleInput = Omit<ExamPoolRule, 'id'>

export type ExamSummary = {
  id: string
  institutionId: string
  subjectId: string
  code: string
  title: string
  durationMinutes: number
  startsAt: string
  endsAt: string
  shuffleQuestions: boolean
  shuffleOptions: boolean
  status: ExamStatus
  createdAt: string
  updatedAt: string
  version: number
  passMarkPercentage: number
}

export type ExamDetail = ExamSummary & {
  createdBy: string
  instructions: string | null
  accessPinConfigured: boolean
  poolRules: ExamPoolRule[]
}

export type ExamInput = {
  code: string
  subjectId: string
  title: string
  instructions?: string | null
  durationMinutes: number
  startsAt: string
  endsAt: string
  accessPin?: string
  shuffleQuestions: boolean
  shuffleOptions: boolean
  poolRules: ExamPoolRuleInput[]
  passMarkPercentage?: number
}

export type ExamCandidate = {
  assignmentId: string
  userId: string
  firstName: string
  lastName: string
  email: string
  registrationNumber: string | null
  status: string
  assignedBy: string
  assignedAt: string
}

export type AcademicPage<T> = PageResponse<T>
