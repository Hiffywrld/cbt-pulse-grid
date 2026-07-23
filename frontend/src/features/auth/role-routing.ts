import type { CurrentUser, Role } from '../../types/auth'

const institutionStaff: Role[] = [
  'INSTITUTION_ADMIN',
  'EXAMINER',
  'INVIGILATOR',
]

export const hasAnyRole = (user: CurrentUser, roles: readonly Role[]) =>
  roles.some((role) => user.roles.includes(role))

export const homeForUser = (user: CurrentUser) => {
  if (user.roles.includes('SUPER_ADMIN')) return '/platform'
  if (hasAnyRole(user, institutionStaff)) return '/institution'
  if (user.roles.includes('STUDENT')) return '/student'
  return '/unauthorized'
}

export const safeInternalDestination = (
  candidate: unknown,
  fallback: string,
) => {
  if (
    typeof candidate !== 'string' ||
    !candidate.startsWith('/') ||
    candidate.startsWith('//') ||
    candidate.includes('\\')
  ) {
    return fallback
  }
  return candidate
}

export const postLoginDestination = (user: CurrentUser, candidate: unknown) => {
  const home = homeForUser(user)
  const safe = safeInternalDestination(candidate, home)
  return safe === home || safe.startsWith(`${home}/`) ? safe : home
}

export const roleLabel = (role: Role) =>
  ({
    SUPER_ADMIN: 'Platform Administrator',
    INSTITUTION_ADMIN: 'Institution Administrator',
    EXAMINER: 'Examiner',
    INVIGILATOR: 'Invigilator',
    STUDENT: 'Student',
  })[role]
