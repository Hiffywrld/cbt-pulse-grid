import { Activity, Eye, Radio, ShieldAlert } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Pagination } from '../../components/data-display/pagination'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { Select } from '../../components/ui/select'
import { friendlyApiError } from '../../lib/api/form-errors'
import { useExams } from '../exams/exam-hooks'
import { useLiveMonitoring } from './use-live-monitoring'
import { useMonitoringDashboard } from './monitoring-hooks'

const riskTone = (risk: number) => risk >= 60 ? 'high' : risk >= 25 ? 'medium' : 'low'

const LiveTable = ({ examId }: { examId: string }) => {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const dashboard = useMonitoringDashboard(examId, page)
  const connection = useLiveMonitoring(examId)
  return <>
    <div className={`live-status live-status--${connection}`} role="status">
      <Radio size={16} /><strong>{connection === 'connected' ? 'Live updates connected' : connection === 'offline' ? 'Browser offline' : 'Reconnecting live updates'}</strong>
      {connection !== 'connected' && <span>REST reconciliation remains active.</span>}
    </div>
    {dashboard.isPending ? <LoadingSkeleton /> : dashboard.isError
      ? <EmptyState title="Monitoring could not be loaded" description={friendlyApiError(dashboard.error)} />
      : dashboard.data.content.length === 0
        ? <EmptyState title="No active attempts" description="Candidate monitoring states will appear after assigned students begin this examination." />
        : <>
          <Card className="responsive-data"><table><thead><tr><th>Candidate</th><th>Attempt</th><th>Connection</th><th>Browser state</th><th>Events</th><th>Risk</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
            {dashboard.data.content.map((row) => <tr key={row.attemptId}>
              <td data-label="Candidate"><strong>{row.firstName} {row.lastName}</strong><small>{row.registrationNumber ?? row.candidateId}</small></td>
              <td data-label="Attempt"><Badge tone={row.attemptStatus === 'IN_PROGRESS' ? 'info' : 'neutral'}>{row.attemptStatus.replaceAll('_', ' ')}</Badge><small>{row.lastHeartbeatAt ? `Heartbeat ${new Date(row.lastHeartbeatAt).toLocaleString()}` : 'No heartbeat yet'}</small></td>
              <td data-label="Connection"><Badge tone={row.connectivity === 'ONLINE' ? 'success' : row.connectivity === 'OFFLINE' ? 'warning' : 'neutral'}>{row.connectivity}</Badge></td>
              <td data-label="Browser state"><span>{row.focused == null ? 'Focus unknown' : row.focused ? 'Focused' : 'Not focused'}</span><small>{row.fullscreen == null ? 'Fullscreen unknown' : row.fullscreen ? 'Fullscreen' : 'Windowed'}</small></td>
              <td data-label="Events">{row.eventCount}</td>
              <td data-label="Risk"><span className={`risk-score risk-score--${riskTone(row.riskScore)}`}>{row.riskScore}/100</span></td>
              <td className="row-actions"><Button size="sm" variant="ghost" icon={<Eye size={15} />} onClick={() => navigate(`/institution/monitoring/attempts/${row.attemptId}`)}>Events</Button></td>
            </tr>)}
          </tbody></table></Card>
          <Pagination page={dashboard.data.page} totalPages={dashboard.data.totalPages} totalElements={dashboard.data.totalElements} onPage={setPage} />
        </>}
  </>
}

export const MonitoringPage = () => {
  const [examId, setExamId] = useState('')
  const exams = useExams({ status: 'PUBLISHED', page: 0, size: 100 })
  return <div>
    <PageHeader eyebrow="Invigilation operations" title="Live monitoring" description="Review tenant-scoped candidate connectivity and browser signals. Live STOMP updates are reconciled with authoritative REST data." />
    <Card className="operations-selector">
      <Activity size={21} />
      {exams.isPending ? <span>Loading monitorable examinations…</span> : exams.isError
        ? <span>{friendlyApiError(exams.error)}</span>
        : <Select id="monitor-exam" label="Published examination" value={examId} onChange={(event) => setExamId(event.target.value)}>
          <option value="">Choose an examination</option>
          {exams.data.content.map((exam) => <option key={exam.id} value={exam.id}>{exam.code} — {exam.title}</option>)}
        </Select>}
    </Card>
    {!examId ? <EmptyState title="Select an examination" description="The dashboard opens only after an authorized staff member chooses a published examination." /> : <LiveTable examId={examId} />}
    <Card className="role-note"><ShieldAlert size={20} /><p>Risk is a server-calculated signal for review, not proof of misconduct. Network transitions and missed heartbeats carry zero risk in this offline-first platform.</p></Card>
  </div>
}
