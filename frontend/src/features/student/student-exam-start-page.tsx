import { ArrowLeft, ShieldCheck } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Alert } from '../../components/feedback/alert'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { Input } from '../../components/ui/input'
import { useStudentExam } from './student-exam-hooks'

export const StudentExamStartPage = () => {
  const { examId = '' } = useParams()
  const exam = useStudentExam(examId)
  if (exam.isPending) return <LoadingSkeleton rows={3} />
  if (exam.isError) return <Alert tone="error">This assigned examination could not be loaded.</Alert>
  return <div><Link className="back-link" to={`/student/exams/${examId}`}><ArrowLeft size={16} />Back to instructions</Link><PageHeader eyebrow="Secure entry" title="Enter exam access PIN" description={`${exam.data.title} · ${exam.data.durationMinutes} minutes`} />
    <Card className="pin-panel"><ShieldCheck size={34} /><h2>Runner hand-off is intentionally locked</h2><p>The access PIN will be sent only when the candidate runner is available, because the backend starts the server-authoritative timer when it validates this step.</p><Input label="Six-digit access PIN" inputMode="numeric" pattern="[0-9]{6}" maxLength={6} autoComplete="off" disabled hint="PIN submission is enabled with the full candidate runner in the next phase." /><Button disabled>Validate PIN and begin</Button></Card>
  </div>
}
