import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { ThemeContext, type ResolvedTheme, type ThemePreference } from './theme-context'

export const THEME_STORAGE_KEY = 'cbt-pulse-grid-theme'
const DARK_MODE_QUERY = '(prefers-color-scheme: dark)'
const preferences: ThemePreference[] = ['LIGHT', 'DARK', 'SYSTEM']

const systemTheme = (): ResolvedTheme =>
  window.matchMedia(DARK_MODE_QUERY).matches ? 'dark' : 'light'

const storedPreference = (): ThemePreference => {
  try {
    const stored = window.localStorage.getItem(THEME_STORAGE_KEY)
    return preferences.includes(stored as ThemePreference) ? stored as ThemePreference : 'SYSTEM'
  } catch {
    return 'SYSTEM'
  }
}

const resolveTheme = (preference: ThemePreference): ResolvedTheme =>
  preference === 'SYSTEM' ? systemTheme() : preference.toLowerCase() as ResolvedTheme

const applyTheme = (preference: ThemePreference, resolvedTheme: ResolvedTheme) => {
  const root = document.documentElement
  root.dataset.theme = resolvedTheme
  root.dataset.themePreference = preference.toLowerCase()
  root.style.colorScheme = resolvedTheme
  document.querySelector<HTMLMetaElement>('meta[name="theme-color"]')
    ?.setAttribute('content', resolvedTheme === 'dark' ? '#08110D' : '#F5F8F6')
}

export const ThemeProvider = ({ children }: { children: ReactNode }) => {
  const [preference, setPreferenceState] = useState<ThemePreference>(storedPreference)
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => resolveTheme(preference))

  useEffect(() => {
    const media = window.matchMedia(DARK_MODE_QUERY)
    const update = () => {
      const next = resolveTheme(preference)
      setResolvedTheme(next)
      applyTheme(preference, next)
    }
    update()
    if (preference === 'SYSTEM') media.addEventListener('change', update)
    return () => media.removeEventListener('change', update)
  }, [preference])

  const setPreference = (next: ThemePreference) => {
    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, next)
    } catch {
      // Theme selection still applies for the current session when storage is unavailable.
    }
    setPreferenceState(next)
  }

  const value = useMemo(
    () => ({ preference, resolvedTheme, setPreference }),
    [preference, resolvedTheme],
  )

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}
