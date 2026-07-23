import { Clipboard, KeyRound, Plus, RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pagination } from '../../components/data-display/pagination'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { Notice, type NoticeValue } from '../../components/feedback/notice'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { Input } from '../../components/ui/input'
import { Modal } from '../../components/ui/modal'
import { Select } from '../../components/ui/select'
import { friendlyApiError } from '../../lib/api/form-errors'
import type { MonitoringEventType, WebhookDeliveryStatus, WebhookSecret } from '../../types/operations'
import { webhooksApi } from './webhooks-api'

const eventTypes: MonitoringEventType[] = ['TAB_HIDDEN', 'WINDOW_BLUR', 'FULLSCREEN_EXIT', 'COPY_ATTEMPT', 'PASTE_ATTEMPT', 'DEVTOOLS_SUSPECTED', 'NETWORK_DISCONNECTED', 'NETWORK_RECONNECTED', 'HEARTBEAT_MISSED']

const SecretDialog = ({ value, onClose }: { value: WebhookSecret | null; onClose(): void }) => <Modal open={Boolean(value)} title="Save signing secret now" onClose={onClose}>
  <p>This secret is displayed once. Store it in the receiving service’s secret manager; it cannot be retrieved later.</p>
  {value && <div className="one-time-secret"><code data-testid="one-time-secret">{value.secret}</code><Button variant="secondary" icon={<Clipboard size={16} />} onClick={() => void navigator.clipboard.writeText(value.secret)}>Copy secret</Button></div>}
  <div className="modal-actions"><Button onClick={onClose}>I have stored it securely</Button></div>
</Modal>

export const WebhooksPage = () => {
  const client = useQueryClient()
  const [subscriptionPage, setSubscriptionPage] = useState(0)
  const [deliveryPage, setDeliveryPage] = useState(0)
  const [deliveryStatus, setDeliveryStatus] = useState<WebhookDeliveryStatus | ''>('')
  const [createOpen, setCreateOpen] = useState(false)
  const [form, setForm] = useState({ name: '', destinationUrl: '', eventTypes: [] as MonitoringEventType[] })
  const [secret, setSecret] = useState<WebhookSecret | null>(null)
  const [notice, setNotice] = useState<NoticeValue>(null)
  const subscriptions = useQuery({ queryKey: ['webhooks', 'subscriptions', subscriptionPage], queryFn: () => webhooksApi.subscriptions(subscriptionPage) })
  const deliveries = useQuery({ queryKey: ['webhooks', 'deliveries', deliveryStatus, deliveryPage], queryFn: () => webhooksApi.deliveries(deliveryStatus, deliveryPage) })
  const statusMutation = useMutation({ mutationFn: ({ id, status }: { id: string; status: 'ACTIVE' | 'PAUSED' }) => webhooksApi.status(id, status), onSuccess: () => client.invalidateQueries({ queryKey: ['webhooks'] }) })
  const retryMutation = useMutation({ mutationFn: webhooksApi.retry, onSuccess: () => client.invalidateQueries({ queryKey: ['webhooks', 'deliveries'] }) })
  const create = async () => {
    try {
      const response = await webhooksApi.create(form)
      setCreateOpen(false); setForm({ name: '', destinationUrl: '', eventTypes: [] }); setSecret(response)
      await client.invalidateQueries({ queryKey: ['webhooks', 'subscriptions'] })
    } catch (error) { setNotice({ tone: 'error', message: friendlyApiError(error) }) }
  }
  const rotate = async (id: string) => {
    try {
      const response = await webhooksApi.rotate(id)
      setSecret(response)
      await client.invalidateQueries({ queryKey: ['webhooks', 'subscriptions'] })
    } catch (error) { setNotice({ tone: 'error', message: friendlyApiError(error) }) }
  }
  const unavailable = subscriptions.isError && 'status' in (subscriptions.error as object) && (subscriptions.error as { status?: number }).status === 404
  return <div>
    <PageHeader eyebrow="Secure integrations" title="Monitoring webhooks" description="Configure institution-owned HTTPS receivers and inspect delivery attempts. Signing secrets are returned only at creation or rotation." actions={<Button icon={<Plus size={16} />} onClick={() => setCreateOpen(true)}>New subscription</Button>} />
    <Notice notice={notice} />
    {unavailable ? <EmptyState title="Webhook delivery is disabled" description="The backend webhook feature is not enabled in this environment. No configuration has been fabricated." /> : subscriptions.isPending ? <LoadingSkeleton /> : subscriptions.isError ? <EmptyState title="Subscriptions could not be loaded" description={friendlyApiError(subscriptions.error)} /> : subscriptions.data.content.length === 0 ? <EmptyState title="No webhook subscriptions" description="Create a receiver only after its destination and secret-storage process are ready." /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Name</th><th>Destination</th><th>Events</th><th>Status</th><th>Secret version</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
        {subscriptions.data.content.map((item) => <tr key={item.id}><td data-label="Name"><strong>{item.name}</strong></td><td data-label="Destination">{item.destinationUrl}</td><td data-label="Events">{item.allEventTypes ? 'All monitoring events' : `${item.eventTypes.length} selected`}</td><td data-label="Status"><Badge tone={item.status === 'ACTIVE' ? 'success' : 'warning'}>{item.status}</Badge></td><td data-label="Secret version">{item.secretVersion}</td><td className="row-actions"><Button size="sm" variant="ghost" onClick={() => statusMutation.mutate({ id: item.id, status: item.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE' })}>{item.status === 'ACTIVE' ? 'Pause' : 'Activate'}</Button><Button size="sm" variant="ghost" icon={<KeyRound size={15} />} onClick={() => void rotate(item.id)}>Rotate</Button></td></tr>)}
      </tbody></table></Card><Pagination page={subscriptions.data.page} totalPages={subscriptions.data.totalPages} totalElements={subscriptions.data.totalElements} onPage={setSubscriptionPage} />
    </>}
    <PageHeader eyebrow="Delivery operations" title="Delivery history" description="Inspect backend delivery state and retry only failed or dead-letter attempts." />
    <Card className="operations-selector"><Select id="delivery-status" label="Delivery status" value={deliveryStatus} onChange={(event) => { setDeliveryStatus(event.target.value as WebhookDeliveryStatus | ''); setDeliveryPage(0) }}><option value="">All statuses</option>{['PENDING', 'IN_FLIGHT', 'SUCCEEDED', 'FAILED', 'DEAD_LETTER'].map((status) => <option key={status}>{status}</option>)}</Select></Card>
    {deliveries.isPending ? <LoadingSkeleton /> : deliveries.isError ? <EmptyState title="Deliveries could not be loaded" description={friendlyApiError(deliveries.error)} /> : deliveries.data.content.length === 0 ? <EmptyState title="No deliveries found" description="No webhook delivery matches the selected status." /> : <><Card className="responsive-data"><table><thead><tr><th>Event</th><th>Status</th><th>Attempts</th><th>HTTP</th><th>Next attempt</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{deliveries.data.content.map((item) => <tr key={item.id}><td data-label="Event"><strong>{item.eventType.replaceAll('_', ' ')}</strong><small>{item.eventId}</small></td><td data-label="Status"><Badge tone={item.status === 'SUCCEEDED' ? 'success' : item.status === 'FAILED' || item.status === 'DEAD_LETTER' ? 'warning' : 'neutral'}>{item.status}</Badge></td><td data-label="Attempts">{item.attemptCount}</td><td data-label="HTTP">{item.responseStatus ?? '—'}<small>{item.failureReason ?? ''}</small></td><td data-label="Next">{item.nextAttemptAt ? new Date(item.nextAttemptAt).toLocaleString() : '—'}</td><td className="row-actions">{(item.status === 'FAILED' || item.status === 'DEAD_LETTER') && <Button size="sm" variant="ghost" icon={<RefreshCw size={15} />} onClick={() => retryMutation.mutate(item.id)}>Retry</Button>}</td></tr>)}</tbody></table></Card><Pagination page={deliveries.data.page} totalPages={deliveries.data.totalPages} totalElements={deliveries.data.totalElements} onPage={setDeliveryPage} /></>}
    <Modal open={createOpen} title="New webhook subscription" onClose={() => { setCreateOpen(false); setForm({ name: '', destinationUrl: '', eventTypes: [] }) }}>
      <div className="management-form"><Input id="webhook-name" label="Name" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /><Input id="webhook-url" label="Destination URL" type="url" value={form.destinationUrl} onChange={(event) => setForm({ ...form, destinationUrl: event.target.value })} /><fieldset className="checkbox-grid"><legend>Events (none selected means all)</legend>{eventTypes.map((type) => <label key={type}><input type="checkbox" checked={form.eventTypes.includes(type)} onChange={() => setForm({ ...form, eventTypes: form.eventTypes.includes(type) ? form.eventTypes.filter((item) => item !== type) : [...form.eventTypes, type] })} /> {type.replaceAll('_', ' ')}</label>)}</fieldset><div className="modal-actions"><Button variant="secondary" onClick={() => setCreateOpen(false)}>Cancel</Button><Button onClick={() => void create()} disabled={!form.name.trim() || !form.destinationUrl.trim()}>Create securely</Button></div></div>
    </Modal>
    <SecretDialog value={secret} onClose={() => setSecret(null)} />
  </div>
}
