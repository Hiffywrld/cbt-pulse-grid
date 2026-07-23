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
import type {
  QuestionDifficulty, QuestionInput, QuestionStatus, QuestionSummary, QuestionType, StaffQuestion,
} from '../../types/academic'
import { useSubjects } from '../subjects/subject-hooks'
import { QuestionForm } from './question-form'
import { useQuestion, useQuestionMutations, useQuestions } from './question-hooks'

const label = (value: string) => value.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (character) => character.toUpperCase())
const statusTone = (status: QuestionStatus) => status === 'PUBLISHED' ? 'success' : status === 'DRAFT' ? 'info' : 'neutral'

export const QuestionsPage = () => {
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [subjectId, setSubjectId] = useState('')
  const [type, setType] = useState<QuestionType | ''>('')
  const [difficulty, setDifficulty] = useState<QuestionDifficulty | ''>('')
  const [status, setStatus] = useState<QuestionStatus | ''>('')
  const [page, setPage] = useState(0)
  const [mode, setMode] = useState<'create' | 'edit' | 'view' | null>(null)
  const [selected, setSelected] = useState<QuestionSummary | null>(null)
  const [transition, setTransition] = useState<{ question: QuestionSummary; status: QuestionStatus } | null>(null)
  const [notice, setNotice] = useState<NoticeValue>(null)
  const query = useQuestions({ search, subjectId, type, difficulty, status, page, size: 20 })
  const subjectsQuery = useSubjects({ status: 'ACTIVE', page: 0, size: 100 })
  const detail = useQuestion(mode === 'edit' || mode === 'view' ? selected?.id : undefined)
  const mutations = useQuestionMutations()
  const subjects = subjectsQuery.data?.content ?? []
  const submit = async (body: QuestionInput) => {
    if (mode === 'edit' && selected) await mutations.update.mutateAsync({ id: selected.id, body })
    else await mutations.create.mutateAsync(body)
    setNotice({ tone: 'success', message: mode === 'edit' ? 'Question updated.' : 'Draft question created.' })
    setMode(null)
  }
  const changeStatus = async () => {
    if (!transition) return
    try {
      await mutations.status.mutateAsync({ id: transition.question.id, status: transition.status })
      setNotice({ tone: 'success', message: `Question moved to ${label(transition.status)}.` })
    } catch (error) {
      setNotice({ tone: 'error', message: friendlyApiError(error) })
    } finally {
      setTransition(null)
    }
  }
  const open = (next: 'edit' | 'view', question: QuestionSummary) => { setSelected(question); setMode(next) }
  const filterSelect = <T extends string>(setter: (value: T | '') => void) =>
    (event: React.ChangeEvent<HTMLSelectElement>) => { setter(event.target.value as T | ''); setPage(0) }
  return <div>
    <PageHeader eyebrow="Academic workspace" title="Question bank" description="Author validated staff-only questions. Correct answers never flow into candidate delivery contracts." actions={<Button icon={<Plus size={17} />} disabled={subjects.length === 0} onClick={() => { setSelected(null); setMode('create') }}>New question</Button>} />
    <Notice notice={notice} />
    {subjectsQuery.isSuccess && subjects.length === 0 ? <Card className="role-note"><strong>An active subject is required</strong><p>Ask an institution administrator to activate a subject before authoring questions.</p></Card> : null}
    <Card className="filter-bar filter-bar--academic">
      <form onSubmit={(event) => { event.preventDefault(); setPage(0); setSearch(searchDraft.trim()) }}><Input id="question-search" label="Search question text" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /><Button type="submit" variant="secondary" icon={<Search size={16} />}>Search</Button></form>
      <Select id="question-subject" label="Subject" value={subjectId} onChange={filterSelect(setSubjectId)}><option value="">All subjects</option>{subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code}</option>)}</Select>
      <Select id="question-type" label="Type" value={type} onChange={filterSelect<QuestionType>(setType)}><option value="">All types</option><option value="SINGLE_CHOICE">Single choice</option><option value="MULTIPLE_CHOICE">Multiple choice</option><option value="TRUE_FALSE">True / False</option></Select>
      <Select id="question-difficulty" label="Difficulty" value={difficulty} onChange={filterSelect<QuestionDifficulty>(setDifficulty)}><option value="">All difficulties</option><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></Select>
      <Select id="question-status" label="Status" value={status} onChange={filterSelect<QuestionStatus>(setStatus)}><option value="">All statuses</option><option value="DRAFT">Draft</option><option value="PUBLISHED">Published</option><option value="ARCHIVED">Archived</option></Select>
    </Card>
    {query.isPending ? <LoadingSkeleton /> : query.isError ? <EmptyState title="Questions could not be loaded" description={friendlyApiError(query.error)} action={<Button onClick={() => void query.refetch()}>Try again</Button>} /> : query.data.content.length === 0 ? <EmptyState title="No questions found" description="Adjust the filters or create a draft question." /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Question</th><th>Type</th><th>Difficulty</th><th>Marks</th><th>Status</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
        {query.data.content.map((question) => <tr key={question.id}>
          <td data-label="Question"><strong className="question-preview">{question.questionText}</strong></td>
          <td data-label="Type">{label(question.type)}</td><td data-label="Difficulty">{label(question.difficulty)}</td><td data-label="Marks">{question.marks}</td>
          <td data-label="Status"><Badge tone={statusTone(question.status)}>{label(question.status)}</Badge></td>
          <td className="row-actions"><Button size="sm" variant="ghost" icon={<Eye size={15} />} onClick={() => open('view', question)}>View</Button><Button size="sm" variant="ghost" icon={<Pencil size={15} />} onClick={() => open('edit', question)}>Edit</Button>{question.status === 'DRAFT' ? <Button size="sm" onClick={() => setTransition({ question, status: 'PUBLISHED' })}>Publish</Button> : null}{question.status === 'PUBLISHED' ? <Button size="sm" variant="secondary" onClick={() => setTransition({ question, status: 'ARCHIVED' })}>Archive</Button> : null}</td>
        </tr>)}
      </tbody></table></Card>
      <Pagination page={query.data.page} totalPages={query.data.totalPages} totalElements={query.data.totalElements} onPage={setPage} />
    </>}
    <Modal open={mode === 'create' || mode === 'edit'} title={mode === 'edit' ? 'Edit question' : 'Create question'} size="wide" onClose={() => setMode(null)}>
      {mode === 'edit' && detail.isPending ? <LoadingSkeleton rows={4} /> : mode === 'edit' && detail.isError ? <EmptyState title="Question could not be loaded" description={friendlyApiError(detail.error)} /> : <QuestionForm question={mode === 'edit' ? detail.data : undefined} subjects={subjects} onSubmit={submit} onCancel={() => setMode(null)} />}
    </Modal>
    <Modal open={mode === 'view'} title="Question details" onClose={() => setMode(null)}>
      {detail.isPending ? <LoadingSkeleton rows={4} /> : detail.data ? <QuestionDetails question={detail.data} /> : detail.isError ? <EmptyState title="Question unavailable" description={friendlyApiError(detail.error)} /> : null}
    </Modal>
    <ConfirmDialog open={Boolean(transition)} title={`${transition ? label(transition.status) : ''} question?`} description={transition?.status === 'PUBLISHED' ? 'Publishing validates the subject and answer structure before the question becomes available to exam pools.' : 'Archived questions remain available historically but leave the published pool.'} confirmLabel={transition ? label(transition.status) : 'Confirm'} loading={mutations.status.isPending} onClose={() => setTransition(null)} onConfirm={() => void changeStatus()} />
  </div>
}

const QuestionDetails = ({ question }: { question: StaffQuestion }) => <div className="question-detail">
  <dl className="detail-list"><div><dt>Question</dt><dd>{question.questionText}</dd></div><div><dt>Type</dt><dd>{label(question.type)}</dd></div><div><dt>Difficulty</dt><dd>{label(question.difficulty)}</dd></div><div><dt>Marks</dt><dd>{question.marks}</dd></div></dl>
  <h3>Staff answer key</h3>
  <ol className="answer-key">{[...question.options].sort((a, b) => a.displayOrder - b.displayOrder).map((option) => <li key={option.id} className={option.correct ? 'answer-key__correct' : ''}>{option.optionText}{option.correct ? <Badge tone="success">Correct</Badge> : null}</li>)}</ol>
</div>
