import type { QuestionType } from './academic'

export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'AUTO_SUBMITTED'

export type CandidateOption = {
  id: string
  optionText: string
  displayOrder: number
}

export type CandidateQuestion = {
  id: string
  position: number
  questionText: string
  questionType: QuestionType
  options: CandidateOption[]
}

export type SavedAnswer = {
  attemptQuestionId: string
  selectedOptionIds: string[]
  clientSequence: number
  answeredAt: string
}

export type AttemptPackage = {
  attemptId: string
  examId: string
  examCode: string
  title: string
  instructions: string | null
  status: AttemptStatus
  serverTime: string
  expiresAt: string
  remainingSeconds: number
  questions: CandidateQuestion[]
  answers: SavedAnswer[]
}

export type AttemptResult = {
  attemptId: string
  status: AttemptStatus
  submittedAt: string | null
  score: number | null
  maximumScore: number | null
  percentage: number | null
  passed: boolean | null
}

export type QueuedAnswer = {
  attemptId: string
  attemptQuestionId: string
  selectedOptionIds: string[]
  clientSequence: number
  updatedAt: string
}

export type MonitoringEventType =
  | 'TAB_HIDDEN'
  | 'WINDOW_BLUR'
  | 'FULLSCREEN_EXIT'
  | 'COPY_ATTEMPT'
  | 'PASTE_ATTEMPT'
  | 'NETWORK_DISCONNECTED'
  | 'NETWORK_RECONNECTED'

