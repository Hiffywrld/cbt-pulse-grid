import type { CurrentUser } from '../../types/auth'

export const UserAvatar = ({ user, size = 'md' }: { user: Pick<CurrentUser, 'firstName' | 'lastName' | 'email' | 'avatarKey'>; size?: 'sm' | 'md' | 'lg' }) => {
  const initials = `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.trim().toUpperCase() || user.email[0]?.toUpperCase() || '?'
  return <span className={`user-avatar user-avatar--${size} ${user.avatarKey ? `user-avatar--${user.avatarKey}` : ''}`} aria-label={`${initials} avatar`}>
    <span aria-hidden="true">{initials}</span><i aria-hidden="true" />
  </span>
}
