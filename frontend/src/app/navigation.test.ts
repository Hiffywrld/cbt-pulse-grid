import { describe, expect, it } from 'vitest'
import { navigationFor } from './navigation'

describe('role-aware navigation', () => {
  it('shows student navigation without staff administration links', () => {
    const labels = navigationFor({ id: '1', email: 'student@example.edu', institutionId: 'i1', roles: ['STUDENT'] }).map((item) => item.label)
    expect(labels).toEqual(['Student overview', 'My examinations', 'My results'])
    expect(labels).not.toContain('Question bank')
  })

  it('shows institution-admin-only links only to institution administrators', () => {
    const examinerLabels = navigationFor({ id: '2', email: 'examiner@example.edu', institutionId: 'i1', roles: ['EXAMINER'] }).map((item) => item.label)
    const adminLabels = navigationFor({ id: '3', email: 'admin@example.edu', institutionId: 'i1', roles: ['INSTITUTION_ADMIN'] }).map((item) => item.label)
    expect(examinerLabels).not.toContain('User accounts')
    expect(examinerLabels).not.toContain('Audit trail')
    expect(adminLabels).toContain('User accounts')
    expect(adminLabels).toContain('Audit trail')
  })
})
