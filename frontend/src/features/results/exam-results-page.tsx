import { ArrowLeft, Download, Eye, Search, Sigma, UsersRound } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Pagination } from '../../components/data-display/pagination'
import { StatCard } from '../../components/data-display/stat-card'
import { EmptyState } from '../../components/feedback/empty-state'
import { LoadingSkeleton } from '../../components/feedback/loading-skeleton'
import { Notice, type NoticeValue } from '../../components/feedback/notice'
import { PageHeader } from '../../components/layout/page-header'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { Input } from '../../components/ui/input'
import { Select } from '../../components/ui/select'
import { friendlyApiError } from '../../lib/api/form-errors'
import type { CandidateResultStatus } from '../../types/results'
import { useCandidateResults, useExamResultSummary } from './result-hooks'
import { resultsApi } from './results-api'

const number = (value: number | null, suffix = '') => value === null ? '—' : `${Number(value).toFixed(1)}${suffix}`
const label = (value: string) => value.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase())

export const ExamResultsPage = () => {
  const { examId = '' } = useParams()
  const navigate = useNavigate()
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState<CandidateResultStatus | ''>('')
  const [passed, setPassed] = useState<boolean | ''>('')
  const [page, setPage] = useState(0)
  const [downloading, setDownloading] = useState(false)
  const [notice, setNotice] = useState<NoticeValue>(null)
  const summary = useExamResultSummary(examId)
  const params = { search, status, passed, page, size: 20 }
  const candidates = useCandidateResults(examId, params)
  const download = async () => {
    setDownloading(true)
    try {
      const blob = await resultsApi.exportCsv(examId, { search, status, passed })
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `exam-${summary.data?.examCode.toLowerCase() ?? examId}-results.csv`
      anchor.click()
      URL.revokeObjectURL(url)
      setNotice({ tone: 'success', message: 'UTF-8 result export downloaded.' })
    } catch (failure) {
      setNotice({ tone: 'error', message: friendlyApiError(failure) })
    } finally {
      setDownloading(false)
    }
  }
  if (summary.isPending) return <LoadingSkeleton rows={5} />
  if (summary.isError) return <EmptyState title="Result summary could not be loaded" description={friendlyApiError(summary.error)} />
  const stats = summary.data
  return <div>
    <Link className="back-link" to="/institution/results"><ArrowLeft size={16} />Back to results</Link>
    <PageHeader eyebrow={stats.examCode} title={stats.examTitle} description="Tenant-secured examination outcome summary and candidate records." actions={<Button icon={<Download size={16} />} loading={downloading} onClick={() => void download()}>Export CSV</Button>} />
    <Notice notice={notice} />
    <div className="result-stat-grid">
      <StatCard label="Assigned" value={String(stats.assignedCandidates)} detail={`${stats.notStarted} not started`} icon={UsersRound} />
      <StatCard label="Absent" value={String(stats.absent)} detail="Window ended without an attempt" icon={UsersRound} />
      <StatCard label="In progress" value={String(stats.inProgress)} detail="Active attempts" icon={Sigma} />
      <StatCard label="Completed" value={String(stats.submitted + stats.autoSubmitted)} detail={`${stats.autoSubmitted} auto-submitted`} icon={Sigma} />
      <StatCard label="Passed / failed" value={`${stats.passed} / ${stats.failed}`} detail={`${number(stats.passRate, '%')} pass rate`} icon={Sigma} />
      <StatCard label="Average" value={number(stats.averagePercentage, '%')} detail={`Min ${number(stats.minimumPercentage, '%')} · Max ${number(stats.maximumPercentage, '%')}`} icon={Sigma} />
      <StatCard label="Obtainable marks" value={number(stats.totalObtainableMarks)} detail="Maximum examination score" icon={Sigma} />
    </div>
    <Card className="filter-bar filter-bar--results">
      <form onSubmit={(event) => { event.preventDefault(); setSearch(searchDraft.trim()); setPage(0) }}><Input id="candidate-result-search" label="Search candidate" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} /><Button type="submit" variant="secondary" icon={<Search size={16} />}>Search</Button></form>
      <Select id="candidate-result-status" label="Attempt status" value={status} onChange={(event) => { setStatus(event.target.value as CandidateResultStatus | ''); setPage(0) }}><option value="">All statuses</option><option value="NOT_STARTED">Not started</option><option value="ABSENT">Absent</option><option value="IN_PROGRESS">In progress</option><option value="SUBMITTED">Submitted</option><option value="AUTO_SUBMITTED">Auto-submitted</option></Select>
      <Select id="candidate-result-passed" label="Outcome" value={passed === '' ? '' : String(passed)} onChange={(event) => { setPassed(event.target.value === '' ? '' : event.target.value === 'true'); setPage(0) }}><option value="">All outcomes</option><option value="true">Passed</option><option value="false">Failed</option></Select>
    </Card>
    {candidates.isPending ? <LoadingSkeleton /> : candidates.isError ? <EmptyState title="Candidate results could not be loaded" description={friendlyApiError(candidates.error)} /> : candidates.data.content.length === 0 ? <EmptyState title="No candidate results found" description="Adjust the search or result filters." /> : <>
      <Card className="responsive-data"><table><thead><tr><th>Candidate</th><th>Status</th><th>Score</th><th>Percentage</th><th>Outcome</th><th><span className="sr-only">Actions</span></th></tr></thead><tbody>
        {candidates.data.content.map((candidate) => <tr key={candidate.candidateId}>
          <td data-label="Candidate"><strong>{candidate.firstName} {candidate.lastName}</strong><small>{candidate.registrationNumber || candidate.email}</small></td>
          <td data-label="Status"><Badge tone={candidate.attemptStatus === 'IN_PROGRESS' ? 'info' : candidate.attemptStatus === 'ABSENT' ? 'warning' : candidate.attemptStatus === 'NOT_STARTED' ? 'neutral' : 'success'}>{label(candidate.attemptStatus)}</Badge></td>
          <td data-label="Score">{candidate.score === null ? '—' : `${candidate.score} / ${candidate.maximumScore}`}</td>
          <td data-label="Percentage">{candidate.percentage === null ? '—' : `${candidate.percentage}%`}</td>
          <td data-label="Outcome">{candidate.attemptStatus === 'ABSENT' ? 'Absent' : candidate.passed === null ? 'Pending' : candidate.passed ? 'Passed' : 'Failed'}</td>
          <td className="row-actions">{candidate.attemptId ? <Button size="sm" variant="ghost" icon={<Eye size={15} />} onClick={() => navigate(`/institution/results/attempts/${candidate.attemptId}`)}>Review</Button> : <span>{candidate.attemptStatus === 'ABSENT' ? 'No attempt created' : 'Not started'}</span>}</td>
        </tr>)}
      </tbody></table></Card>
      <Pagination page={candidates.data.page} totalPages={candidates.data.totalPages} totalElements={candidates.data.totalElements} onPage={setPage} />
    </>}
  </div>
}
