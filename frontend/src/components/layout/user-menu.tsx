import { ExternalLink, HelpCircle, LayoutDashboard, LogOut, UserRound } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { primaryRoleFor } from '../../app/navigation'
import { fullNameFor, institutionNameFor } from '../../features/auth/identity-display'
import { homeForUser, roleLabel } from '../../features/auth/role-routing'
import { useAuth } from '../../features/auth/use-auth'
import { ThemeControl } from '../../features/theme/theme-control'
import { UserAvatar } from '../identity/avatar'

export const UserMenu = () => {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const root = useRef<HTMLDivElement>(null)
  const trigger = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (!open) return
    const outside = (event: PointerEvent) => { if (!root.current?.contains(event.target as Node)) setOpen(false) }
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape') { setOpen(false); trigger.current?.focus() } }
    document.addEventListener('pointerdown', outside)
    document.addEventListener('keydown', escape)
    return () => { document.removeEventListener('pointerdown', outside); document.removeEventListener('keydown', escape) }
  }, [open])
  if (!user) return null
  return <div className="user-menu" ref={root}>
    <button ref={trigger} className="user-menu__trigger" aria-label="Open user menu" aria-expanded={open} aria-haspopup="menu" onClick={() => setOpen((value) => !value)}><UserAvatar user={user} /><span><strong>{fullNameFor(user)}</strong><small>{roleLabel(primaryRoleFor(user))}</small></span></button>
    {open && <div className="user-menu__dropdown" role="menu">
      <div className="user-menu__summary"><UserAvatar user={user} size="lg" /><div><strong>{fullNameFor(user)}</strong><span>{roleLabel(primaryRoleFor(user))}</span><small>{institutionNameFor(user)}</small></div></div>
      <Link role="menuitem" to="/profile" onClick={() => setOpen(false)}><UserRound />Open profile</Link>
      <Link role="menuitem" to={homeForUser(user)} onClick={() => setOpen(false)}><LayoutDashboard />Open dashboard</Link>
      <div className="user-menu__theme"><span>Theme</span><ThemeControl /></div>
      <a role="menuitem" href="/#security" onClick={() => setOpen(false)}><HelpCircle />Help and about<ExternalLink /></a>
      <button role="menuitem" onClick={() => void logout()}><LogOut />Log out</button>
    </div>}
  </div>
}
