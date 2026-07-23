import { ArrowRight, Building2, ClipboardCheck, FileQuestion, MonitorCheck, ShieldCheck, Wifi } from 'lucide-react'
import { Link } from 'react-router-dom'
import { primaryRoleFor } from '../../app/navigation'
import { StatCard } from '../../components/data-display/stat-card'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Card } from '../../components/ui/card'
import { roleLabel } from '../auth/role-routing'
import { firstNameFor, fullNameFor, institutionCodeFor, institutionNameFor } from '../auth/identity-display'
import { useAuth } from '../auth/use-auth'

type Shortcut = { title: string; description: string; to: string; icon: typeof Building2 }
const shortcutsByArea: Record<'platform' | 'institution' | 'student', Shortcut[]> = {
  platform: [{ title: 'Institution network', description: 'Institution management integration arrives in the next frontend phase.', to: '/platform/institutions', icon: Building2 }],
  institution: [
    { title: 'Prepare examinations', description: 'Authoring and examination-management screens are coming next.', to: '/institution/exams', icon: ClipboardCheck },
    { title: 'Build the question bank', description: 'Subject and question workflows will use the secured backend APIs.', to: '/institution/questions', icon: FileQuestion },
    { title: 'Monitor live sessions', description: 'The invigilator workspace will connect to authenticated STOMP updates.', to: '/institution/monitoring', icon: MonitorCheck },
  ],
  student: [{ title: 'My examinations', description: 'Candidate-safe exam discovery and attempt delivery arrive in a later phase.', to: '/student/exams', icon: ClipboardCheck }],
}

export const DashboardPage = ({ area }: { area: 'platform' | 'institution' | 'student' }) => {
  const { user } = useAuth()
  if (!user) return null
  const role = primaryRoleFor(user)
  const institutionName = institutionNameFor(user)
  const institutionCode = institutionCodeFor(user)
  return (
    <div className="dashboard-page">
      <PageHeader eyebrow={area === 'platform' ? 'Platform operations' : area === 'student' ? 'Candidate workspace' : 'Institution workspace'} title={`Welcome, ${firstNameFor(user)}`} description="Your secure CBT-Pulse Grid workspace is ready. Only features authorized for your account are shown." actions={<Badge tone="info">{roleLabel(role)}</Badge>} />
      <section className="status-grid" aria-label="Current workspace status">
        <StatCard label="Session" value="Authenticated" detail="Access token active" icon={ShieldCheck} />
        <StatCard label="Network" value={navigator.onLine ? 'Online' : 'Offline'} detail="Browser connectivity" icon={Wifi} />
        <StatCard label="Institution" value={institutionName} detail={institutionCode ?? (user.institutionId ? 'Institution account' : 'Platform-wide access')} icon={Building2} />
      </section>
      <section className="section-block">
        <div className="section-heading"><div><p className="eyebrow">Continue your work</p><h2>Workspace shortcuts</h2></div><p>Phase 1 establishes secure navigation; connected feature screens follow.</p></div>
        <div className="shortcut-grid">
          {shortcutsByArea[area].map(({ title, description, to, icon: Icon }) => (
            <Card className="shortcut-card" key={to}><span className="shortcut-card__icon"><Icon aria-hidden="true" /></span><div><h3>{title}</h3><p>{description}</p></div><Link to={to}>Open preview <ArrowRight size={16} aria-hidden="true" /></Link></Card>
          ))}
        </div>
      </section>
      <Card className="institution-context">
        <div><p className="eyebrow">Authenticated context</p><h2>Identity supplied by the backend</h2></div>
        <dl><div><dt>Name</dt><dd>{fullNameFor(user)}</dd></div><div><dt>Email</dt><dd>{user.email}</dd></div><div><dt>Role</dt><dd>{roleLabel(role)}</dd></div><div><dt>Institution</dt><dd>{institutionName}{institutionCode ? ` (${institutionCode})` : ''}</dd></div></dl>
      </Card>
    </div>
  )
}
