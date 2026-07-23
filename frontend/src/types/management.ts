import type { Role } from './auth'

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type InstitutionStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'LOCKED'

export type Institution = {
  id: string
  name: string
  code: string
  status: InstitutionStatus
  createdAt: string
  updatedAt: string
  version: number
}

export type ManagedUser = {
  id: string
  firstName: string
  lastName: string
  email: string
  institutionId: string
  roles: Role[]
  registrationNumber: string | null
  status: UserStatus
  createdAt: string
  updatedAt: string
  version: number
}

export type ExamAvailability = 'UPCOMING' | 'ACTIVE' | 'ENDED'
export type StudentExamSummary = {
  id: string
  code: string
  title: string
  durationMinutes: number
  startsAt: string
  endsAt: string
  availability: ExamAvailability
}

export type StudentExamDetail = StudentExamSummary & { instructions: string | null }
