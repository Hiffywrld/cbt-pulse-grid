import { zodResolver } from '@hookform/resolvers/zod'
import { Plus, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '../../components/feedback/alert'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { Select } from '../../components/ui/select'
import { Textarea } from '../../components/ui/textarea'
import { applyApiFormErrors, friendlyApiError } from '../../lib/api/form-errors'
import type { ExamDetail, ExamInput, Subject } from '../../types/academic'

const ruleSchema = z.object({
  difficulty: z.enum(['EASY', 'MEDIUM', 'HARD']),
  questionCount: z.number().int().min(1, 'At least one question is required'),
  marksPerQuestion: z.number().positive('Marks must be greater than zero'),
})
const baseSchema = z.object({
  code: z.string().trim().min(1, 'Code is required').max(50),
  subjectId: z.string().min(1, 'Choose a subject'),
  title: z.string().trim().min(1, 'Title is required').max(200),
  instructions: z.string().max(10000).optional(),
  durationMinutes: z.number().int().min(1).max(480),
  startsAt: z.string().min(1, 'Start time is required'),
  endsAt: z.string().min(1, 'End time is required'),
  accessPin: z.string().optional(),
  passMarkPercentage: z.number().min(0).max(100),
  shuffleQuestions: z.boolean(),
  shuffleOptions: z.boolean(),
  poolRules: z.array(ruleSchema).min(1, 'Add at least one pool rule').max(3),
}).superRefine((value, context) => {
  if (new Date(value.startsAt) >= new Date(value.endsAt)) {
    context.addIssue({ code: 'custom', path: ['endsAt'], message: 'End time must be after start time' })
  }
  if (new Date(value.endsAt).getTime() - new Date(value.startsAt).getTime() < value.durationMinutes * 60_000) {
    context.addIssue({ code: 'custom', path: ['endsAt'], message: 'Exam window must cover the full duration' })
  }
  if (new Set(value.poolRules.map((rule) => rule.difficulty)).size !== value.poolRules.length) {
    context.addIssue({ code: 'custom', path: ['poolRules'], message: 'Each difficulty can appear only once' })
  }
})
type Values = z.infer<typeof baseSchema>

const toLocalInput = (instant?: string) => {
  if (!instant) return ''
  const date = new Date(instant)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16)
}

export const ExamForm = ({ exam, subjects, onSubmit, onCancel }: {
  exam?: ExamDetail
  subjects: Subject[]
  onSubmit(body: ExamInput & { accessPin?: string }): Promise<void>
  onCancel(): void
}) => {
  const editing = Boolean(exam)
  const [error, setErrorMessage] = useState<string | null>(null)
  const defaults: Values = {
    code: exam?.code ?? '',
    subjectId: exam?.subjectId ?? subjects[0]?.id ?? '',
    title: exam?.title ?? '',
    instructions: exam?.instructions ?? '',
    durationMinutes: exam?.durationMinutes ?? 60,
    startsAt: toLocalInput(exam?.startsAt),
    endsAt: toLocalInput(exam?.endsAt),
    accessPin: '',
    passMarkPercentage: exam?.passMarkPercentage ?? 50,
    shuffleQuestions: exam?.shuffleQuestions ?? true,
    shuffleOptions: exam?.shuffleOptions ?? true,
    poolRules: exam?.poolRules.map(({ difficulty, questionCount, marksPerQuestion }) => ({ difficulty, questionCount, marksPerQuestion })) ?? [{ difficulty: 'MEDIUM', questionCount: 10, marksPerQuestion: 1 }],
  }
  const { register, control, reset, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(baseSchema),
    defaultValues: defaults,
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'poolRules' })
  useEffect(() => reset(defaults), [exam]) // eslint-disable-line react-hooks/exhaustive-deps
  const submit = handleSubmit(async (values) => {
    setErrorMessage(null)
    if (!editing && !/^\d{6}$/.test(values.accessPin ?? '')) {
      setError('accessPin', { message: 'Enter exactly six digits' }); return
    }
    const body: ExamInput & { accessPin?: string } = {
      ...values,
      code: values.code.trim(),
      instructions: values.instructions?.trim() || null,
      startsAt: new Date(values.startsAt).toISOString(),
      endsAt: new Date(values.endsAt).toISOString(),
      accessPin: editing ? undefined : values.accessPin,
    }
    try {
      await onSubmit(body)
    } catch (failure) {
      if (!applyApiFormErrors(failure, setError)) setErrorMessage(friendlyApiError(failure))
    }
  })
  return <form className="management-form exam-form" onSubmit={submit} noValidate>
    {error ? <Alert tone="error">{error}</Alert> : null}
    <div className="form-grid">
      <Input label="Exam code" {...register('code')} error={errors.code?.message} hint="Stored in uppercase." />
      <Select label="Subject" {...register('subjectId')} error={errors.subjectId?.message}><option value="">Choose subject</option>{subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code} — {subject.name}</option>)}</Select>
    </div>
    <Input label="Exam title" {...register('title')} error={errors.title?.message} />
    <Textarea label="Candidate instructions" rows={4} {...register('instructions')} error={errors.instructions?.message} />
    <div className="form-grid form-grid--three">
      <Input label="Duration (minutes)" type="number" min="1" max="480" {...register('durationMinutes', { valueAsNumber: true })} error={errors.durationMinutes?.message} />
      <Input label="Starts at" type="datetime-local" {...register('startsAt')} error={errors.startsAt?.message} />
      <Input label="Ends at" type="datetime-local" {...register('endsAt')} error={errors.endsAt?.message} />
    </div>
    <div className="form-grid">
      {!editing ? <Input label="Six-digit access PIN" type="password" inputMode="numeric" maxLength={6} autoComplete="new-password" {...register('accessPin')} error={errors.accessPin?.message} hint="The PIN is hashed and never displayed again." /> : null}
      <Input label="Pass mark (%)" type="number" min="0" max="100" step="0.01" {...register('passMarkPercentage', { valueAsNumber: true })} error={errors.passMarkPercentage?.message} />
    </div>
    <div className="check-grid">
      <label className="check-control"><input type="checkbox" {...register('shuffleQuestions')} /><span>Shuffle questions</span></label>
      <label className="check-control"><input type="checkbox" {...register('shuffleOptions')} /><span>Shuffle answer options</span></label>
    </div>
    <fieldset className="pool-editor">
      <legend>Question pool rules</legend>
      <p>Publishing checks that enough published questions exist for every rule.</p>
      {fields.map((field, index) => <div className="pool-row" key={field.id}>
        <Select label={`Difficulty ${index + 1}`} {...register(`poolRules.${index}.difficulty`)} error={errors.poolRules?.[index]?.difficulty?.message}><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option></Select>
        <Input label="Question count" type="number" min="1" {...register(`poolRules.${index}.questionCount`, { valueAsNumber: true })} error={errors.poolRules?.[index]?.questionCount?.message} />
        <Input label="Marks each" type="number" min="0.01" step="0.01" {...register(`poolRules.${index}.marksPerQuestion`, { valueAsNumber: true })} error={errors.poolRules?.[index]?.marksPerQuestion?.message} />
        <Button type="button" size="sm" variant="ghost" icon={<Trash2 size={16} />} disabled={fields.length === 1} onClick={() => remove(index)}>Remove</Button>
      </div>)}
      {typeof errors.poolRules?.message === 'string' ? <span className="field__error">{errors.poolRules.message}</span> : null}
      {errors.poolRules?.root?.message ? <span className="field__error">{errors.poolRules.root.message}</span> : null}
      <Button type="button" variant="secondary" size="sm" icon={<Plus size={16} />} disabled={fields.length >= 3} onClick={() => append({ difficulty: 'EASY', questionCount: 5, marksPerQuestion: 1 })}>Add pool rule</Button>
    </fieldset>
    <div className="modal-actions"><Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button><Button type="submit" loading={isSubmitting}>{editing ? 'Save exam' : 'Create draft exam'}</Button></div>
  </form>
}
