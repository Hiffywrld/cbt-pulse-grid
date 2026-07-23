import { BarChart3, Eye, Search } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Pagination } from '../../components/data-display/pagination'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { Input } from '../../components/ui/input'
import { Select } from '../../components/ui/select'
import { friendlyApiError } from '../../lib/api/form-errors'
import type { ExamStatus } from '../../types/academic'
import { useExams } from '../exams/exam-hooks'

export const ResultsPage = () => {
  const navigate = useNavigate()
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<ExamStatus | ''>('')
  const [page, setPage] = useState(0)
  const exams = useExams({ search, status, page, size: 20 })
  return <div>
    <PageHeader eyebrow="Assessment outcomes" title="Results workspace" description="Open a real examination summary, review candidate outcomes and export tenant-scoped CSV data." />
    <Card className="filter-bar">
      <form onSubmit={(event) => { event.preventDefault(); setSearch(searchDraft.trim()); setPage(0) }}>
        <Input id="result-exam-search" label="Search examination" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} />
        <Button type="submit" variant="secondary" icon={<Search size={16} />}>Search</Button>
      </form>
      <Select id="result-exam-status" label="Lifecycle status" value={status} onChange={(event) => { setStatus(event.target.value as ExamStatus | ''); setPage(0) }}>
        <option value="">All statuses</option><option value="PUBLISHED">Published</option><option value="CLOSED">Closed</option><option value="CANCELLED">Cancelled</option><option value="DRAFT">Draft</option>
      </Select>
    </Card>
    {exams.isPending ? <LoadingSkeleton /> : exams.isError ? <EmptyState title="Results could not be loaded" description={friendlyApiError(exams.error)} /> : exams.data.content.length === 0 ? <EmptyState title="No examinations found" description="Adjust the lifecycle or search filters." /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Examination</th><th>Window</th><th>Lifecycle</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
        {exams.data.content.map((exam) => <tr key={exam.id}>
          <td data-label="Examination"><strong>{exam.title}</strong><small>{exam.code}</small></td>
          <td data-label="Window">{new Date(exam.startsAt).toLocaleString()}<small>to {new Date(exam.endsAt).toLocaleString()}</small></td>
          <td data-label="Lifecycle"><Badge tone={exam.status === 'PUBLISHED' ? 'success' : 'neutral'}>{exam.status}</Badge></td>
          <td className="row-actions"><Button size="sm" variant="ghost" icon={<Eye size={15} />} onClick={() => navigate(`/institution/results/exams/${exam.id}`)}>View results</Button></td>
        </tr>)}
      </tbody></table></Card>
      <Pagination page={exams.data.page} totalPages={exams.data.totalPages} totalElements={exams.data.totalElements} onPage={setPage} />
    </>}
    <Card className="role-note"><BarChart3 size={20} /><p>Counts and statistics are calculated only after you open an examination, using the dedicated tenant-secured result endpoints.</p></Card>
  </div>
}

