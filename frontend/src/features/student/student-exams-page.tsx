import { Button } from '../../components/ui/button'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { friendlyApiError } from '../../lib/api/form-errors'
import { ExamCard } from './exam-card'
import { useStudentExams } from './student-exam-hooks'

export const StudentExamsPage = () => {
  const exams = useStudentExams()
  return <div><PageHeader eyebrow="Candidate workspace" title="My examinations" description="Only examinations assigned to your authenticated student account are shown." />
    {exams.isPending ? <LoadingSkeleton rows={3} /> : exams.isError ? <EmptyState title="Examinations could not be loaded" description={friendlyApiError(exams.error)} action={<Button onClick={() => void exams.refetch()}>Try again</Button>} /> : exams.data.length === 0 ? <EmptyState title="No assigned examinations" description="Your institution has not assigned any published examinations to this account." /> : <div className="exam-grid">{exams.data.map((exam) => <ExamCard key={exam.id} exam={exam} />)}</div>}
  </div>
}
