import { Monitor, Moon, Sun } from 'lucide-react'
import type { ThemePreference } from './theme-context'
import { useTheme } from './use-theme'

const icons = {
  LIGHT: Sun,
  DARK: Moon,
  SYSTEM: Monitor,
} satisfies Record<ThemePreference, typeof Sun>

export const ThemeControl = () => {
  const { preference, setPreference } = useTheme()
  const Icon = icons[preference]

  return (
    <div className="theme-control">
      <Icon size={17} aria-hidden="true" />
      <label className="sr-only" htmlFor="theme-preference">Color theme</label>
      <select
        id="theme-preference"
        aria-label="Color theme"
        value={preference}
        onChange={(event) => setPreference(event.target.value as ThemePreference)}
      >
        <option value="LIGHT">Light</option>
        <option value="DARK">Dark</option>
        <option value="SYSTEM">System</option>
      </select>
    </div>
  )
}
