import type { CurrentUser } from '../../types/auth'

const present = (value: string | null | undefined) => value?.trim() || null

export const firstNameFor = (user: CurrentUser) =>
  present(user.firstName) ?? user.email

export const fullNameFor = (user: CurrentUser) => {
  const fullName = [present(user.firstName), present(user.lastName)].filter(Boolean).join(' ')
  return fullName || user.email
}

export const institutionNameFor = (user: CurrentUser) =>
  present(user.institutionName) ?? (user.institutionId ? 'Institution workspace' : 'Platform administration')

export const institutionCodeFor = (user: CurrentUser) => present(user.institutionCode)
