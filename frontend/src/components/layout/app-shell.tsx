import { ChevronLeft, ChevronRight, LogOut, Menu, PanelLeftClose, X } from 'lucide-react'
import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { navigationFor, primaryRoleFor } from '../../app/navigation'
import { fullNameFor, institutionCodeFor, institutionNameFor } from '../../features/auth/identity-display'
import { roleLabel } from '../../features/auth/role-routing'
import { useAuth } from '../../features/auth/use-auth'
import { Badge } from '../ui/badge'
import { Button } from '../ui/button'
import { Brand } from './brand'

export const AppShell = () => {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  if (!user) return null
  const navigation = navigationFor(user)
  const current = [...navigation].sort((a, b) => b.to.length - a.to.length).find((item) => location.pathname === item.to || location.pathname.startsWith(`${item.to}/`))
  const primaryRole = primaryRoleFor(user)
  const fullName = fullNameFor(user)
  const institutionName = institutionNameFor(user)
  const institutionCode = institutionCodeFor(user)
  const sidebar = (
    <>
      <div className="sidebar__brand-row"><Brand compact={collapsed} /><button className="icon-button sidebar__mobile-close" onClick={() => setMobileOpen(false)} aria-label="Close navigation"><X size={20} /></button></div>
      <div className="sidebar__context"><span className="sidebar__context-dot" /><span><small>{institutionCode ?? 'Workspace'}</small><strong>{institutionName}</strong></span></div>
      <nav className="sidebar__nav" aria-label="Primary navigation">
        <p className="sidebar__nav-label">Navigation</p>
        {navigation.map(({ label, to, icon: Icon }) => <NavLink key={to} to={to} end={to === '/platform' || to === '/institution' || to === '/student'} onClick={() => setMobileOpen(false)} title={collapsed ? label : undefined}><Icon size={20} aria-hidden="true" /><span>{label}</span></NavLink>)}
      </nav>
      <div className="sidebar__footer">
        <div className="sidebar__security"><span className="shield-pulse" /><div><strong>Secure session</strong><small>JWT protected</small></div></div>
        <button className="sidebar__collapse" onClick={() => setCollapsed((value) => !value)} aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}><PanelLeftClose size={18} /><span>Collapse sidebar</span>{collapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}</button>
      </div>
    </>
  )
  return (
    <div className={`app-shell ${collapsed ? 'app-shell--collapsed' : ''}`}>
      <aside className="sidebar">{sidebar}</aside>
      {mobileOpen ? <div className="mobile-drawer"><button className="mobile-drawer__backdrop" aria-label="Close navigation" onClick={() => setMobileOpen(false)} /><aside className="sidebar sidebar--mobile">{sidebar}</aside></div> : null}
      <div className="app-shell__main">
        <header className="topbar">
          <button className="icon-button topbar__menu" onClick={() => setMobileOpen(true)} aria-label="Open navigation"><Menu size={21} /></button>
          <div className="topbar__crumbs"><span>Workspace</span><ChevronRight size={14} /><strong>{current?.label ?? 'Overview'}</strong></div>
          <div className="topbar__user"><div className="topbar__identity"><span className="avatar">{fullName.slice(0, 1).toUpperCase()}</span><span><strong>{fullName}</strong><small>{roleLabel(primaryRole)}</small></span></div><Badge tone="success">Active</Badge><Button variant="ghost" size="sm" icon={<LogOut size={17} />} onClick={() => void logout()}>Log out</Button></div>
        </header>
        <div className="app-content"><Outlet /></div>
      </div>
    </div>
  )
}
