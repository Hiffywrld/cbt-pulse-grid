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
import type { Subject, SubjectStatus } from '../../types/academic'
import { useAuth } from '../auth/use-auth'
import { SubjectForm } from './subject-form'
import { useSubjectMutations, useSubjects } from './subject-hooks'

export const SubjectsPage = () => {
  const { user } = useAuth()
  const canManage = Boolean(user?.roles.includes('INSTITUTION_ADMIN'))
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<SubjectStatus | ''>('')
  const [page, setPage] = useState(0)
  const [mode, setMode] = useState<'create' | 'edit' | 'view' | null>(null)
  const [selected, setSelected] = useState<Subject | null>(null)
  const [confirm, setConfirm] = useState<Subject | null>(null)
  const [notice, setNotice] = useState<NoticeValue>(null)
  const query = useSubjects({ search, status, page, size: 20 })
  const mutations = useSubjectMutations()
  const submitSearch = (event: React.FormEvent) => {
    event.preventDefault(); setPage(0); setSearch(searchDraft.trim())
  }
  const submit = async (body: { code: string; name: string; description?: string | null }) => {
    if (mode === 'edit' && selected) await mutations.update.mutateAsync({ id: selected.id, body })
    else await mutations.create.mutateAsync(body)
    setNotice({ tone: 'success', message: mode === 'edit' ? 'Subject updated.' : 'Subject created.' })
    setMode(null)
  }
  const changeStatus = async () => {
    if (!confirm) return
    const next: SubjectStatus = confirm.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await mutations.status.mutateAsync({ id: confirm.id, status: next })
      setNotice({ tone: 'success', message: `${confirm.name} is now ${next.toLowerCase()}.` })
    } catch (error) {
      setNotice({ tone: 'error', message: friendlyApiError(error) })
    } finally {
      setConfirm(null)
    }
  }
  return <div>
    <PageHeader
      eyebrow="Academic workspace"
      title="Subjects"
      description="Organize the institution question bank into active teaching subjects."
      actions={canManage ? <Button icon={<Plus size={17} />} onClick={() => { setSelected(null); setMode('create') }}>New subject</Button> : undefined}
    />
    <Notice notice={notice} />
    {!canManage ? <Card className="role-note"><strong>Read-only access</strong><p>Examiners can browse subjects and use them while authoring questions.</p></Card> : null}
    <Card className="filter-bar">
      <form onSubmit={submitSearch}>
        <Input id="subject-search" label="Search subjects" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} />
        <Button type="submit" variant="secondary" icon={<Search size={16} />}>Search</Button>
      </form>
      <Select id="subject-status" label="Status" value={status} onChange={(event) => { setStatus(event.target.value as SubjectStatus | ''); setPage(0) }}>
        <option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option>
      </Select>
    </Card>
    {query.isPending ? <LoadingSkeleton /> : query.isError ? <EmptyState title="Subjects could not be loaded" description={friendlyApiError(query.error)} action={<Button onClick={() => void query.refetch()}>Try again</Button>} /> : query.data.content.length === 0 ? <EmptyState title="No subjects found" description="Adjust the filters or create the first subject." /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Subject</th><th>Code</th><th>Status</th><th>Updated</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
        {query.data.content.map((subject) => <tr key={subject.id}>
          <td data-label="Subject"><strong>{subject.name}</strong><small>{subject.description || 'No description'}</small></td>
          <td data-label="Code">{subject.code}</td>
          <td data-label="Status"><Badge tone={subject.status === 'ACTIVE' ? 'success' : 'neutral'}>{subject.status}</Badge></td>
          <td data-label="Updated">{new Date(subject.updatedAt).toLocaleDateString()}</td>
          <td className="row-actions"><Button size="sm" variant="ghost" icon={<Eye size={15} />} onClick={() => { setSelected(subject); setMode('view') }}>View</Button>{canManage ? <><Button size="sm" variant="ghost" icon={<Pencil size={15} />} onClick={() => { setSelected(subject); setMode('edit') }}>Edit</Button><Button size="sm" variant={subject.status === 'ACTIVE' ? 'danger' : 'secondary'} onClick={() => setConfirm(subject)}>{subject.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</Button></> : null}</td>
        </tr>)}
      </tbody></table></Card>
      <Pagination page={query.data.page} totalPages={query.data.totalPages} totalElements={query.data.totalElements} onPage={setPage} />
    </>}
    <Modal open={mode === 'create' || mode === 'edit'} title={mode === 'edit' ? 'Edit subject' : 'Create subject'} onClose={() => setMode(null)}>
      <SubjectForm subject={mode === 'edit' ? selected ?? undefined : undefined} onSubmit={submit} onCancel={() => setMode(null)} />
    </Modal>
    <Modal open={mode === 'view'} title="Subject details" onClose={() => setMode(null)}>{selected ? <dl className="detail-list"><div><dt>Name</dt><dd>{selected.name}</dd></div><div><dt>Code</dt><dd>{selected.code}</dd></div><div><dt>Status</dt><dd>{selected.status}</dd></div><div><dt>Description</dt><dd>{selected.description || 'No description provided'}</dd></div></dl> : null}</Modal>
    <ConfirmDialog open={Boolean(confirm)} title={confirm?.status === 'ACTIVE' ? 'Deactivate subject?' : 'Activate subject?'} description={confirm?.status === 'ACTIVE' ? 'The subject remains available historically but cannot be used for new active academic work.' : 'The subject will become available for question and exam authoring.'} confirmLabel={confirm?.status === 'ACTIVE' ? 'Deactivate' : 'Activate'} loading={mutations.status.isPending} onClose={() => setConfirm(null)} onConfirm={() => void changeStatus()} />
  </div>
}
