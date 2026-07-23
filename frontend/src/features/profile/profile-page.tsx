import { Check, LockKeyhole, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { PageHeader } from '../../components/layout/page-header'
import { Alert } from '../../components/feedback/alert'
import { UserAvatar } from '../../components/identity/avatar'
import { avatarKeys } from '../../components/identity/avatar-keys'
import { Button } from '../../components/ui/button'
import { Card } from '../../components/ui/card'
import { Input } from '../../components/ui/input'
import { PasswordInput } from '../../components/ui/password-input'
import { friendlyApiError } from '../../lib/api/form-errors'
import { authApi } from '../auth/auth-api'
import { institutionNameFor } from '../auth/identity-display'
import { useAuth } from '../auth/use-auth'

type ProfileValues = { firstName: string; lastName: string; avatarKey: string | null }
type PasswordValues = { currentPassword: string; newPassword: string; confirmPassword: string }

export const ProfilePage = () => {
  const { user, refreshProfile, logout } = useAuth()
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const profile = useForm<ProfileValues>({ defaultValues: { firstName: user?.firstName ?? '', lastName: user?.lastName ?? '', avatarKey: user?.avatarKey ?? null } })
  const password = useForm<PasswordValues>({ defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' } })
  useEffect(() => profile.reset({ firstName: user?.firstName ?? '', lastName: user?.lastName ?? '', avatarKey: user?.avatarKey ?? null }), [user, profile])
  if (!user) return null
  const selected = profile.watch('avatarKey')
  const preview = { ...user, firstName: profile.watch('firstName'), lastName: profile.watch('lastName'), avatarKey: selected }
  const saveProfile = profile.handleSubmit(async (values) => {
    setError(null); setNotice(null)
    if (!values.firstName.trim()) return profile.setError('firstName', { message: 'First name is required' })
    if (!values.lastName.trim()) return profile.setError('lastName', { message: 'Last name is required' })
    try {
      await authApi.updateProfile({ ...values, firstName: values.firstName.trim(), lastName: values.lastName.trim() })
      await refreshProfile?.(); setNotice('Profile updated.')
    } catch (failure) { setError(friendlyApiError(failure)) }
  })
  const changePassword = password.handleSubmit(async (values) => {
    setError(null); setNotice(null)
    if (values.newPassword.length < 8) return password.setError('newPassword', { message: 'Use at least 8 characters' })
    if (values.newPassword !== values.confirmPassword) return password.setError('confirmPassword', { message: 'Passwords do not match' })
    try {
      await authApi.changePassword(values)
      password.reset()
      await logout()
    } catch (failure) { setError(friendlyApiError(failure)) }
  })
  return <div>
    <PageHeader eyebrow="Personal account" title="Profile" description="Update your display identity and password. Institution, role, email, status and registration number remain administrator-controlled." />
    {notice && <Alert tone="success">{notice}</Alert>}{error && <Alert tone="error">{error}</Alert>}
    <div className="profile-layout">
      <Card className="profile-preview"><UserAvatar user={preview} size="lg" /><h2>{preview.firstName || user.email} {preview.lastName}</h2><p>{user.email}</p><span>{institutionNameFor(user)}</span></Card>
      <Card className="profile-panel"><header><UserRound /><div><h2>Identity</h2><p>Choose names and a locally bundled avatar.</p></div></header>
        <form onSubmit={saveProfile} className="management-form"><div className="form-grid"><Input label="First name" {...profile.register('firstName')} error={profile.formState.errors.firstName?.message} /><Input label="Last name" {...profile.register('lastName')} error={profile.formState.errors.lastName?.message} /></div>
          <fieldset className="avatar-picker"><legend>Built-in avatar</legend>{avatarKeys.map((key) => <button type="button" key={key} className={selected === key ? 'selected' : ''} aria-label={`Select ${key} avatar`} aria-pressed={selected === key} onClick={() => profile.setValue('avatarKey', key, { shouldDirty: true })}><UserAvatar user={{ ...preview, avatarKey: key }} />{selected === key && <Check />}</button>)}</fieldset>
          <div className="modal-actions"><Button type="submit" loading={profile.formState.isSubmitting}>Save profile</Button></div>
        </form>
      </Card>
      <Card className="profile-panel"><header><LockKeyhole /><div><h2>Change password</h2><p>All refresh-token sessions are revoked after a successful change.</p></div></header>
        <form onSubmit={changePassword} className="management-form"><PasswordInput label="Current password" autoComplete="current-password" {...password.register('currentPassword')} error={password.formState.errors.currentPassword?.message} /><PasswordInput label="New password" autoComplete="new-password" {...password.register('newPassword')} error={password.formState.errors.newPassword?.message} /><PasswordInput label="Confirm new password" autoComplete="new-password" {...password.register('confirmPassword')} error={password.formState.errors.confirmPassword?.message} /><div className="modal-actions"><Button type="submit" loading={password.formState.isSubmitting}>Change password</Button></div></form>
      </Card>
    </div>
  </div>
}
