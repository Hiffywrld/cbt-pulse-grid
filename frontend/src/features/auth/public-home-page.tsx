import { Activity, ArrowRight, Clock3, Network, Shuffle, ShieldCheck, Webhook, Zap } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Brand } from '../../components/layout/brand'
import { Button } from '../../components/ui/button'
import { ThemeControl } from '../theme/theme-control'
import { homeForUser } from './role-routing'
import { useAuth } from './use-auth'

const capabilities = [
  [Network, 'Offline-first delivery', 'Run resilient examinations across institution-controlled local networks.'],
  [Shuffle, 'Randomized pools', 'Build each attempt from validated difficulty pools without exposing correct answers.'],
  [Clock3, 'Authoritative timing', 'The backend controls availability, expiry and automatic submission.'],
  [Activity, 'Live monitoring', 'Invigilators receive tenant-secured connectivity and anti-cheat signals.'],
  [Zap, 'Automatic scoring', 'Objective questions are scored immediately from immutable attempt snapshots.'],
  [Webhook, 'Signed integrations', 'Deliver operational signals through retryable HMAC-signed webhooks.'],
] as const

export const PublicHomePage = () => {
  const { status, user } = useAuth()
  const action = status === 'authenticated' && user ? homeForUser(user) : '/login'
  return <main className="public-home">
    <header className="public-nav"><Brand /><nav aria-label="Public navigation"><a href="#capabilities">Capabilities</a><a href="#roles">Roles</a><a href="#security">Security</a></nav><div><ThemeControl /><Link to={action}><Button>{user ? 'Open dashboard' : 'Sign in'} <ArrowRight size={17} /></Button></Link></div></header>
    <section className="public-hero"><div><p className="eyebrow">Secure institutional assessment infrastructure</p><h1>Examinations that keep working when the internet does not.</h1><p>CBT-Pulse Grid coordinates concurrent computer-based tests, monitoring and rapid scoring across secure institutional networks.</p><div className="public-hero__actions"><Link to={action}><Button size="lg">{user ? 'Open your dashboard' : 'Sign in to your workspace'} <ArrowRight size={18} /></Button></Link><span>Accounts are provisioned by authorized administrators.</span></div></div><div className="public-signal" aria-hidden="true"><span /><span /><span /><i /></div></section>
    <section id="capabilities" className="public-section"><p className="eyebrow">Operational capabilities</p><h2>Built for the complete examination window</h2><div className="public-feature-grid">{capabilities.map(([Icon, title, text]) => <article key={title}><Icon /><h3>{title}</h3><p>{text}</p></article>)}</div></section>
    <section id="roles" className="public-section public-roles"><div><p className="eyebrow">Role-bound workspaces</p><h2>Every participant sees only the work they are authorized to perform.</h2></div><div>{['Administrators govern institutions, accounts and integrations.', 'Examiners prepare questions, pools and examination schedules.', 'Invigilators monitor live attempts without accessing answers.', 'Students receive a calm, resumable and time-authoritative runner.'].map((text) => <p key={text}><ShieldCheck />{text}</p>)}</div></section>
    <section id="security" className="public-section public-security"><ShieldCheck /><div><p className="eyebrow">Production-minded architecture</p><h2>Tenant isolation from API to database.</h2><p>Spring Boot modular services, PostgreSQL persistence, Flyway migrations, short-lived JWT access, refresh rotation, immutable audit history and container-ready deployment provide a disciplined foundation.</p></div></section>
    <footer className="public-footer"><Brand /><p>CBT-Pulse Grid · Distributed institutional computer based testing platform</p><Link to="/login">Institution sign in</Link></footer>
  </main>
}
