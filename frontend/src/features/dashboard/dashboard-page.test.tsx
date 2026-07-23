import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import type { CurrentUser } from '../../types/auth'
import { AuthContext } from '../auth/auth-context'
import { DashboardPage } from './dashboard-page'

vi.mock('../institutions/institution-hooks', () => ({ useInstitutions: () => ({ data: { totalElements: 2 } }) }))
vi.mock('../users/user-hooks', () => ({ useUsers: (params: { role?: string }) => ({ data: { totalElements: params.role === 'STUDENT' ? 18 : 24 } }) }))
vi.mock('../student/student-exam-hooks', () => ({ useStudentExams: () => ({ data: [] }) }))

const renderDashboard = (user: CurrentUser, area: 'platform' | 'institution' = 'institution') => render(
  <AuthContext.Provider value={{ status: 'authenticated', user, login: vi.fn(), logout: vi.fn() }}>
    <MemoryRouter><DashboardPage area={area} /></MemoryRouter>
  </AuthContext.Provider>,
)

describe('Dashboard authenticated identity display', () => {
  it('shows an institution user name, role, and human-friendly institution', () => {
    const institutionId = '70d87f4e-4b94-45ef-84eb-4270c2337fa1'
    renderDashboard({
      id: 'user-1',
      email: 'amina@niitlagos.local',
      firstName: 'Amina',
      lastName: 'Okafor',
      institutionId,
      institutionName: 'NIIT Lagos Campus',
      institutionCode: 'NIIT-LAGOS',
      registrationNumber: 'NIIT-2026-001',
      roles: ['INSTITUTION_ADMIN'],
    })

    expect(screen.getByRole('heading', { name: 'Welcome, Amina' })).toBeInTheDocument()
    expect(screen.getByText('Amina Okafor')).toBeInTheDocument()
    expect(screen.getAllByText('Institution Administrator').length).toBeGreaterThan(0)
    expect(screen.getAllByText(/NIIT Lagos Campus/).length).toBeGreaterThan(0)
    expect(screen.getByText('NIIT-2026-001')).toBeInTheDocument()
    expect(screen.queryByText(institutionId)).not.toBeInTheDocument()
  })

  it('shows platform administration for a super administrator without an institution', () => {
    renderDashboard({
      id: 'user-2',
      email: 'admin@example.edu',
      firstName: 'System',
      lastName: 'Administrator',
      institutionId: null,
      institutionName: null,
      institutionCode: null,
      roles: ['SUPER_ADMIN'],
    }, 'platform')

    expect(screen.getByRole('heading', { name: 'Welcome, System' })).toBeInTheDocument()
    expect(screen.getAllByText('Platform administration').length).toBeGreaterThan(0)
  })

  it('falls back safely when optional profile fields are absent', () => {
    const institutionId = 'e147c990-6904-4b2c-aeb5-4975ba4f5664'
    renderDashboard({
      id: 'user-3',
      email: 'staff@example.edu',
      institutionId,
      roles: ['EXAMINER'],
    })

    expect(screen.getByRole('heading', { name: 'Welcome, staff@example.edu' })).toBeInTheDocument()
    expect(screen.getAllByText('Institution workspace').length).toBeGreaterThan(0)
    expect(screen.queryByText(institutionId)).not.toBeInTheDocument()
  })
})
