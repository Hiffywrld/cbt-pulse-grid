import { Check, ChevronLeft, ChevronRight, Clock3, Expand, Send, Wifi, WifiOff } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { Alert } from '../../components/feedback/alert'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { ConfirmDialog } from '../../components/ui/confirm-dialog'
import { friendlyApiError } from '../../lib/api/form-errors'
import { attemptStorage } from '../../lib/storage/attempt-storage'
import type { CandidateQuestion } from '../../types/attempt'
import { attemptApi } from './attempt-api'
import { formatCountdown, useAuthoritativeTimer } from './use-authoritative-timer'
import { useAttemptMonitoring } from './use-attempt-monitoring'

const attemptKey = (attemptId: string) => ['student-attempt', attemptId] as const

const QuestionOptions = ({
  question,
  selected,
  disabled,
  onChange,
}: {
  question: CandidateQuestion
  selected: string[]
  disabled: boolean
  onChange(ids: string[]): void
}) => {
  const multiple = question.questionType === 'MULTIPLE_CHOICE'
  return <fieldset className="runner-options" disabled={disabled}>
    <legend className="sr-only">Answer options</legend>
    {question.options.map((option) => {
      const checked = selected.includes(option.id)
      return <label key={option.id} className={checked ? 'runner-option runner-option--selected' : 'runner-option'}>
        <input
          type={multiple ? 'checkbox' : 'radio'}
          name={`question-${question.id}`}
          checked={checked}
          onChange={() => onChange(multiple
            ? checked ? selected.filter((id) => id !== option.id) : [...selected, option.id]
            : [option.id])}
        />
        <span className="runner-option__marker">{String.fromCharCode(64 + option.displayOrder)}</span>
        <span>{option.optionText}</span>
      </label>
    })}
  </fieldset>
}

export const StudentAttemptPage = () => {
  const { attemptId = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const attempt = useQuery({ queryKey: attemptKey(attemptId), queryFn: () => attemptApi.get(attemptId), enabled: Boolean(attemptId) })
  const [currentIndex, setCurrentIndex] = useState(0)
  const [answers, setAnswers] = useState<Record<string, string[]>>({})
  const [saveState, setSaveState] = useState<'saved' | 'pending' | 'saving' | 'offline'>('saved')
  const [pendingCount, setPendingCount] = useState(0)
  const [submitOpen, setSubmitOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [finalizing, setFinalizing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [online, setOnline] = useState(() => navigator.onLine)
  const sequences = useRef<Record<string, number>>({})
  const flushTimer = useRef<number | null>(null)
  const expiryHandled = useRef(false)
  const automaticSubmissionPending = useRef(false)
  const data = attempt.data
  const active = data?.status === 'IN_PROGRESS' && !finalizing
  useAttemptMonitoring(attemptId, active)

  const refreshPending = useCallback(async () => {
    const queued = await attemptStorage.answers(attemptId)
    setPendingCount(queued.length)
    return queued
  }, [attemptId])

  const flushPending = useCallback(async () => {
    if (!navigator.onLine || !active) {
      setSaveState('offline')
      return false
    }
    setSaveState('saving')
    try {
      for (let batchNumber = 0; batchNumber < 20; batchNumber += 1) {
        const queued = await refreshPending()
        if (!queued.length) {
          setSaveState('saved')
          return true
        }
        const batch = await attemptStorage.answerBatch(attemptId)
        if (!batch) {
          setSaveState('saved')
          return true
        }
        const response = await attemptApi.syncAnswers(attemptId, batch.syncId, batch.answers.map((answer) => ({
          attemptQuestionId: answer.attemptQuestionId,
          selectedOptionIds: answer.selectedOptionIds,
          clientSequence: answer.clientSequence,
        })))
        await attemptStorage.acknowledgeAnswers(
          attemptId,
          new Map(response.savedAnswers.map((answer) => [answer.attemptQuestionId, answer.clientSequence])),
        )
        await attemptStorage.acknowledgeAnswerBatch(batch.syncId)
      }
      setSaveState('pending')
      return false
    } catch (failure) {
      setSaveState(navigator.onLine ? 'pending' : 'offline')
      setError(friendlyApiError(failure))
      return false
    }
  }, [active, attemptId, refreshPending])

  useEffect(() => {
    if (!data) return
    void attemptStorage.rememberAttempt(data.attemptId, data.examId)
    void attemptStorage.answers(data.attemptId).then((queued) => {
      const restored: Record<string, string[]> = {}
      data.answers.forEach((answer) => {
        restored[answer.attemptQuestionId] = answer.selectedOptionIds
        sequences.current[answer.attemptQuestionId] = answer.clientSequence
      })
      queued.forEach((answer) => {
        if ((sequences.current[answer.attemptQuestionId] ?? -1) < answer.clientSequence) {
          restored[answer.attemptQuestionId] = answer.selectedOptionIds
          sequences.current[answer.attemptQuestionId] = answer.clientSequence
        }
      })
      setAnswers(restored)
      setPendingCount(queued.length)
      if (queued.length) setSaveState(navigator.onLine ? 'pending' : 'offline')
    })
    if (data.status !== 'IN_PROGRESS') navigate(`/student/attempts/${data.attemptId}/result`, { replace: true })
  }, [data, navigate])

  useEffect(() => {
    const markOnline = () => { setOnline(true); void flushPending() }
    const markOffline = () => { setOnline(false); setSaveState('offline') }
    window.addEventListener('online', markOnline)
    window.addEventListener('offline', markOffline)
    return () => {
      window.removeEventListener('online', markOnline)
      window.removeEventListener('offline', markOffline)
    }
  }, [flushPending])

  useEffect(() => {
    const warn = (event: BeforeUnloadEvent) => {
      if (pendingCount > 0 && active) {
        event.preventDefault()
        event.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [active, pendingCount])

  useEffect(() => () => {
    if (flushTimer.current !== null) window.clearTimeout(flushTimer.current)
  }, [])

  const submit = useCallback(async (automatic = false) => {
    if (submitting) return
    setSubmitting(true)
    setFinalizing(true)
    setError(null)
    try {
      if (navigator.onLine) await flushPending()
      if (!navigator.onLine) {
        automaticSubmissionPending.current = automatic
        setFinalizing(automatic)
        setError('Time has ended. Reconnect to retrieve the backend automatic-submission result.')
        return
      }
      const result = await attemptApi.submit(attemptId)
      queryClient.setQueryData(['student-attempt-result', attemptId], result)
      await attemptStorage.clearAttempt(attemptId)
      navigate(`/student/attempts/${attemptId}/result`, { replace: true, state: { automatic } })
    } catch (failure) {
      setError(friendlyApiError(failure))
      if (navigator.onLine) {
        try {
          const result = await attemptApi.result(attemptId)
          if (result.status !== 'IN_PROGRESS') {
            queryClient.setQueryData(['student-attempt-result', attemptId], result)
            await attemptStorage.clearAttempt(attemptId)
            navigate(`/student/attempts/${attemptId}/result`, { replace: true })
            return
          }
        } catch {
          // Keep the safe original error visible.
        }
      }
      if (!automatic) setFinalizing(false)
    } finally {
      setSubmitting(false)
      setSubmitOpen(false)
    }
  }, [attemptId, flushPending, navigate, queryClient, submitting])

  useEffect(() => {
    if (!online || !automaticSubmissionPending.current || submitting) return
    automaticSubmissionPending.current = false
    void submit(true)
  }, [online, submit, submitting])

  const expire = useCallback(() => {
    if (data?.status !== 'IN_PROGRESS' || expiryHandled.current) return
    expiryHandled.current = true
    void submit(true)
  }, [data?.status, submit])

  const remaining = useAuthoritativeTimer(
    data?.serverTime ?? '1970-01-01T00:00:00.000Z',
    data?.expiresAt ?? '9999-12-31T23:59:59.000Z',
    expire,
  )

  const choose = async (questionId: string, selectedOptionIds: string[]) => {
    if (!active || remaining === 0) return
    const clientSequence = (sequences.current[questionId] ?? -1) + 1
    sequences.current[questionId] = clientSequence
    setAnswers((current) => ({ ...current, [questionId]: selectedOptionIds }))
    setSaveState(navigator.onLine ? 'pending' : 'offline')
    await attemptStorage.queueAnswer({
      attemptId,
      attemptQuestionId: questionId,
      selectedOptionIds,
      clientSequence,
      updatedAt: new Date().toISOString(),
    })
    await refreshPending()
    if (flushTimer.current !== null) window.clearTimeout(flushTimer.current)
    flushTimer.current = window.setTimeout(() => void flushPending(), 600)
  }

  if (attempt.isPending || !data) return <div className="runner-loading"><LoadingSkeleton rows={6} /></div>
  if (attempt.isError) return <div className="runner-loading"><Alert tone="error">{friendlyApiError(attempt.error)}</Alert></div>
  const question = data.questions[currentIndex]
  const answered = data.questions.filter((item) => (answers[item.id]?.length ?? 0) > 0).length
  return <main className="exam-runner">
    <header className="runner-header">
      <div><span className="exam-code">{data.examCode}</span><h1>{data.title}</h1></div>
      <div className={remaining <= 300 ? 'runner-timer runner-timer--urgent' : 'runner-timer'} role="timer" aria-live="polite">
        <Clock3 size={18} /><span><small>Time remaining</small><strong>{formatCountdown(remaining)}</strong></span>
      </div>
      <div className="runner-status">
        <span>{online ? <Wifi size={16} /> : <WifiOff size={16} />}{online ? 'Online' : 'Offline'}</span>
        <span><Check size={16} />{saveState === 'saved' ? 'Saved' : saveState === 'saving' ? 'Saving…' : saveState === 'offline' ? 'Offline' : 'Pending'}</span>
        <Button size="sm" variant="secondary" icon={<Expand size={15} />} onClick={() => void document.documentElement.requestFullscreen?.()}>Fullscreen</Button>
      </div>
    </header>
    {error ? <Alert tone="error">{error}</Alert> : null}
    <div className="runner-layout">
      <aside className="question-palette" aria-label="Question navigation">
        <div><strong>Question palette</strong><small>{answered} of {data.questions.length} answered</small></div>
        <nav>{data.questions.map((item, index) => <button
          key={item.id}
          type="button"
          aria-label={`Question ${index + 1}${answers[item.id]?.length ? ', answered' : ', unanswered'}`}
          aria-current={index === currentIndex ? 'step' : undefined}
          className={`${answers[item.id]?.length ? 'palette-item palette-item--answered' : 'palette-item'}${index === currentIndex ? ' palette-item--current' : ''}`}
          onClick={() => setCurrentIndex(index)}
        >{index + 1}</button>)}</nav>
        <div className="palette-key"><span><i />Answered</span><span><i />Current</span></div>
      </aside>
      <Card className="runner-question">
        <div className="runner-question__heading"><span>Question {currentIndex + 1} of {data.questions.length}</span><small>{question.questionType.replaceAll('_', ' ')}</small></div>
        <h2>{question.questionText}</h2>
        <QuestionOptions question={question} selected={answers[question.id] ?? []} disabled={!active || remaining === 0} onChange={(ids) => void choose(question.id, ids)} />
        <footer className="runner-actions">
          <Button variant="secondary" icon={<ChevronLeft size={17} />} disabled={currentIndex === 0} onClick={() => setCurrentIndex((index) => index - 1)}>Previous</Button>
          {currentIndex < data.questions.length - 1
            ? <Button icon={<ChevronRight size={17} />} onClick={() => setCurrentIndex((index) => index + 1)}>Next question</Button>
            : <Button icon={<Send size={17} />} onClick={() => setSubmitOpen(true)}>Review and submit</Button>}
        </footer>
      </Card>
    </div>
    <ConfirmDialog
      open={submitOpen}
      title="Submit examination?"
      description={`${answered} answered and ${data.questions.length - answered} unanswered. Pending answers will be synchronized before final submission when online. Submission cannot be undone.`}
      confirmLabel="Submit examination"
      loading={submitting}
      onClose={() => setSubmitOpen(false)}
      onConfirm={() => void submit(false)}
    />
  </main>
}
