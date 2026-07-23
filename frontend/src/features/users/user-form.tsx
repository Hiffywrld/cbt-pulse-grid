import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { Alert } from '../../components/feedback/alert'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { PasswordInput } from '../../components/ui/password-input'
import { Select } from '../../components/ui/select'
import { applyApiFormErrors, friendlyApiError } from '../../lib/api/form-errors'
import type { Role } from '../../types/auth'
import type { Institution, ManagedUser } from '../../types/management'
import type { CreateUserBody, UpdateUserBody } from './users-api'

const schema = z.object({ firstName: z.string().trim().min(1, 'First name is required').max(100), lastName: z.string().trim().min(1, 'Last name is required').max(100), email: z.string().trim().email('Enter a valid email').max(254), password: z.string().min(8, 'Use at least 8 characters').max(200), institutionId: z.string().optional(), role: z.enum(['INSTITUTION_ADMIN', 'EXAMINER', 'INVIGILATOR', 'STUDENT']), registrationNumber: z.string().max(100).optional() }).superRefine((value, context) => { if (value.role === 'STUDENT' && !value.registrationNumber?.trim()) context.addIssue({ code: 'custom', path: ['registrationNumber'], message: 'Registration number is required for students' }) })
type Values = z.infer<typeof schema>
type ManageableRole = Exclude<Role, 'SUPER_ADMIN'>

export const UserForm = ({ user, institutions, allowedRoles, fixedInstitutionId, onSubmit, onCancel }: { user?: ManagedUser; institutions?: Institution[]; allowedRoles: ManageableRole[]; fixedInstitutionId?: string; onSubmit(body: CreateUserBody | UpdateUserBody): Promise<void>; onCancel(): void }) => {
  const editing = Boolean(user)
  const initialRole = allowedRoles.find((role) => user?.roles.includes(role)) ?? allowedRoles[0]
  const [error, setErrorMessage] = useState<string | null>(null)
  const { register, control, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { firstName: user?.firstName ?? '', lastName: user?.lastName ?? '', email: user?.email ?? '', password: '', institutionId: user?.institutionId ?? fixedInstitutionId ?? '', role: initialRole, registrationNumber: user?.registrationNumber ?? '' } })
  useEffect(() => reset({ firstName: user?.firstName ?? '', lastName: user?.lastName ?? '', email: user?.email ?? '', password: '', institutionId: user?.institutionId ?? fixedInstitutionId ?? '', role: initialRole, registrationNumber: user?.registrationNumber ?? '' }), [fixedInstitutionId, initialRole, reset, user])
  const role = useWatch({ control, name: 'role' })
  const submit = handleSubmit(async (values) => {
    setErrorMessage(null)
    const body = editing ? { firstName: values.firstName, lastName: values.lastName, registrationNumber: values.registrationNumber?.trim() || null } : { firstName: values.firstName, lastName: values.lastName, email: values.email, password: values.password, institutionId: institutions ? values.institutionId : undefined, roles: [values.role], registrationNumber: values.registrationNumber?.trim() || null }
    try { await onSubmit(body) } catch (failure) { if (!applyApiFormErrors(failure, setError)) setErrorMessage(friendlyApiError(failure)) }
  })
  return <form className="management-form" onSubmit={submit} noValidate>
    {error ? <Alert tone="error">{error}</Alert> : null}
    <div className="form-grid"><Input label="First name" {...register('firstName')} error={errors.firstName?.message} /><Input label="Last name" {...register('lastName')} error={errors.lastName?.message} /></div>
    {!editing ? <><Input label="Email" type="email" {...register('email')} error={errors.email?.message} /><PasswordInput label="Temporary password" {...register('password')} error={errors.password?.message} />{institutions ? <Select label="Institution" {...register('institutionId')} error={errors.institutionId?.message}><option value="">Select institution</option>{institutions.map((institution) => <option key={institution.id} value={institution.id}>{institution.name} ({institution.code})</option>)}</Select> : null}<Select label="Role" {...register('role')} error={errors.role?.message}>{allowedRoles.map((allowedRole) => <option key={allowedRole} value={allowedRole}>{allowedRole.replaceAll('_', ' ')}</option>)}</Select></> : null}
    {(editing ? user?.roles.includes('STUDENT') : role === 'STUDENT') ? <Input label="Registration number" {...register('registrationNumber')} error={errors.registrationNumber?.message} /> : null}
    <div className="modal-actions"><Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button><Button type="submit" loading={isSubmitting}>{editing ? 'Save changes' : 'Create account'}</Button></div>
  </form>
}
