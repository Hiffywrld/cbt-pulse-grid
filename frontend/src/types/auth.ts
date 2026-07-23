export const domainRoles = [
  'SUPER_ADMIN',
  'INSTITUTION_ADMIN',
  'EXAMINER',
  'INVIGILATOR',
  'STUDENT',
] as const

export type Role = (typeof domainRoles)[number]

export type LoginRequest = {
  email: string
  password: string
}

export type RefreshRequest = {
  refreshToken: string
}

export type LogoutRequest = RefreshRequest

export type TokenResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresAt: string
}

export type CurrentUser = {
  id: string
  email: string
  firstName?: string | null
  lastName?: string | null
  registrationNumber?: string | null
  institutionId: string | null
  institutionName?: string | null
  institutionCode?: string | null
  roles: Role[]
}

export const isRole = (value: string): value is Role =>
  domainRoles.some((role) => role === value)
