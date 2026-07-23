import { ArrowLeft, Braces } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Pagination } from '../../components/data-display/pagination'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { Modal } from '../../components/ui/modal'
import { friendlyApiError } from '../../lib/api/form-errors'
import type { MonitoringEvent } from '../../types/operations'
import { useMonitoringEvents } from './monitoring-hooks'

export const MonitoringEventsPage = () => {
  const { attemptId = '' } = useParams()
  const [page, setPage] = useState(0)
  const [detail, setDetail] = useState<MonitoringEvent | null>(null)
  const events = useMonitoringEvents(attemptId, page)
  return <div>
    <Link className="back-link" to="/institution/monitoring"><ArrowLeft size={16} /> Back to monitoring</Link>
    <PageHeader eyebrow="Immutable monitoring history" title="Attempt events" description={`Attempt ${attemptId}`} />
    {events.isPending ? <LoadingSkeleton /> : events.isError ? <EmptyState title="Events could not be loaded" description={friendlyApiError(events.error)} /> : events.data.content.length === 0 ? <EmptyState title="No monitoring events" description="No persistent monitoring signals have been recorded for this attempt." /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Event</th><th>Occurred</th><th>Received</th><th>Risk applied</th><th><span className="sr-only">Details</span></th></tr></thead><tbody>
        {events.data.content.map((event) => <tr key={event.id}><td data-label="Event"><strong>{event.eventType.replaceAll('_', ' ')}</strong></td><td data-label="Occurred">{new Date(event.occurredAt).toLocaleString()}</td><td data-label="Received">{new Date(event.receivedAt).toLocaleString()}</td><td data-label="Risk"><Badge tone={event.riskPointsApplied ? 'warning' : 'neutral'}>{event.riskPointsApplied} points</Badge></td><td className="row-actions"><Button size="sm" variant="ghost" icon={<Braces size={15} />} onClick={() => setDetail(event)}>Metadata</Button></td></tr>)}
      </tbody></table></Card>
      <Pagination page={events.data.page} totalPages={events.data.totalPages} totalElements={events.data.totalElements} onPage={setPage} />
    </>}
    <Modal open={Boolean(detail)} title="Monitoring event details" onClose={() => setDetail(null)}>
      {detail && <dl className="detail-list"><div><dt>Event ID</dt><dd>{detail.id}</dd></div><div><dt>Client event ID</dt><dd>{detail.clientEventId}</dd></div><div><dt>Backend weight</dt><dd>{detail.riskWeight}</dd></div><div><dt>Applied</dt><dd>{detail.riskPointsApplied}</dd></div></dl>}
      <pre className="metadata-viewer">{JSON.stringify(detail?.metadata ?? {}, null, 2)}</pre>
    </Modal>
  </div>
}
