import { forwardRef, type InputHTMLAttributes } from 'react'

export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  error?: string
  hint?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, hint, id, className = '', ...props }, ref) => {
    const inputId = id ?? props.name
    const describedBy = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined
    return (
      <div className={`field ${className}`.trim()}>
        <label className="field__label" htmlFor={inputId}>{label}</label>
        <input
          ref={ref}
          id={inputId}
          className={`input ${error ? 'input--error' : ''}`}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          {...props}
        />
        {error ? <span className="field__error" id={`${inputId}-error`}>{error}</span> : null}
        {!error && hint ? <span className="field__hint" id={`${inputId}-hint`}>{hint}</span> : null}
      </div>
    )
  },
)
Input.displayName = 'Input'
