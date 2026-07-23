import { Inbox } from 'lucide-react'
import type { ReactNode } from 'react'

export const EmptyState = ({
  title,
  description,
  action,
}: {
  title: string
  description: string
  action?: ReactNode
}) => (
  <div className="empty-state">
    <span className="empty-state__icon"><Inbox aria-hidden="true" /></span>
    <h2>{title}</h2>
    <p>{description}</p>
    {action}
  </div>
)
