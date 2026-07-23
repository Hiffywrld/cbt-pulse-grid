import { forwardRef, type SelectHTMLAttributes } from 'react'

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label: string
  error?: string
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, error, id, name, children, ...props }, ref) => {
    const selectId = id ?? name
    return (
      <div className="field">
        <label className="field__label" htmlFor={selectId}>{label}</label>
        <select
          ref={ref}
          id={selectId}
          name={name}
          className={`select ${error ? 'input--error' : ''}`}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${selectId}-error` : undefined}
          {...props}
        >
          {children}
        </select>
        {error ? <span className="field__error" id={`${selectId}-error`}>{error}</span> : null}
      </div>
    )
  },
)
Select.displayName = 'Select'
