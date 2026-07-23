import { format } from 'date-fns'
import { ArrowLeft, KeyRound } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Card } from '../../components/ui/card'
import { friendlyApiError } from '../../lib/api/form-errors'
import { useStudentExam } from './student-exam-hooks'

export const StudentExamDetailPage = () => {
  const { examId = '' } = useParams()
  const exam = useStudentExam(examId)
  if (exam.isPending) return <LoadingSkeleton rows={4} />
  if (exam.isError) return <EmptyState title="Exam details unavailable" description={friendlyApiError(exam.error)} />
  return <div><Link className="back-link" to="/student/exams"><ArrowLeft size={16} />Back to examinations</Link><PageHeader eyebrow={exam.data.code} title={exam.data.title} description="Review the official instructions and timing before continuing." actions={<Badge tone={exam.data.availability === 'ACTIVE' ? 'success' : 'info'}>{exam.data.availability}</Badge>} />
    <div className="detail-grid"><Card className="detail-panel"><h2>Exam information</h2><dl className="detail-list"><div><dt>Starts</dt><dd>{format(new Date(exam.data.startsAt), 'PPpp')}</dd></div><div><dt>Ends</dt><dd>{format(new Date(exam.data.endsAt), 'PPpp')}</dd></div><div><dt>Duration</dt><dd>{exam.data.durationMinutes} minutes</dd></div></dl></Card><Card className="detail-panel"><h2>Instructions</h2><p className="instructions-text">{exam.data.instructions?.trim() || 'No additional instructions were supplied for this examination.'}</p></Card></div>
    <Card className="start-callout"><div><KeyRound /><div><h2>Ready to continue?</h2><p>The next step confirms the six-digit access PIN. The backend remains authoritative for assignment, schedule, PIN, and device checks.</p></div></div>{exam.data.availability === 'ACTIVE' ? <Link className="button button--primary button--md" to={`/student/exams/${exam.data.id}/start`}>Start exam</Link> : <span>Start is available only during the active exam window.</span>}</Card>
  </div>
}
