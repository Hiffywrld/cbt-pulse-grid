import { forwardRef, type TextareaHTMLAttributes } from 'react'

type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label: string
  error?: string
  hint?: string
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, hint, id, name, ...props }, ref) => {
    const inputId = id ?? name
    return <div className="field">
      <label className="field__label" htmlFor={inputId}>{label}</label>
      <textarea
        ref={ref}
        id={inputId}
        name={name}
        className={`textarea ${error ? 'input--error' : ''}`}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined}
        {...props}
      />
      {error ? <span className="field__error" id={`${inputId}-error`}>{error}</span> : null}
      {!error && hint ? <span className="field__hint" id={`${inputId}-hint`}>{hint}</span> : null}
    </div>
  },
)
Textarea.displayName = 'Textarea'
