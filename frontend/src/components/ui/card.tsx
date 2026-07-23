import type { HTMLAttributes } from 'react'

export const Card = ({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) => (
  <div className={`card ${className}`.trim()} {...props} />
)
