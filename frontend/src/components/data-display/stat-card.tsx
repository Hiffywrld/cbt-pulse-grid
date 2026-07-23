import type { LucideIcon } from 'lucide-react'
import { Card } from '../ui/card'

export const StatCard = ({
  label,
  value,
  detail,
  icon: Icon,
}: {
  label: string
  value: string
  detail: string
  icon: LucideIcon
}) => (
  <Card className="stat-card">
    <span className="stat-card__icon"><Icon size={20} aria-hidden="true" /></span>
    <div><p>{label}</p><strong>{value}</strong><small>{detail}</small></div>
  </Card>
)
