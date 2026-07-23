import { describe, expect, it } from 'vitest'
import { navigationFor } from './navigation'

describe('role-aware navigation', () => {
  it('keeps platform management exclusive to super administrators', () => {
    const labels = navigationFor({ id: '0', email: 'platform@example.edu', institutionId: null, roles: ['SUPER_ADMIN'] }).map((item) => item.label)
    expect(labels).toEqual(['Platform overview', 'Institutions', 'Institution admins'])
    expect(labels).not.toContain('User accounts')
  })

  it('limits governance operations to institution administrators', () => {
    const admin = navigationFor({ id: '3', email: 'admin@example.edu', institutionId: 'i1', roles: ['INSTITUTION_ADMIN'] }).map((item) => item.to)
    const examiner = navigationFor({ id: '2', email: 'examiner@example.edu', institutionId: 'i1', roles: ['EXAMINER'] }).map((item) => item.to)
    expect(admin).toContain('/institution/audit')
    expect(admin).toContain('/institution/webhooks')
    expect(examiner).not.toContain('/institution/audit')
    expect(examiner).not.toContain('/institution/webhooks')
    expect(examiner).not.toContain('/institution/monitoring')
    expect(examiner).toContain('/institution/results')
  })

  it('shows student navigation without staff administration links', () => {
    const labels = navigationFor({ id: '1', email: 'student@example.edu', institutionId: 'i1', roles: ['STUDENT'] }).map((item) => item.label)
    expect(labels).toEqual(['Student overview', 'My examinations', 'My results'])
    expect(labels).not.toContain('Question bank')
    expect(labels).not.toContain('Results')
  })

  it('shows institution-admin-only links only to institution administrators', () => {
    const examinerLabels = navigationFor({ id: '2', email: 'examiner@example.edu', institutionId: 'i1', roles: ['EXAMINER'] }).map((item) => item.label)
    const adminLabels = navigationFor({ id: '3', email: 'admin@example.edu', institutionId: 'i1', roles: ['INSTITUTION_ADMIN'] }).map((item) => item.label)
    expect(examinerLabels).not.toContain('User accounts')
    expect(examinerLabels).not.toContain('Audit trail')
    expect(adminLabels).toContain('User accounts')
    expect(adminLabels).toContain('Audit trail')
  })

  it('matches academic and examination navigation to backend staff roles', () => {
    const examinerLabels = navigationFor({ id: '2', email: 'examiner@example.edu', institutionId: 'i1', roles: ['EXAMINER'] }).map((item) => item.label)
    const invigilatorLabels = navigationFor({ id: '4', email: 'invigilator@example.edu', institutionId: 'i1', roles: ['INVIGILATOR'] }).map((item) => item.label)
    expect(examinerLabels).toEqual(expect.arrayContaining(['Subjects', 'Question bank', 'Examinations']))
    expect(invigilatorLabels).toContain('Examinations')
    expect(invigilatorLabels).toContain('Live monitoring')
    expect(invigilatorLabels).not.toContain('Results')
    expect(invigilatorLabels).not.toContain('Subjects')
    expect(invigilatorLabels).not.toContain('Question bank')
  })
})
