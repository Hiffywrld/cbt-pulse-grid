import { ArrowLeft, KeyRound, Pencil, Plus, Trash2 } from 'lucide-react'
import { useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
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
import { friendlyApiError } from '../../lib/api/form-errors'
import type { ExamDetail, ExamInput } from '../../types/academic'
import { useAuth } from '../auth/use-auth'
import { useSubjects } from '../subjects/subject-hooks'
import { useUsers } from '../users/user-hooks'
import { studentExamKeys } from '../student/student-exam-hooks'
import { ExamForm } from './exam-form'
import { examKeys, useExam, useExamCandidates, useExamMutations } from './exam-hooks'
import { examsApi } from './exams-api'

type Lifecycle = 'publish' | 'cancel' | 'close'
const label = (value: string) => value.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase())

export const ExamDetailPage = () => {
  const { examId = '' } = useParams()
  const { user } = useAuth()
  const canManage = Boolean(user?.roles.some((role) => role === 'INSTITUTION_ADMIN' || role === 'EXAMINER'))
  const canSearchStudents = Boolean(user?.roles.includes('INSTITUTION_ADMIN'))
  const exam = useExam(examId)
  const [candidatePage, setCandidatePage] = useState(0)
  const candidates = useExamCandidates(examId, candidatePage)
  const subjects = useSubjects({ status: 'ACTIVE', page: 0, size: 100 }, canManage)
  const mutations = useExamMutations()
  const queryClient = useQueryClient()
  const [modal, setModal] = useState<'edit' | 'pin' | 'assign' | null>(null)
  const [lifecycle, setLifecycle] = useState<Lifecycle | null>(null)
  const [removeCandidate, setRemoveCandidate] = useState<{ userId: string; name: string } | null>(null)
  const [notice, setNotice] = useState<NoticeValue>(null)
  const current = exam.data
  const mutate = async (work: () => Promise<unknown>, message: string, close = true) => {
    try {
      await work(); setNotice({ tone: 'success', message }); if (close) setModal(null)
    } catch (error) {
      setNotice({ tone: 'error', message: friendlyApiError(error) })
    }
  }
  const lifecycleAction = async () => {
    if (!lifecycle) return
    const action = mutations[lifecycle]
    await mutate(() => action.mutateAsync(examId), `Exam ${lifecycle === 'publish' ? 'published' : lifecycle === 'cancel' ? 'cancelled' : 'closed'}.`, false)
    setLifecycle(null)
  }
  const rotatePin = async (accessPin: string) => {
    await examsApi.rotatePin(examId, accessPin)
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: examKeys.all }),
      queryClient.invalidateQueries({ queryKey: studentExamKeys.all }),
    ])
    setNotice({ tone: 'success', message: 'Access PIN rotated. Securely provide the new PIN to assigned candidates.' })
  }
  if (exam.isPending) return <LoadingSkeleton rows={6} />
  if (exam.isError || !current) return <EmptyState title="Exam could not be loaded" description={friendlyApiError(exam.error)} action={<Link className="button button--secondary button--md" to="/institution/exams">Back to exams</Link>} />
  const draft = current.status === 'DRAFT'
  return <div>
    <PageHeader eyebrow="Examination operations" title={current.title} description={`${current.code} · ${label(current.status)}`} actions={<Link className="button button--secondary button--md" to="/institution/exams"><ArrowLeft size={16} /> <span>All exams</span></Link>} />
    <Notice notice={notice} />
    <Card className="exam-command-bar">
      <div><Badge tone={current.status === 'PUBLISHED' ? 'success' : current.status === 'DRAFT' ? 'info' : 'neutral'}>{label(current.status)}</Badge><span>{new Date(current.startsAt).toLocaleString()} — {new Date(current.endsAt).toLocaleString()}</span></div>
      {canManage ? <div>{draft ? <><Button size="sm" variant="secondary" icon={<Pencil size={15} />} onClick={() => setModal('edit')}>Edit</Button><Button size="sm" variant="secondary" icon={<KeyRound size={15} />} onClick={() => setModal('pin')}>Rotate PIN</Button><Button size="sm" onClick={() => setLifecycle('publish')}>Publish</Button></> : null}{current.status === 'PUBLISHED' ? <><Button size="sm" variant="secondary" onClick={() => setLifecycle('close')}>Close exam</Button><Button size="sm" variant="danger" onClick={() => setLifecycle('cancel')}>Cancel</Button></> : null}{current.status === 'DRAFT' ? <Button size="sm" variant="danger" onClick={() => setLifecycle('cancel')}>Cancel</Button> : null}</div> : null}
    </Card>
    <div className="detail-grid">
      <Card className="detail-panel"><h2>Exam definition</h2><dl className="detail-list"><div><dt>Duration</dt><dd>{current.durationMinutes} minutes</dd></div><div><dt>Pass mark</dt><dd>{current.passMarkPercentage}%</dd></div><div><dt>Access PIN</dt><dd>{current.accessPinConfigured ? 'Configured securely' : 'Not configured'}</dd></div><div><dt>Question order</dt><dd>{current.shuffleQuestions ? 'Shuffled' : 'Fixed'}</dd></div><div><dt>Option order</dt><dd>{current.shuffleOptions ? 'Shuffled' : 'Fixed'}</dd></div><div><dt>Instructions</dt><dd>{current.instructions || 'No candidate instructions provided'}</dd></div></dl></Card>
      <Card className="detail-panel"><h2>Question pools</h2><div className="pool-summary">{current.poolRules.map((rule) => <div key={rule.id}><Badge tone="neutral">{label(rule.difficulty)}</Badge><strong>{rule.questionCount} question{rule.questionCount === 1 ? '' : 's'}</strong><span>{rule.marksPerQuestion} mark{rule.marksPerQuestion === 1 ? '' : 's'} each</span></div>)}</div></Card>
    </div>
    <section className="section-block">
      <div className="section-heading"><div><h2>Assigned candidates</h2><p>Only active students from this institution can be assigned.</p></div>{canManage && draft && canSearchStudents ? <Button size="sm" icon={<Plus size={16} />} onClick={() => setModal('assign')}>Assign students</Button> : null}</div>
      {canManage && draft && !canSearchStudents ? <Card className="role-note"><strong>Candidate search requires institution administrator access</strong><p>Examiners can manage exam definitions and lifecycle, but the current user-search contract does not authorize examiner access. An institution administrator must assign candidates.</p></Card> : null}
      {candidates.isPending ? <LoadingSkeleton rows={3} /> : candidates.isError ? <EmptyState title="Candidates could not be loaded" description={friendlyApiError(candidates.error)} /> : candidates.data.content.length === 0 ? <EmptyState title="No candidates assigned" description={draft ? 'Assign active students before publishing this exam.' : 'No candidate assignments are available.'} /> : <>
        <Card className="responsive-data"><table><thead><tr><th>Candidate</th><th>Registration</th><th>Status</th><th>Assigned</th>{canManage && draft ? <th><span className="sr-only">Actions</span></th> : null}</tr></thead><tbody>{candidates.data.content.map((candidate) => <tr key={candidate.assignmentId}><td data-label="Candidate"><strong>{candidate.firstName} {candidate.lastName}</strong><small>{candidate.email}</small></td><td data-label="Registration">{candidate.registrationNumber || 'Not provided'}</td><td data-label="Status"><Badge tone={candidate.status === 'ACTIVE' ? 'success' : 'warning'}>{label(candidate.status)}</Badge></td><td data-label="Assigned">{new Date(candidate.assignedAt).toLocaleDateString()}</td>{canManage && draft ? <td className="row-actions"><Button size="sm" variant="ghost" icon={<Trash2 size={15} />} onClick={() => setRemoveCandidate({ userId: candidate.userId, name: `${candidate.firstName} ${candidate.lastName}` })}>Remove</Button></td> : null}</tr>)}</tbody></table></Card>
        <Pagination page={candidates.data.page} totalPages={candidates.data.totalPages} totalElements={candidates.data.totalElements} onPage={setCandidatePage} />
      </>}
    </section>
    <Modal open={modal === 'edit'} title="Edit examination" size="wide" onClose={() => setModal(null)}><ExamForm exam={current} subjects={subjects.data?.content ?? []} onSubmit={async (body) => mutate(() => mutations.update.mutateAsync({ id: examId, body: body as Omit<ExamInput, 'accessPin'> }), 'Exam updated.')} onCancel={() => setModal(null)} /></Modal>
    <Modal open={modal === 'pin'} title="Rotate access PIN" onClose={() => setModal(null)}><PinForm onCancel={() => setModal(null)} onSuccess={() => setModal(null)} onSubmit={rotatePin} /></Modal>
    <Modal open={modal === 'assign'} title="Assign students" onClose={() => setModal(null)}><CandidateAssignment exam={current} assignedIds={new Set(candidates.data?.content.map((candidate) => candidate.userId))} onCancel={() => setModal(null)} onAssign={(userIds) => mutate(() => mutations.assign.mutateAsync({ id: examId, userIds }), `${userIds.length} candidate${userIds.length === 1 ? '' : 's'} assigned.`)} /></Modal>
    <ConfirmDialog open={Boolean(lifecycle)} title={`${lifecycle ? label(lifecycle) : ''} exam?`} description={lifecycle === 'publish' ? 'Publishing validates candidate assignments, subject status, schedule, PIN and every question pool.' : lifecycle === 'close' ? 'Closing ends this published exam lifecycle.' : 'Cancellation preserves all historical records and cannot be undone through this portal.'} confirmLabel={lifecycle ? label(lifecycle) : 'Confirm'} loading={lifecycle ? mutations[lifecycle].isPending : false} onClose={() => setLifecycle(null)} onConfirm={() => void lifecycleAction()} />
    <ConfirmDialog open={Boolean(removeCandidate)} title="Remove candidate?" description={`${removeCandidate?.name ?? 'This candidate'} will no longer be assigned to this draft exam.`} confirmLabel="Remove" loading={mutations.remove.isPending} onClose={() => setRemoveCandidate(null)} onConfirm={() => { if (!removeCandidate) return; void mutate(() => mutations.remove.mutateAsync({ id: examId, userId: removeCandidate.userId }), 'Candidate removed.', false).finally(() => setRemoveCandidate(null)) }} />
  </div>
}

export const PinForm = ({ onSubmit, onCancel, onSuccess = onCancel }: {
  onSubmit(pin: string): Promise<void>
  onCancel(): void
  onSuccess?(): void
}) => {
  const [pin, setPin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!/^\d{6}$/.test(pin)) {
      setError('Enter exactly six digits')
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await onSubmit(pin)
      setPin('')
      onSuccess()
    } catch (failure) {
      setError(friendlyApiError(failure))
    } finally {
      setSubmitting(false)
    }
  }
  return <form className="management-form" onSubmit={(event) => void submit(event)}>
    <Input label="New six-digit access PIN" type="password" inputMode="numeric" maxLength={6} autoComplete="new-password" value={pin} onChange={(event) => { setPin(event.target.value); setError(null) }} error={error ?? undefined} hint="Submitted once and cleared after success. The backend stores only a hash, so the original PIN cannot be retrieved or displayed later. Securely provide the new PIN to candidates." />
    <div className="modal-actions"><Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button><Button type="submit" loading={submitting}>Rotate PIN</Button></div>
  </form>
}

const CandidateAssignment = ({ exam, assignedIds, onAssign, onCancel }: {
  exam: ExamDetail
  assignedIds: Set<string>
  onAssign(userIds: string[]): Promise<void>
  onCancel(): void
}) => {
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const users = useUsers({ search, role: 'STUDENT', status: 'ACTIVE', page, size: 20 })
  const eligible = useMemo(() => users.data?.content.filter((student) => student.institutionId === exam.institutionId && !assignedIds.has(student.id)) ?? [], [assignedIds, exam.institutionId, users.data])
  const toggle = (id: string) => setSelected((current) => {
    const next = new Set(current); if (next.has(id)) next.delete(id); else next.add(id); return next
  })
  return <div className="candidate-picker">
    <form className="inline-search" onSubmit={(event) => { event.preventDefault(); setPage(0); setSearch(searchDraft.trim()) }}><Input id="candidate-search" label="Search active students" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /><Button type="submit" variant="secondary">Search</Button></form>
    {users.isPending ? <LoadingSkeleton rows={3} /> : users.isError ? <EmptyState title="Students could not be loaded" description={friendlyApiError(users.error)} /> : eligible.length === 0 ? <EmptyState title="No eligible students found" description="Try another search or confirm that active student accounts exist." /> : <div className="candidate-options">{eligible.map((student) => <label key={student.id}><input type="checkbox" checked={selected.has(student.id)} onChange={() => toggle(student.id)} /><span><strong>{student.firstName} {student.lastName}</strong><small>{student.registrationNumber || student.email}</small></span></label>)}</div>}
    {users.data ? <Pagination page={users.data.page} totalPages={users.data.totalPages} totalElements={users.data.totalElements} onPage={setPage} /> : null}
    <div className="modal-actions"><Button variant="secondary" onClick={onCancel}>Cancel</Button><Button disabled={selected.size === 0} onClick={() => void onAssign([...selected])}>Assign {selected.size || ''} student{selected.size === 1 ? '' : 's'}</Button></div>
  </div>
}
