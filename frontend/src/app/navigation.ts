import { BookOpenCheck, Building2, ClipboardCheck, FileQuestion, Gauge, History, LayoutDashboard, MonitorCheck, RadioTower, ScrollText, Users, type LucideIcon } from 'lucide-react'
import type { CurrentUser, Role } from '../types/auth'

export type NavigationItem = { label: string; to: string; icon: LucideIcon; roles: readonly Role[] }
const allStaff: Role[] = ['INSTITUTION_ADMIN', 'EXAMINER', 'INVIGILATOR']
const academicManagers: Role[] = ['INSTITUTION_ADMIN', 'EXAMINER']

export const navigationItems: NavigationItem[] = [
  { label: 'Platform overview', to: '/platform', icon: Gauge, roles: ['SUPER_ADMIN'] },
  { label: 'Institutions', to: '/platform/institutions', icon: Building2, roles: ['SUPER_ADMIN'] },
  { label: 'Institution admins', to: '/platform/administrators', icon: Users, roles: ['SUPER_ADMIN'] },
  { label: 'Institution overview', to: '/institution', icon: LayoutDashboard, roles: allStaff },
  { label: 'User accounts', to: '/institution/users', icon: Users, roles: ['INSTITUTION_ADMIN'] },
  { label: 'Subjects', to: '/institution/subjects', icon: BookOpenCheck, roles: academicManagers },
  { label: 'Question bank', to: '/institution/questions', icon: FileQuestion, roles: academicManagers },
  { label: 'Examinations', to: '/institution/exams', icon: ClipboardCheck, roles: allStaff },
  { label: 'Live monitoring', to: '/institution/monitoring', icon: MonitorCheck, roles: allStaff },
  { label: 'Results', to: '/institution/results', icon: ScrollText, roles: allStaff },
  { label: 'Audit trail', to: '/institution/audit', icon: History, roles: ['INSTITUTION_ADMIN'] },
  { label: 'Webhooks', to: '/institution/webhooks', icon: RadioTower, roles: ['INSTITUTION_ADMIN'] },
  { label: 'Student overview', to: '/student', icon: LayoutDashboard, roles: ['STUDENT'] },
  { label: 'My examinations', to: '/student/exams', icon: ClipboardCheck, roles: ['STUDENT'] },
]

export const navigationFor = (user: CurrentUser) => navigationItems.filter((item) => item.roles.some((role) => user.roles.includes(role)))

export const primaryRoleFor = (user: CurrentUser): Role => {
  const precedence: Role[] = ['SUPER_ADMIN', 'INSTITUTION_ADMIN', 'EXAMINER', 'INVIGILATOR', 'STUDENT']
  return precedence.find((role) => user.roles.includes(role)) ?? 'STUDENT'
}
