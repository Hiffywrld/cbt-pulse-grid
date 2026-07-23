import { Eye, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
import type { ExamInput, ExamStatus } from '../../types/academic'
import { useAuth } from '../auth/use-auth'
import { useSubjects } from '../subjects/subject-hooks'
import { ExamForm } from './exam-form'
import { useExamMutations, useExams } from './exam-hooks'

const label = (value: string) => value.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase())
const statusTone = (status: ExamStatus) => status === 'PUBLISHED' ? 'success' : status === 'DRAFT' ? 'info' : status === 'CANCELLED' ? 'warning' : 'neutral'

export const ExamsPage = () => {
  const navigate = useNavigate()
  const { user } = useAuth()
  const canManage = Boolean(user?.roles.some((role) => role === 'INSTITUTION_ADMIN' || role === 'EXAMINER'))
  const invigilatorOnly = Boolean(user?.roles.includes('INVIGILATOR') && !canManage)
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [subjectId, setSubjectId] = useState('')
  const [status, setStatus] = useState<ExamStatus | ''>(invigilatorOnly ? 'PUBLISHED' : '')
  const [page, setPage] = useState(0)
  const [createOpen, setCreateOpen] = useState(false)
  const [notice, setNotice] = useState<NoticeValue>(null)
  const exams = useExams({ search, subjectId, status, page, size: 20 })
  const subjectsQuery = useSubjects({ status: 'ACTIVE', page: 0, size: 100 }, canManage)
  const subjects = subjectsQuery.data?.content ?? []
  const mutations = useExamMutations()
  const create = async (body: ExamInput & { accessPin?: string }) => {
    const created = await mutations.create.mutateAsync(body as ExamInput & { accessPin: string })
    setCreateOpen(false)
    setNotice({ tone: 'success', message: 'Draft exam created. Assign candidates and verify pool availability before publishing.' })
    navigate(`/institution/exams/${created.id}`)
  }
  return <div>
    <PageHeader eyebrow="Examination operations" title="Examinations" description={invigilatorOnly ? 'Review published exam schedules and candidate assignments.' : 'Configure secure exam windows, question pools, candidates and lifecycle.'} actions={canManage ? <Button icon={<Plus size={17} />} disabled={subjects.length === 0} onClick={() => setCreateOpen(true)}>New exam</Button> : undefined} />
    <Notice notice={notice} />
    {invigilatorOnly ? <Card className="role-note"><strong>Invigilator view</strong><p>Only published exam details and candidate lists are available. Authoring and lifecycle controls remain hidden.</p></Card> : null}
    <Card className="filter-bar">
      <form onSubmit={(event) => { event.preventDefault(); setPage(0); setSearch(searchDraft.trim()) }}><Input id="exam-search" label="Search title or code" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /><Button type="submit" variant="secondary" icon={<Search size={16} />}>Search</Button></form>
      {canManage ? <Select id="exam-subject" label="Subject" value={subjectId} onChange={(event) => { setSubjectId(event.target.value); setPage(0) }}><option value="">All subjects</option>{subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code}</option>)}</Select> : null}
      {!invigilatorOnly ? <Select id="exam-status" label="Status" value={status} onChange={(event) => { setStatus(event.target.value as ExamStatus | ''); setPage(0) }}><option value="">All statuses</option><option value="DRAFT">Draft</option><option value="PUBLISHED">Published</option><option value="CANCELLED">Cancelled</option><option value="CLOSED">Closed</option></Select> : null}
    </Card>
    {exams.isPending ? <LoadingSkeleton /> : exams.isError ? <EmptyState title="Exams could not be loaded" description={friendlyApiError(exams.error)} action={<Button onClick={() => void exams.refetch()}>Try again</Button>} /> : exams.data.content.length === 0 ? <EmptyState title="No exams found" description={canManage ? 'Adjust the filters or create a draft exam.' : 'No published exams are available for invigilation.'} /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Exam</th><th>Schedule</th><th>Duration</th><th>Pass mark</th><th>Status</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
        {exams.data.content.map((exam) => <tr key={exam.id}>
          <td data-label="Exam"><strong>{exam.title}</strong><small>{exam.code}</small></td>
          <td data-label="Schedule"><span>{new Date(exam.startsAt).toLocaleString()}</span><small>to {new Date(exam.endsAt).toLocaleString()}</small></td>
          <td data-label="Duration">{exam.durationMinutes} minutes</td><td data-label="Pass mark">{exam.passMarkPercentage}%</td>
          <td data-label="Status"><Badge tone={statusTone(exam.status)}>{label(exam.status)}</Badge></td>
          <td className="row-actions"><Button size="sm" variant="ghost" icon={<Eye size={15} />} onClick={() => navigate(`/institution/exams/${exam.id}`)}>Open</Button></td>
        </tr>)}
      </tbody></table></Card>
      <Pagination page={exams.data.page} totalPages={exams.data.totalPages} totalElements={exams.data.totalElements} onPage={setPage} />
    </>}
    <Modal open={createOpen} title="Create examination" size="wide" onClose={() => setCreateOpen(false)}><ExamForm subjects={subjects} onSubmit={create} onCancel={() => setCreateOpen(false)} /></Modal>
  </div>
}
