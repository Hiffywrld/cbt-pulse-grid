import { createContext } from 'react'

export type ThemePreference = 'LIGHT' | 'DARK' | 'SYSTEM'
export type ResolvedTheme = 'light' | 'dark'

export type ThemeContextValue = {
  preference: ThemePreference
  resolvedTheme: ResolvedTheme
  setPreference(preference: ThemePreference): void
}

export const ThemeContext = createContext<ThemeContextValue | null>(null)
