import { ArrowLeft, ShieldCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Alert } from '../../components/feedback/alert'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { PageHeader } from '../../components/layout/page-header'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { PasswordInput } from '../../components/ui/password-input'
import { friendlyApiError } from '../../lib/api/form-errors'
import { attemptStorage } from '../../lib/storage/attempt-storage'
import { getDeviceId } from '../../lib/storage/device-identity'
import { attemptApi } from './attempt-api'
import { useStudentExam } from './student-exam-hooks'

export const StudentExamStartPage = () => {
  const { examId = '' } = useParams()
  const navigate = useNavigate()
  const exam = useStudentExam(examId)
  const [pin, setPin] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [starting, setStarting] = useState(false)
  const [restoring, setRestoring] = useState(true)

  useEffect(() => {
    let cancelled = false
    void attemptStorage.attemptForExam(examId).then(async (attemptId) => {
      if (!attemptId) return
      try {
        const existing = await attemptApi.get(attemptId)
        if (cancelled) return
        navigate(existing.status === 'IN_PROGRESS'
          ? `/student/attempts/${attemptId}`
          : `/student/attempts/${attemptId}/result`, { replace: true })
      } catch {
        // The PIN form remains available when stale local restoration data cannot be used.
      }
    }).finally(() => { if (!cancelled) setRestoring(false) })
    return () => { cancelled = true }
  }, [examId, navigate])

  const start = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!/^\d{6}$/.test(pin)) {
      setError('Enter exactly six digits')
      return
    }
    setError(null)
    setStarting(true)
    try {
      const attempt = await attemptApi.start(examId, pin, getDeviceId())
      setPin('')
      await attemptStorage.rememberAttempt(attempt.attemptId, attempt.examId)
      navigate(attempt.status === 'IN_PROGRESS'
        ? `/student/attempts/${attempt.attemptId}`
        : `/student/attempts/${attempt.attemptId}/result`, { replace: true })
    } catch (failure) {
      setError(friendlyApiError(failure))
    } finally {
      setStarting(false)
    }
  }

  if (exam.isPending || restoring) return <LoadingSkeleton rows={3} />
  if (exam.isError) return <Alert tone="error">This assigned examination could not be loaded.</Alert>
  const active = exam.data.availability === 'ACTIVE'
  return <div>
    <Link className="back-link" to={`/student/exams/${examId}`}><ArrowLeft size={16} />Back to instructions</Link>
    <PageHeader eyebrow="Secure entry" title="Enter exam access PIN" description={`${exam.data.title} · ${exam.data.durationMinutes} minutes`} />
    <Card className="pin-panel">
      <ShieldCheck size={34} aria-hidden="true" />
      <h2>Authoritative exam start</h2>
      <p>A successful PIN check starts the backend-controlled timer and locks this attempt to this browser device. The PIN is submitted once and is never saved locally.</p>
      {!active ? <Alert tone="warning">This exam is not currently available to start.</Alert> : null}
      <form className="management-form" onSubmit={(event) => void start(event)} noValidate>
        <PasswordInput
          id="exam-access-pin"
          label="Six-digit access PIN"
          inputMode="numeric"
          pattern="[0-9]{6}"
          maxLength={6}
          autoComplete="off"
          value={pin}
          disabled={!active || starting}
          error={error ?? undefined}
          onChange={(event) => { setPin(event.target.value.replace(/\D/g, '').slice(0, 6)); setError(null) }}
          hint="The PIN is validated by the backend and is not persisted in this browser."
        />
        <Button type="submit" loading={starting} disabled={!active || starting}>Validate PIN and begin</Button>
      </form>
    </Card>
  </div>
}
