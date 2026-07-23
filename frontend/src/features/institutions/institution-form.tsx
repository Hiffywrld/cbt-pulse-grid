import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '../../components/feedback/alert'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { applyApiFormErrors, friendlyApiError } from '../../lib/api/form-errors'
import type { Institution } from '../../types/management'

const schema = z.object({ name: z.string().trim().min(1, 'Name is required').max(160), code: z.string().trim().min(1, 'Code is required').max(32) })
type Values = z.infer<typeof schema>

export const InstitutionForm = ({ institution, onSubmit, onCancel }: { institution?: Institution; onSubmit(values: Values): Promise<void>; onCancel(): void }) => {
  const [error, setErrorMessage] = useState<string | null>(null)
  const { register, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name: institution?.name ?? '', code: institution?.code ?? '' } })
  useEffect(() => reset({ name: institution?.name ?? '', code: institution?.code ?? '' }), [institution, reset])
  const submit = handleSubmit(async (values) => {
    setErrorMessage(null)
    try { await onSubmit(values) } catch (failure) { if (!applyApiFormErrors(failure, setError)) setErrorMessage(friendlyApiError(failure)) }
  })
  return <form className="management-form" onSubmit={submit} noValidate>
    {error ? <Alert tone="error">{error}</Alert> : null}
    <Input label="Institution name" {...register('name')} error={errors.name?.message} />
    <Input label="Institution code" {...register('code')} error={errors.code?.message} disabled={Boolean(institution)} hint={institution ? 'Codes cannot be changed after creation.' : 'Stored in uppercase by the backend.'} />
    <div className="modal-actions"><Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button><Button type="submit" loading={isSubmitting}>{institution ? 'Save changes' : 'Create institution'}</Button></div>
  </form>
}
