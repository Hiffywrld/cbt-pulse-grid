import { AlertCircle, CheckCircle2, Info, TriangleAlert } from 'lucide-react'
import type { ReactNode } from 'react'

const icons = {
  error: AlertCircle,
  success: CheckCircle2,
  info: Info,
  warning: TriangleAlert,
}

export const Alert = ({
  children,
  tone = 'info',
  title,
}: {
  children: ReactNode
  tone?: keyof typeof icons
  title?: string
}) => {
  const Icon = icons[tone]
  return (
    <div className={`alert alert--${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      <Icon size={20} aria-hidden="true" />
      <div>{title ? <strong>{title}</strong> : null}<div>{children}</div></div>
    </div>
  )
}
