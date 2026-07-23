import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Spinner } from '../feedback/spinner'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  icon?: ReactNode
}

export const Button = ({
  children,
  className = '',
  variant = 'primary',
  size = 'md',
  loading = false,
  icon,
  disabled,
  ...props
}: ButtonProps) => (
  <button
    className={`button button--${variant} button--${size} ${className}`.trim()}
    disabled={disabled || loading}
    {...props}
  >
    {loading ? <Spinner size="sm" label="Working" /> : icon}
    <span>{children}</span>
  </button>
)
