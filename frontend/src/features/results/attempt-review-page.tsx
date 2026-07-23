import { ArrowLeft, CheckCircle2, Circle } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Card } from '../../components/ui/card'
import { friendlyApiError } from '../../lib/api/form-errors'
import { useStaffAttemptResult } from './result-hooks'

export const AttemptReviewPage = () => {
  const { attemptId = '' } = useParams()
  const result = useStaffAttemptResult(attemptId)
  if (result.isPending) return <LoadingSkeleton rows={6} />
  if (result.isError) return <EmptyState title="Attempt review could not be loaded" description={friendlyApiError(result.error)} />
  const data = result.data
  return <div>
    <Link className="back-link" to={`/institution/results/exams/${data.examId}`}><ArrowLeft size={16} />Back to examination results</Link>
    <PageHeader eyebrow={data.examCode} title={`${data.firstName} ${data.lastName}`} description={`${data.examTitle} · ${data.registrationNumber || data.email}`} actions={<Badge tone={data.passed ? 'success' : 'neutral'}>{data.status.replaceAll('_', ' ')}</Badge>} />
    <Card className="attempt-review-summary">
      <div><span>Score</span><strong>{data.score ?? '—'} / {data.maximumScore ?? '—'}</strong></div>
      <div><span>Percentage</span><strong>{data.percentage === null ? '—' : `${data.percentage}%`}</strong></div>
      <div><span>Outcome</span><strong>{data.passed === null ? 'Pending' : data.passed ? 'Passed' : 'Failed'}</strong></div>
    </Card>
    {!data.reviewAvailable ? <EmptyState title="Answer review is not available" description="Correctness is never exposed while an attempt remains in progress." /> : <div className="attempt-review-list">
      {data.questions.map((question) => <Card key={question.attemptQuestionId} className="review-question">
        <header><span>Question {question.position + 1}</span><strong>{question.awardedMarks} / {question.marks} marks</strong></header>
        <h2>{question.questionText}</h2>
        <ul>{question.options.map((option) => <li key={option.optionId} className={option.correct ? 'review-option review-option--correct' : 'review-option'}>
          {option.selected ? <CheckCircle2 size={17} /> : <Circle size={17} />}
          <span>{option.optionText}</span>
          {option.correct ? <Badge tone="success">Correct</Badge> : option.selected ? <Badge tone="warning">Selected</Badge> : null}
        </li>)}</ul>
      </Card>)}
    </div>}
  </div>
}

