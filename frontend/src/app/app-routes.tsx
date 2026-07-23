import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from '../components/layout/app-shell'
import { LoginPage } from '../features/auth/login-page'
import { ProtectedRoute } from '../features/auth/protected-route'
import { RoleRoute } from '../features/auth/role-route'
import { homeForUser } from '../features/auth/role-routing'
import { useAuth } from '../features/auth/use-auth'
import { DashboardPage } from '../features/dashboard/dashboard-page'
import type { Role } from '../types/auth'
import { NotFoundPage, UnauthorizedPage } from './error-pages'
import { PlaceholderPage } from './placeholder-page'

const RootRedirect = () => {
  const { status, user } = useAuth()
  if (status === 'restoring') return null
  return <Navigate to={status === 'authenticated' && user ? homeForUser(user) : '/login'} replace />
}

const placeholders = {
  institutions: ['Institutions', 'Create and manage institutional tenants.'], users: ['User accounts', 'Manage institution-scoped staff and student accounts.'], subjects: ['Subjects', 'Organize the institution question bank by subject.'], questions: ['Question bank', 'Create and publish validated examination questions.'], exams: ['Examinations', 'Configure schedules, pools, candidates and exam lifecycle.'], monitoring: ['Live monitoring', 'Observe candidate connectivity and anti-cheat signals.'], results: ['Results', 'Review secure assessment outcomes and exports.'], audit: ['Audit trail', 'Review immutable operational and security events.'], studentExams: ['My examinations', 'View assigned examinations and availability.'], studentResults: ['My results', 'View your submitted examination outcomes.'],
} as const

const Placeholder = ({ page }: { page: keyof typeof placeholders }) => {
  const [title, description] = placeholders[page]
  return <PlaceholderPage title={title} description={description} />
}

const RoleOnly = ({ roles, page }: { roles: readonly Role[]; page: keyof typeof placeholders }) => {
  const { user } = useAuth()
  return user && roles.some((role) => user.roles.includes(role)) ? <Placeholder page={page} /> : <Navigate to="/unauthorized" replace />
}

export const AppRoutes = () => (
  <Routes>
    <Route path="/" element={<RootRedirect />} />
    <Route path="/login" element={<LoginPage />} />
    <Route element={<ProtectedRoute />}>
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route element={<RoleRoute roles={['SUPER_ADMIN']} />}><Route element={<AppShell />}><Route path="/platform" element={<DashboardPage area="platform" />} /><Route path="/platform/institutions" element={<Placeholder page="institutions" />} /></Route></Route>
      <Route element={<RoleRoute roles={['INSTITUTION_ADMIN', 'EXAMINER', 'INVIGILATOR']} />}>
        <Route element={<AppShell />}>
          <Route path="/institution" element={<DashboardPage area="institution" />} />
          <Route path="/institution/users" element={<RoleOnly roles={['INSTITUTION_ADMIN']} page="users" />} />
          <Route path="/institution/subjects" element={<RoleOnly roles={['INSTITUTION_ADMIN', 'EXAMINER']} page="subjects" />} />
          <Route path="/institution/questions" element={<RoleOnly roles={['INSTITUTION_ADMIN', 'EXAMINER']} page="questions" />} />
          <Route path="/institution/exams" element={<Placeholder page="exams" />} />
          <Route path="/institution/monitoring" element={<Placeholder page="monitoring" />} />
          <Route path="/institution/results" element={<Placeholder page="results" />} />
          <Route path="/institution/audit" element={<RoleOnly roles={['INSTITUTION_ADMIN']} page="audit" />} />
        </Route>
      </Route>
      <Route element={<RoleRoute roles={['STUDENT']} />}><Route element={<AppShell />}><Route path="/student" element={<DashboardPage area="student" />} /><Route path="/student/exams" element={<Placeholder page="studentExams" />} /><Route path="/student/results" element={<Placeholder page="studentResults" />} /></Route></Route>
    </Route>
    <Route path="*" element={<NotFoundPage />} />
  </Routes>
)
