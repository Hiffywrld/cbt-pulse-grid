import { zodResolver } from '@hookform/resolvers/zod'
import { Plus, Trash2 } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '../../components/feedback/alert'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { Select } from '../../components/ui/select'
import { Textarea } from '../../components/ui/textarea'
import { applyApiFormErrors, friendlyApiError } from '../../lib/api/form-errors'
import type { QuestionInput, StaffQuestion, Subject } from '../../types/academic'

const optionSchema = z.object({
  optionText: z.string().trim().min(1, 'Option text is required').max(4000),
  correct: z.boolean(),
})
const schema = z.object({
  subjectId: z.string().min(1, 'Choose a subject'),
  questionText: z.string().trim().min(1, 'Question text is required').max(10000),
  type: z.enum(['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE']),
  difficulty: z.enum(['EASY', 'MEDIUM', 'HARD']),
  marks: z.number().positive('Marks must be greater than zero'),
  options: z.array(optionSchema).min(2, 'Add at least two options'),
}).superRefine((value, context) => {
  const correct = value.options.filter((option) => option.correct).length
  if ((value.type === 'SINGLE_CHOICE' || value.type === 'TRUE_FALSE') && correct !== 1) {
    context.addIssue({ code: 'custom', path: ['options'], message: 'Choose exactly one correct answer' })
  }
  if (value.type === 'MULTIPLE_CHOICE' && correct < 2) {
    context.addIssue({ code: 'custom', path: ['options'], message: 'Choose at least two correct answers' })
  }
  if (value.type === 'TRUE_FALSE' && value.options.length !== 2) {
    context.addIssue({ code: 'custom', path: ['options'], message: 'True/False questions require exactly two options' })
  }
})
type Values = z.infer<typeof schema>

const blankOptions = [{ optionText: '', correct: false }, { optionText: '', correct: false }]

export const QuestionForm = ({ question, subjects, onSubmit, onCancel }: {
  question?: StaffQuestion
  subjects: Subject[]
  onSubmit(body: QuestionInput): Promise<void>
  onCancel(): void
}) => {
  const [error, setErrorMessage] = useState<string | null>(null)
  const defaults: Values = {
    subjectId: question?.subjectId ?? subjects[0]?.id ?? '',
    questionText: question?.questionText ?? '',
    type: question?.type ?? 'SINGLE_CHOICE',
    difficulty: question?.difficulty ?? 'MEDIUM',
    marks: question?.marks ?? 1,
    options: question?.options.map(({ optionText, correct }) => ({ optionText, correct })) ?? blankOptions,
  }
  const { register, control, reset, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: defaults,
  })
  const { fields, append, remove, replace } = useFieldArray({ control, name: 'options' })
  const type = useWatch({ control, name: 'type' })
  useEffect(() => reset(defaults), [question]) // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (type === 'TRUE_FALSE' && !question) replace([{ optionText: 'True', correct: true }, { optionText: 'False', correct: false }])
  }, [question, replace, type])
  const submit = handleSubmit(async (values) => {
    setErrorMessage(null)
    const body: QuestionInput = {
      ...values,
      options: values.options.map((option, index) => ({ ...option, displayOrder: index + 1 })),
    }
    try {
      await onSubmit(body)
    } catch (failure) {
      if (!applyApiFormErrors(failure, setError)) setErrorMessage(friendlyApiError(failure))
    }
  })
  return <form className="management-form question-form" onSubmit={submit} noValidate>
    {error ? <Alert tone="error">{error}</Alert> : null}
    <Select label="Subject" {...register('subjectId')} error={errors.subjectId?.message}>
      <option value="">Choose subject</option>
      {subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.code} — {subject.name}</option>)}
    </Select>
    <Textarea label="Question text" rows={4} {...register('questionText')} error={errors.questionText?.message} />
    <div className="form-grid form-grid--three">
      <Select label="Question type" {...register('type')} error={errors.type?.message}>
        <option value="SINGLE_CHOICE">Single choice</option><option value="MULTIPLE_CHOICE">Multiple choice</option><option value="TRUE_FALSE">True / False</option>
      </Select>
      <Select label="Difficulty" {...register('difficulty')} error={errors.difficulty?.message}>
        <option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
      </Select>
      <Input label="Marks" type="number" min="0.01" step="0.01" {...register('marks', { valueAsNumber: true })} error={errors.marks?.message} />
    </div>
    <fieldset className="option-editor">
      <legend>Answer options</legend>
      <p>Mark the correct answer choices. Display order follows the order below.</p>
      {fields.map((field, index) => <div className="option-row" key={field.id}>
        <Input label={`Option ${index + 1}`} {...register(`options.${index}.optionText`)} error={errors.options?.[index]?.optionText?.message} />
        <label className="check-control"><input type="checkbox" {...register(`options.${index}.correct`)} /><span>Correct</span></label>
        <Button type="button" size="sm" variant="ghost" icon={<Trash2 size={16} />} disabled={fields.length <= 2 || type === 'TRUE_FALSE'} onClick={() => remove(index)}>Remove</Button>
      </div>)}
      {typeof errors.options?.message === 'string' ? <span className="field__error">{errors.options.message}</span> : null}
      {errors.options?.root?.message ? <span className="field__error">{errors.options.root.message}</span> : null}
      {type !== 'TRUE_FALSE' ? <Button type="button" variant="secondary" size="sm" icon={<Plus size={16} />} onClick={() => append({ optionText: '', correct: false })}>Add option</Button> : null}
    </fieldset>
    <div className="modal-actions"><Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button><Button type="submit" loading={isSubmitting}>{question ? 'Save question' : 'Create draft'}</Button></div>
  </form>
}
