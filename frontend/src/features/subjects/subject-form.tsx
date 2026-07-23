import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '../../components/feedback/alert'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { Textarea } from '../../components/ui/textarea'
import { applyApiFormErrors, friendlyApiError } from '../../lib/api/form-errors'
import type { Subject, SubjectInput } from '../../types/academic'

const schema = z.object({
  code: z.string().trim().min(1, 'Code is required').max(50),
  name: z.string().trim().min(1, 'Name is required').max(150),
  description: z.string().max(4000).optional(),
})
type Values = z.infer<typeof schema>

export const SubjectForm = ({ subject, onSubmit, onCancel }: {
  subject?: Subject
  onSubmit(body: SubjectInput): Promise<void>
  onCancel(): void
}) => {
  const [error, setErrorMessage] = useState<string | null>(null)
  const { register, reset, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { code: subject?.code ?? '', name: subject?.name ?? '', description: subject?.description ?? '' },
  })
  useEffect(() => reset({ code: subject?.code ?? '', name: subject?.name ?? '', description: subject?.description ?? '' }), [reset, subject])
  const submit = handleSubmit(async (values) => {
    setErrorMessage(null)
    try {
      await onSubmit({ code: values.code, name: values.name, description: values.description?.trim() || null })
    } catch (failure) {
      if (!applyApiFormErrors(failure, setError)) setErrorMessage(friendlyApiError(failure))
    }
  })
  return <form className="management-form" onSubmit={submit} noValidate>
    {error ? <Alert tone="error">{error}</Alert> : null}
    <div className="form-grid">
      <Input label="Subject code" {...register('code')} error={errors.code?.message} hint="Stored in uppercase." />
      <Input label="Subject name" {...register('name')} error={errors.name?.message} />
    </div>
    <Textarea label="Description" rows={4} {...register('description')} error={errors.description?.message} />
    <div className="modal-actions">
      <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>
      <Button type="submit" loading={isSubmitting}>{subject ? 'Save changes' : 'Create subject'}</Button>
    </div>
  </form>
}
