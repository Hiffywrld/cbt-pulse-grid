import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AppShell } from '../components/layout/app-shell'
import { LoginPage } from '../features/auth/login-page'
import { ProtectedRoute } from '../features/auth/protected-route'
import { RoleRoute } from '../features/auth/role-route'
import { homeForUser } from '../features/auth/role-routing'
import { useAuth } from '../features/auth/use-auth'
import { DashboardPage } from '../features/dashboard/dashboard-page'
import { ExamDetailPage } from '../features/exams/exam-detail-page'
import { ExamsPage } from '../features/exams/exams-page'
import { InstitutionsPage } from '../features/institutions/institutions-page'
import { QuestionsPage } from '../features/questions/questions-page'
import { AttemptReviewPage } from '../features/results/attempt-review-page'
import { ExamResultsPage } from '../features/results/exam-results-page'
import { ResultsPage } from '../features/results/results-page'
import { StudentAttemptPage } from '../features/student/student-attempt-page'
import { StudentAttemptResultPage } from '../features/student/student-attempt-result-page'
import { StudentExamDetailPage } from '../features/student/student-exam-detail-page'
import { StudentExamStartPage } from '../features/student/student-exam-start-page'
import { StudentExamsPage } from '../features/student/student-exams-page'
import { SubjectsPage } from '../features/subjects/subjects-page'
import { UsersPage } from '../features/users/users-page'
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

const RoleOnly = ({ roles, page, element }: { roles: readonly Role[]; page?: keyof typeof placeholders; element?: ReactNode }) => {
  const { user } = useAuth()
  return user && roles.some((role) => user.roles.includes(role)) ? (element ?? (page ? <Placeholder page={page} /> : null)) : <Navigate to="/unauthorized" replace />
}

export const AppRoutes = () => (
  <Routes>
    <Route path="/" element={<RootRedirect />} />
    <Route path="/login" element={<LoginPage />} />
    <Route element={<ProtectedRoute />}>
      <Route path="/unauthorized" element={<UnauthorizedPage />} />
      <Route element={<RoleRoute roles={['SUPER_ADMIN']} />}><Route element={<AppShell />}><Route path="/platform" element={<DashboardPage area="platform" />} /><Route path="/platform/institutions" element={<InstitutionsPage />} /><Route path="/platform/administrators" element={<UsersPage mode="platform-admins" />} /></Route></Route>
      <Route element={<RoleRoute roles={['INSTITUTION_ADMIN', 'EXAMINER', 'INVIGILATOR']} />}>
        <Route element={<AppShell />}>
          <Route path="/institution" element={<DashboardPage area="institution" />} />
          <Route path="/institution/users" element={<RoleOnly roles={['INSTITUTION_ADMIN']} element={<UsersPage mode="institution" />} />} />
          <Route path="/institution/subjects" element={<RoleOnly roles={['INSTITUTION_ADMIN', 'EXAMINER']} element={<SubjectsPage />} />} />
          <Route path="/institution/questions" element={<RoleOnly roles={['INSTITUTION_ADMIN', 'EXAMINER']} element={<QuestionsPage />} />} />
          <Route path="/institution/exams" element={<ExamsPage />} />
          <Route path="/institution/exams/:examId" element={<ExamDetailPage />} />
          <Route path="/institution/monitoring" element={<Placeholder page="monitoring" />} />
          <Route path="/institution/results" element={<ResultsPage />} />
          <Route path="/institution/results/exams/:examId" element={<ExamResultsPage />} />
          <Route path="/institution/results/attempts/:attemptId" element={<AttemptReviewPage />} />
          <Route path="/institution/audit" element={<RoleOnly roles={['INSTITUTION_ADMIN']} page="audit" />} />
        </Route>
      </Route>
      <Route element={<RoleRoute roles={['STUDENT']} />}>
        <Route element={<AppShell />}><Route path="/student" element={<DashboardPage area="student" />} /><Route path="/student/exams" element={<StudentExamsPage />} /><Route path="/student/exams/:examId" element={<StudentExamDetailPage />} /><Route path="/student/exams/:examId/start" element={<StudentExamStartPage />} /></Route>
        <Route path="/student/attempts/:attemptId" element={<StudentAttemptPage />} />
        <Route element={<AppShell />}><Route path="/student/attempts/:attemptId/result" element={<StudentAttemptResultPage />} /></Route>
      </Route>
    </Route>
    <Route path="*" element={<NotFoundPage />} />
  </Routes>
)
