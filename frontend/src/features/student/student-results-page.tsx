import { PageHeader } from '../../components/layout/page-header'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { Card } from '../../components/ui/card'
import { Badge } from '../../components/ui/badge'
import { friendlyApiError } from '../../lib/api/form-errors'
import { useStudentExams } from './student-exam-hooks'

export const StudentResultsPage = () => {
  const exams = useStudentExams()
  if (exams.isPending) return <LoadingSkeleton />
  if (exams.isError) return <EmptyState title="Results unavailable" description={friendlyApiError(exams.error)} />
  const records = exams.data.filter((exam) => exam.participationStatus === 'ABSENT' || exam.participationStatus === 'SUBMITTED' || exam.participationStatus === 'AUTO_SUBMITTED')
  return <div><PageHeader eyebrow="Assessment history" title="My results" description="Submitted outcomes and absences derived by the backend from your assignments and examination windows." />
    {records.length === 0 ? <EmptyState title="No completed results" description="Submitted or missed examinations will appear here." /> : <div className="exam-grid">{records.map((exam) => <Card className="exam-card" key={exam.id}><div className="exam-card__top"><span className="exam-code">{exam.code}</span><Badge tone={exam.participationStatus === 'ABSENT' ? 'warning' : 'success'}>{exam.participationStatus?.replaceAll('_', ' ')}</Badge></div><h2>{exam.title}</h2><p>{exam.participationStatus === 'ABSENT' ? 'Absent · 0 points · no attempt record created' : `${exam.score ?? 0} / ${exam.maximumScore ?? 0} · ${exam.percentage ?? 0}% · ${exam.passed ? 'Passed' : 'Failed'}`}</p></Card>)}</div>}
  </div>
}
