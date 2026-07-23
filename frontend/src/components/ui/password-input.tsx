import { forwardRef, useState, type InputHTMLAttributes } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import type { InputProps } from './input'

export const PasswordInput = forwardRef<
  HTMLInputElement,
  Omit<InputProps, 'type'> & InputHTMLAttributes<HTMLInputElement>
>(({ label, error, hint, id, name, className = '', ...props }, ref) => {
  const [visible, setVisible] = useState(false)
  const inputId = id ?? name
  const describedBy = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined
  return (
    <div className={`field ${className}`.trim()}>
      <label className="field__label" htmlFor={inputId}>{label}</label>
      <div className={`password-field ${error ? 'input--error' : ''}`}>
        <input
          ref={ref}
          id={inputId}
          name={name}
          type={visible ? 'text' : 'password'}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          {...props}
        />
        <button
          type="button"
          className="password-field__toggle"
          aria-label={visible ? 'Hide password' : 'Show password'}
          onClick={() => setVisible((current) => !current)}
        >
          {visible ? <EyeOff size={18} /> : <Eye size={18} />}
        </button>
      </div>
      {error ? <span className="field__error" id={`${inputId}-error`}>{error}</span> : null}
      {!error && hint ? <span className="field__hint" id={`${inputId}-hint`}>{hint}</span> : null}
    </div>
  )
})
PasswordInput.displayName = 'PasswordInput'
