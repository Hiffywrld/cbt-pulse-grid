import type { ReactNode } from 'react'

export const Badge = ({
  children,
  tone = 'neutral',
}: {
  children: ReactNode
  tone?: 'neutral' | 'info' | 'success' | 'warning'
}) => <span className={`badge badge--${tone}`}>{children}</span>
