import { format } from 'date-fns'
import { ArrowRight, CalendarClock, Clock3 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Badge } from '../../components/ui/badge'
import { Card } from '../../components/ui/card'
import type { StudentExamSummary } from '../../types/management'

const availabilityTone = (availability: StudentExamSummary['availability']) => availability === 'ACTIVE' ? 'success' : availability === 'UPCOMING' ? 'info' : 'neutral'

export const ExamCard = ({ exam }: { exam: StudentExamSummary }) => <Card className="exam-card">
  <div className="exam-card__top"><span className="exam-code">{exam.code}</span><Badge tone={exam.participationStatus === 'ABSENT' ? 'warning' : availabilityTone(exam.availability)}>{exam.participationStatus === 'ABSENT' ? 'ABSENT' : exam.availability}</Badge></div>
  <h2>{exam.title}</h2>
  <div className="exam-card__facts"><span><CalendarClock size={16} />{format(new Date(exam.startsAt), 'PPp')} – {format(new Date(exam.endsAt), 'PPp')}</span><span><Clock3 size={16} />{exam.durationMinutes} minutes</span></div>
  {exam.participationStatus === 'ABSENT' && <p className="absence-note">Window closed without an attempt · Score 0 / {exam.maximumScore ?? 0}</p>}
  <Link className="text-link" to={`/student/exams/${exam.id}`}>{exam.availability === 'ACTIVE' ? 'View and start exam' : 'View details'} <ArrowRight size={16} /></Link>
</Card>
