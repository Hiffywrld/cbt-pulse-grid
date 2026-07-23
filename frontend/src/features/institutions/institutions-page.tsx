import { Eye, Pencil, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import { Pagination } from '../../components/data-display/pagination'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { Notice, type NoticeValue } from '../../components/feedback/notice'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { ConfirmDialog } from '../../components/ui/confirm-dialog'
import { Input } from '../../components/ui/input'
import { Modal } from '../../components/ui/modal'
import { Select } from '../../components/ui/select'
import { friendlyApiError } from '../../lib/api/form-errors'
import type { Institution, InstitutionStatus } from '../../types/management'
import { InstitutionForm } from './institution-form'
import { useInstitutionMutations, useInstitutions } from './institution-hooks'

const tone = (status: InstitutionStatus) => status === 'ACTIVE' ? 'success' : status === 'SUSPENDED' ? 'warning' : 'neutral'

export const InstitutionsPage = () => {
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<InstitutionStatus | ''>('')
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState<'create' | 'edit' | 'view' | null>(null)
  const [selected, setSelected] = useState<Institution | null>(null)
  const [confirm, setConfirm] = useState<Institution | null>(null)
  const [notice, setNotice] = useState<NoticeValue>(null)
  const query = useInstitutions({ search, status, page, size: 20 })
  const mutations = useInstitutionMutations()
  const open = (kind: 'edit' | 'view', institution: Institution) => { setSelected(institution); setModal(kind) }
  const submitSearch = (event: React.FormEvent) => { event.preventDefault(); setPage(0); setSearch(searchDraft.trim()) }
  const changeStatus = async () => {
    if (!confirm) return
    const next: InstitutionStatus = confirm.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE'
    try { await mutations.status.mutateAsync({ id: confirm.id, status: next }); setNotice({ tone: 'success', message: `${confirm.name} is now ${next.toLowerCase()}.` }); setConfirm(null) } catch (error) { setNotice({ tone: 'error', message: friendlyApiError(error) }); setConfirm(null) }
  }
  return <div>
    <PageHeader eyebrow="Platform administration" title="Institutions" description="Create and manage institutional tenants without deleting their examination history." actions={<Button icon={<Plus size={17} />} onClick={() => { setSelected(null); setModal('create') }}>New institution</Button>} />
    <Notice notice={notice} />
    <Card className="filter-bar"><form onSubmit={submitSearch}><Input id="institution-search" label="Search name or code" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /><Button type="submit" variant="secondary" icon={<Search size={16} />}>Search</Button></form><Select id="institution-status" label="Status" value={status} onChange={(event) => { setStatus(event.target.value as InstitutionStatus | ''); setPage(0) }}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option><option value="SUSPENDED">Suspended</option></Select></Card>
    {query.isPending ? <LoadingSkeleton /> : query.isError ? <EmptyState title="Institutions could not be loaded" description={friendlyApiError(query.error)} action={<Button onClick={() => void query.refetch()}>Try again</Button>} /> : query.data.content.length === 0 ? <EmptyState title="No institutions found" description="Adjust the filters or create the first institution." /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Institution</th><th>Code</th><th>Status</th><th>Updated</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{query.data.content.map((item) => <tr key={item.id}><td data-label="Institution"><strong>{item.name}</strong></td><td data-label="Code">{item.code}</td><td data-label="Status"><Badge tone={tone(item.status)}>{item.status}</Badge></td><td data-label="Updated">{new Date(item.updatedAt).toLocaleDateString()}</td><td className="row-actions"><Button size="sm" variant="ghost" icon={<Eye size={15} />} onClick={() => open('view', item)}>View</Button><Button size="sm" variant="ghost" icon={<Pencil size={15} />} onClick={() => open('edit', item)}>Edit</Button><Button size="sm" variant={item.status === 'ACTIVE' ? 'danger' : 'secondary'} onClick={() => setConfirm(item)}>{item.status === 'ACTIVE' ? 'Suspend' : 'Activate'}</Button></td></tr>)}</tbody></table></Card>
      <Pagination page={query.data.page} totalPages={query.data.totalPages} totalElements={query.data.totalElements} onPage={setPage} />
    </>}
    <Modal open={modal === 'create' || modal === 'edit'} title={modal === 'edit' ? 'Edit institution' : 'Create institution'} onClose={() => setModal(null)}><InstitutionForm institution={modal === 'edit' ? selected ?? undefined : undefined} onCancel={() => setModal(null)} onSubmit={async (values) => { if (selected && modal === 'edit') await mutations.update.mutateAsync({ id: selected.id, name: values.name }); else await mutations.create.mutateAsync(values); setNotice({ tone: 'success', message: modal === 'edit' ? 'Institution updated.' : 'Institution created.' }); setModal(null) }} /></Modal>
    <Modal open={modal === 'view'} title="Institution details" onClose={() => setModal(null)}>{selected ? <dl className="detail-list"><div><dt>Name</dt><dd>{selected.name}</dd></div><div><dt>Code</dt><dd>{selected.code}</dd></div><div><dt>Status</dt><dd>{selected.status}</dd></div><div><dt>Created</dt><dd>{new Date(selected.createdAt).toLocaleString()}</dd></div></dl> : null}</Modal>
    <ConfirmDialog open={Boolean(confirm)} title={confirm?.status === 'ACTIVE' ? 'Suspend institution?' : 'Activate institution?'} description={confirm?.status === 'ACTIVE' ? 'Access will be suspended while historical records remain available.' : 'This restores active institutional access.'} confirmLabel={confirm?.status === 'ACTIVE' ? 'Suspend' : 'Activate'} loading={mutations.status.isPending} onClose={() => setConfirm(null)} onConfirm={() => void changeStatus()} />
  </div>
}
