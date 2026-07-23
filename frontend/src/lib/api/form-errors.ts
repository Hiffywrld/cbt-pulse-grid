import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { isApiClientError } from './api-error'

export const applyApiFormErrors = <T extends FieldValues>(error: unknown, setError: UseFormSetError<T>) => {
  if (!isApiClientError(error)) return false
  Object.entries(error.validationErrors).forEach(([field, message]) => {
    setError(field as Path<T>, { type: 'server', message })
  })
  return Object.keys(error.validationErrors).length > 0
}

export const friendlyApiError = (error: unknown) => {
  if (!isApiClientError(error)) return 'The request could not be completed.'
  if (error.status === 403) return 'You do not have permission to perform this action.'
  if (error.status === 404) return 'The requested record could not be found.'
  if (error.status === 409) return error.message || 'This change conflicts with an existing record.'
  return error.message
}
