import { ChevronLeft, ChevronRight, Menu, PanelLeftClose, X } from 'lucide-react'
import { useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { navigationFor } from '../../app/navigation'
import { institutionCodeFor, institutionNameFor } from '../../features/auth/identity-display'
import { useAuth } from '../../features/auth/use-auth'
import { Brand } from './brand'
import { UserMenu } from './user-menu'

export const AppShell = () => {
  const { user } = useAuth()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  if (!user) return null
  const navigation = navigationFor(user)
  const current = [...navigation].sort((a, b) => b.to.length - a.to.length).find((item) => location.pathname === item.to || location.pathname.startsWith(`${item.to}/`))
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
          <div className="topbar__user"><UserMenu /></div>
        </header>
        <div className="app-content"><Outlet /></div>
      </div>
    </div>
  )
}
