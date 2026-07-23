import { Braces, Search } from 'lucide-react'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Pagination } from '../../components/data-display/pagination'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { Input } from '../../components/ui/input'
import { Modal } from '../../components/ui/modal'
import { Select } from '../../components/ui/select'
import { friendlyApiError } from '../../lib/api/form-errors'
import { auditActions, auditResourceTypes, type AuditEvent } from '../../types/operations'
import { auditApi, type AuditFilters } from './audit-api'
import { validateHistoricalRange } from '../../lib/date-ranges'

const label = (value: string) => value.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (character) => character.toUpperCase())

export const AuditPage = () => {
  const [page, setPage] = useState(0)
  const [draft, setDraft] = useState({ action: '', resourceType: '', actorId: '', from: '', to: '' })
  const [filters, setFilters] = useState<Omit<AuditFilters, 'page' | 'size'>>({})
  const [dateError, setDateError] = useState('')
  const [nowLocal] = useState(() => {
    const now = new Date()
    return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
  })
  const [detail, setDetail] = useState<AuditEvent | null>(null)
  const query = useQuery({ queryKey: ['audit', filters, page], queryFn: () => auditApi.list({ ...filters, page, size: 20 }) })
  const apply = () => {
    const invalid = validateHistoricalRange(draft.from, draft.to, Date.now())
    if (invalid) { setDateError(invalid); return }
    setDateError('')
    setFilters({
      action: draft.action || undefined, resourceType: draft.resourceType || undefined,
      actorId: draft.actorId.trim() || undefined,
      from: draft.from ? new Date(draft.from).toISOString() : undefined,
      to: draft.to ? new Date(draft.to).toISOString() : undefined,
    })
    setPage(0)
  }
  const clear = () => { setDraft({ action: '', resourceType: '', actorId: '', from: '', to: '' }); setFilters({}); setDateError(''); setPage(0) }
  return <div>
    <PageHeader eyebrow="Institution governance" title="Immutable audit trail" description="Review tenant-scoped administrative and security actions. Audit records cannot be edited or deleted." />
    <Card className="filter-toolbar audit-filters">
      <Select id="audit-action" label="Action" value={draft.action} onChange={(event) => setDraft({ ...draft, action: event.target.value })}><option value="">All actions</option>{auditActions.map((item) => <option key={item} value={item}>{label(item)}</option>)}</Select>
      <Select id="audit-resource" label="Resource" value={draft.resourceType} onChange={(event) => setDraft({ ...draft, resourceType: event.target.value })}><option value="">All resources</option>{auditResourceTypes.map((item) => <option key={item} value={item}>{label(item)}</option>)}</Select>
      <Input id="audit-actor" label="Actor ID" value={draft.actorId} onChange={(event) => setDraft({ ...draft, actorId: event.target.value })} />
      <Input id="audit-from" label="From" type="datetime-local" max={draft.to || nowLocal} value={draft.from} onChange={(event) => setDraft({ ...draft, from: event.target.value })} />
      <Input id="audit-to" label="To" type="datetime-local" min={draft.from || undefined} max={nowLocal} value={draft.to} onChange={(event) => setDraft({ ...draft, to: event.target.value })} />
      <div className="filter-toolbar__actions"><Button icon={<Search size={16} />} onClick={apply}>Apply filters</Button><Button variant="secondary" onClick={clear}>Clear filters</Button></div>
      {dateError && <p className="filter-toolbar__error" role="alert">{dateError}</p>}
    </Card>
    {query.isPending ? <LoadingSkeleton /> : query.isError ? <EmptyState title="Audit events could not be loaded" description={friendlyApiError(query.error)} /> : query.data.content.length === 0 ? <EmptyState title="No audit events found" description="No immutable records match the supported filters." /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Time</th><th>Action</th><th>Resource</th><th>Actor</th><th>Outcome</th><th><span className="sr-only">Details</span></th></tr></thead><tbody>
        {query.data.content.map((event) => <tr key={event.id}><td data-label="Time">{new Date(event.occurredAt).toLocaleString()}</td><td data-label="Action"><strong>{label(event.action)}</strong></td><td data-label="Resource">{label(event.resourceType)}<small>{event.resourceId ?? '—'}</small></td><td data-label="Actor">{event.actorId ?? 'System'}<small>{event.actorRoles || 'No business role'}</small></td><td data-label="Outcome"><Badge tone={event.outcome === 'SUCCESS' ? 'success' : 'warning'}>{event.outcome}</Badge></td><td className="row-actions"><Button size="sm" variant="ghost" icon={<Braces size={15} />} onClick={() => setDetail(event)}>Details</Button></td></tr>)}
      </tbody></table></Card>
      <Pagination page={query.data.page} totalPages={query.data.totalPages} totalElements={query.data.totalElements} onPage={setPage} />
    </>}
    <Modal open={Boolean(detail)} title="Audit event details" onClose={() => setDetail(null)}>
      {detail && <dl className="detail-list"><div><dt>Request ID</dt><dd>{detail.requestId}</dd></div><div><dt>Institution ID</dt><dd>{detail.institutionId}</dd></div><div><dt>Event ID</dt><dd>{detail.id}</dd></div></dl>}
      <pre className="metadata-viewer">{JSON.stringify(detail?.metadata ?? {}, null, 2)}</pre>
    </Modal>
  </div>
}
