import { ArrowRight, Building2, ClipboardCheck, FileQuestion, MonitorCheck, ShieldCheck, Users, Wifi } from 'lucide-react'
import { Link } from 'react-router-dom'
import { primaryRoleFor } from '../../app/navigation'
import { StatCard } from '../../components/data-display/stat-card'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Card } from '../../components/ui/card'
import { roleLabel } from '../auth/role-routing'
import { firstNameFor, fullNameFor, institutionCodeFor, institutionNameFor } from '../auth/identity-display'
import { useAuth } from '../auth/use-auth'
import { useInstitutions } from '../institutions/institution-hooks'
import { useStudentExams } from '../student/student-exam-hooks'
import { useUsers } from '../users/user-hooks'
import type { CurrentUser } from '../../types/auth'

type Shortcut = { title: string; description: string; to: string; icon: typeof Building2 }
const shortcutsByArea: Record<'platform' | 'institution' | 'student', Shortcut[]> = {
  platform: [{ title: 'Institution network', description: 'Create, search and manage institutional tenants.', to: '/platform/institutions', icon: Building2 }],
  institution: [
    { title: 'Prepare examinations', description: 'Authoring and examination-management screens are coming next.', to: '/institution/exams', icon: ClipboardCheck },
    { title: 'Build the question bank', description: 'Subject and question workflows will use the secured backend APIs.', to: '/institution/questions', icon: FileQuestion },
    { title: 'Monitor live sessions', description: 'The invigilator workspace will connect to authenticated STOMP updates.', to: '/institution/monitoring', icon: MonitorCheck },
  ],
  student: [{ title: 'My examinations', description: 'Review your real assigned examinations and secure entry instructions.', to: '/student/exams', icon: ClipboardCheck }],
}

export const DashboardPage = ({ area }: { area: 'platform' | 'institution' | 'student' }) => {
  const { user } = useAuth()
  if (!user) return null
  return <AuthenticatedDashboard area={area} user={user} />
}

const AuthenticatedDashboard = ({ area, user }: { area: 'platform' | 'institution' | 'student'; user: CurrentUser }) => {
  const role = primaryRoleFor(user)
  const institutionName = institutionNameFor(user)
  const institutionCode = institutionCodeFor(user)
  const platform = area === 'platform'
  const institutionAdmin = area === 'institution' && user.roles.includes('INSTITUTION_ADMIN')
  const student = area === 'student'
  const institutions = useInstitutions({ page: 0, size: 1 }, platform)
  const managedUsers = useUsers({ page: 0, size: 1, institutionId: platform ? undefined : user.institutionId ?? undefined }, platform || institutionAdmin)
  const students = useUsers({ page: 0, size: 1, role: 'STUDENT' }, institutionAdmin)
  const exams = useStudentExams(student)
  const realStats = platform ? [
    { label: 'Institutions', value: institutions.data?.totalElements.toLocaleString() ?? '-', detail: 'Registered platform tenants', icon: Building2 },
    { label: 'Managed accounts', value: managedUsers.data?.totalElements.toLocaleString() ?? '-', detail: 'Institution-scoped users', icon: Users },
  ] : institutionAdmin ? [
    { label: 'User accounts', value: managedUsers.data?.totalElements.toLocaleString() ?? '-', detail: 'Accounts in your institution', icon: Users },
    { label: 'Students', value: students.data?.totalElements.toLocaleString() ?? '-', detail: 'Registered candidates', icon: ClipboardCheck },
  ] : student ? [
    { label: 'Assigned exams', value: exams.data?.length.toLocaleString() ?? '-', detail: 'Published assignments', icon: ClipboardCheck },
    { label: 'Active now', value: exams.data?.filter((exam) => exam.availability === 'ACTIVE').length.toLocaleString() ?? '-', detail: 'Available examination windows', icon: ShieldCheck },
  ] : []
  return (
    <div className="dashboard-page">
      <PageHeader eyebrow={area === 'platform' ? 'Platform operations' : area === 'student' ? 'Candidate workspace' : 'Institution workspace'} title={`Welcome, ${firstNameFor(user)}`} description="Your secure CBT-Pulse Grid workspace is ready. Only features authorized for your account are shown." actions={<Badge tone="info">{roleLabel(role)}</Badge>} />
      <section className="status-grid" aria-label="Current workspace status">
        {realStats.map((stat) => <StatCard key={stat.label} {...stat} />)}
        <StatCard label="Network" value={navigator.onLine ? 'Online' : 'Offline'} detail="Browser connectivity" icon={Wifi} />
      </section>
      <section className="section-block">
        <div className="section-heading"><div><p className="eyebrow">Continue your work</p><h2>Workspace shortcuts</h2></div><p>Only features available to your authenticated role are shown.</p></div>
        <div className="shortcut-grid">
          {shortcutsByArea[area].map(({ title, description, to, icon: Icon }) => (
            <Card className="shortcut-card" key={to}><span className="shortcut-card__icon"><Icon aria-hidden="true" /></span><div><h3>{title}</h3><p>{description}</p></div><Link to={to}>Open preview <ArrowRight size={16} aria-hidden="true" /></Link></Card>
          ))}
        </div>
      </section>
      <Card className="institution-context">
        <div><p className="eyebrow">Authenticated context</p><h2>Identity supplied by the backend</h2></div>
        <dl><div><dt>Name</dt><dd>{fullNameFor(user)}</dd></div><div><dt>Email</dt><dd>{user.email}</dd></div><div><dt>Role</dt><dd>{roleLabel(role)}</dd></div><div><dt>Institution</dt><dd>{institutionName}{institutionCode ? ` (${institutionCode})` : ''}</dd></div>{user.registrationNumber ? <div><dt>Registration number</dt><dd>{user.registrationNumber}</dd></div> : null}</dl>
      </Card>
    </div>
  )
}
