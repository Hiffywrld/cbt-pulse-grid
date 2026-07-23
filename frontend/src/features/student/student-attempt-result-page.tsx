import { Award, CheckCircle2, Clock3, XCircle } from 'lucide-react'
import { useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { Alert } from '../../components/feedback/alert'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { Card } from '../../components/ui/card'
import { friendlyApiError } from '../../lib/api/form-errors'
import { attemptStorage } from '../../lib/storage/attempt-storage'
import { attemptApi } from './attempt-api'

export const StudentAttemptResultPage = () => {
  const { attemptId = '' } = useParams()
  const result = useQuery({
    queryKey: ['student-attempt-result', attemptId],
    queryFn: () => attemptApi.result(attemptId),
    enabled: Boolean(attemptId),
  })
  useEffect(() => {
    if (result.data?.status === 'SUBMITTED' || result.data?.status === 'AUTO_SUBMITTED') {
      void attemptStorage.clearAttempt(attemptId)
    }
  }, [attemptId, result.data?.status])
  if (result.isPending) return <LoadingSkeleton rows={4} />
  if (result.isError) return <Alert tone="error">{friendlyApiError(result.error)}</Alert>
  const data = result.data
  const finalized = data.status === 'SUBMITTED' || data.status === 'AUTO_SUBMITTED'
  return <div className="student-result-page">
    <Card className="student-result-card">
      {finalized ? data.passed ? <CheckCircle2 className="result-pass" size={48} /> : <XCircle className="result-fail" size={48} /> : <Clock3 size={48} />}
      <span className="eyebrow">{data.status.replaceAll('_', ' ')}</span>
      <h1>{finalized ? data.passed ? 'Examination passed' : 'Examination completed' : 'Result pending'}</h1>
      {finalized ? <div className="result-score">
        <strong>{data.score ?? 0}<small> / {data.maximumScore ?? 0}</small></strong>
        <span>{data.percentage ?? 0}%</span>
      </div> : <p>The backend has not finalized this attempt yet.</p>}
      {data.status === 'AUTO_SUBMITTED' ? <Alert tone="info">The backend automatically submitted this attempt when its authoritative timer expired.</Alert> : null}
      <div className="result-actions"><Award size={18} /><span>This result is calculated and persisted by the backend.</span></div>
      <Link className="button button--primary button--md" to="/student/exams">Return to my examinations</Link>
    </Card>
  </div>
}
