import { Pencil, Plus, Search } from 'lucide-react'
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
import type { Role } from '../../types/auth'
import type { ManagedUser, UserStatus } from '../../types/management'
import { useInstitutions } from '../institutions/institution-hooks'
import { useAuth } from '../auth/use-auth'
import { useUserMutations, useUsers } from './user-hooks'
import { UserForm } from './user-form'
import type { CreateUserBody, UpdateUserBody } from './users-api'

type Mode = 'platform-admins' | 'institution'
const staffRoles: Exclude<Role, 'SUPER_ADMIN'>[] = ['EXAMINER', 'INVIGILATOR', 'STUDENT']

export const UsersPage = ({ mode }: { mode: Mode }) => {
  const { user: actor } = useAuth()
  const platform = mode === 'platform-admins'
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [institutionId, setInstitutionId] = useState('')
  const [role, setRole] = useState<Role | ''>(platform ? 'INSTITUTION_ADMIN' : '')
  const [status, setStatus] = useState<UserStatus | ''>('')
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState<'create' | 'edit' | null>(null)
  const [selected, setSelected] = useState<ManagedUser | null>(null)
  const [confirm, setConfirm] = useState<ManagedUser | null>(null)
  const [notice, setNotice] = useState<NoticeValue>(null)
  const institutionsQuery = useInstitutions({ page: 0, size: 100 }, platform)
  const effectiveInstitutionId = platform ? institutionId || undefined : undefined
  const users = useUsers({ search, institutionId: effectiveInstitutionId, role: platform ? 'INSTITUTION_ADMIN' : role, status, page, size: 20 })
  const mutations = useUserMutations()
  const institutions = institutionsQuery.data?.content.filter((item) => item.status === 'ACTIVE') ?? []
  const createDisabled = platform && institutions.length === 0
  const submitSearch = (event: React.FormEvent) => { event.preventDefault(); setPage(0); setSearch(searchDraft.trim()) }
  const submitUser = async (body: CreateUserBody | UpdateUserBody) => {
    if (selected && modal === 'edit') await mutations.update.mutateAsync({ id: selected.id, body: body as UpdateUserBody })
    else await mutations.create.mutateAsync(body as CreateUserBody)
    setNotice({ tone: 'success', message: selected ? 'User profile updated.' : 'User account created.' }); setModal(null); setSelected(null)
  }
  const changeStatus = async () => { if (!confirm) return; const next: UserStatus = confirm.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'; try { await mutations.status.mutateAsync({ id: confirm.id, status: next }); setNotice({ tone: 'success', message: `${confirm.firstName} ${confirm.lastName} is now ${next.toLowerCase()}.` }) } catch (error) { setNotice({ tone: 'error', message: friendlyApiError(error) }) } finally { setConfirm(null) } }
  return <div>
    <PageHeader eyebrow={platform ? 'Platform administration' : 'Institution administration'} title={platform ? 'Institution administrators' : 'User accounts'} description={platform ? 'Create and manage administrator accounts for active institutions.' : 'Manage staff and students within your authenticated institution.'} actions={<Button icon={<Plus size={17} />} disabled={createDisabled} onClick={() => { setSelected(null); setModal('create') }}>New account</Button>} />
    <Notice notice={notice} />
    <Card className="filter-bar"><form onSubmit={submitSearch}><Input id="user-search" label="Search users" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /><Button type="submit" variant="secondary" icon={<Search size={16} />}>Search</Button></form>{platform ? <Select id="user-institution" label="Institution" value={institutionId} onChange={(event) => { setInstitutionId(event.target.value); setPage(0) }}><option value="">All active institutions</option>{institutions.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</Select> : <Select id="user-role" label="Role" value={role} onChange={(event) => { setRole(event.target.value as Role | ''); setPage(0) }}><option value="">All roles</option>{staffRoles.map((item) => <option key={item} value={item}>{item.replaceAll('_', ' ')}</option>)}</Select>}<Select id="user-status" label="Status" value={status} onChange={(event) => { setStatus(event.target.value as UserStatus | ''); setPage(0) }}><option value="">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option><option value="LOCKED">Locked</option></Select></Card>
    {users.isPending ? <LoadingSkeleton /> : users.isError ? <EmptyState title="Users could not be loaded" description={friendlyApiError(users.error)} action={<Button onClick={() => void users.refetch()}>Try again</Button>} /> : users.data.content.length === 0 ? <EmptyState title="No matching accounts" description="Adjust the filters or create a new account." /> : <><Card className="responsive-data"><table><thead><tr><th>User</th><th>Role</th><th>Registration</th><th>Status</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>{users.data.content.map((item) => <tr key={item.id}><td data-label="User"><strong>{item.firstName} {item.lastName}</strong><small>{item.email}</small></td><td data-label="Role">{item.roles.map((itemRole) => itemRole.replaceAll('_', ' ')).join(', ')}</td><td data-label="Registration">{item.registrationNumber ?? '—'}</td><td data-label="Status"><Badge tone={item.status === 'ACTIVE' ? 'success' : 'warning'}>{item.status}</Badge></td><td className="row-actions"><Button size="sm" variant="ghost" icon={<Pencil size={15} />} onClick={() => { setSelected(item); setModal('edit') }}>Edit</Button><Button size="sm" variant={item.status === 'ACTIVE' ? 'danger' : 'secondary'} onClick={() => setConfirm(item)}>{item.status === 'ACTIVE' ? 'Suspend' : 'Activate'}</Button></td></tr>)}</tbody></table></Card><Pagination page={users.data.page} totalPages={users.data.totalPages} totalElements={users.data.totalElements} onPage={setPage} /></>}
    <Modal open={Boolean(modal)} title={modal === 'edit' ? 'Edit user profile' : platform ? 'Create institution administrator' : 'Create user account'} onClose={() => setModal(null)}><UserForm user={modal === 'edit' ? selected ?? undefined : undefined} institutions={platform ? institutions : undefined} fixedInstitutionId={platform ? undefined : actor?.institutionId ?? undefined} allowedRoles={platform ? ['INSTITUTION_ADMIN'] : staffRoles} onSubmit={submitUser} onCancel={() => setModal(null)} /></Modal>
    <ConfirmDialog open={Boolean(confirm)} title={confirm?.status === 'ACTIVE' ? 'Suspend account?' : 'Activate account?'} description={confirm?.status === 'ACTIVE' ? 'The user will be unable to authenticate while inactive.' : 'The user will regain access according to their assigned role.'} confirmLabel={confirm?.status === 'ACTIVE' ? 'Suspend' : 'Activate'} loading={mutations.status.isPending} onConfirm={() => void changeStatus()} onClose={() => setConfirm(null)} />
  </div>
}
