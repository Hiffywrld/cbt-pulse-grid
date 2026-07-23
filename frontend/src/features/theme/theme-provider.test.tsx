import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeControl } from './theme-control'
import { ThemeProvider, THEME_STORAGE_KEY } from './theme-provider'

const installMatchMedia = (initiallyDark: boolean) => {
  let dark = initiallyDark
  const listeners = new Set<(event: MediaQueryListEvent) => void>()
  const media = {
    get matches() { return dark },
    media: '(prefers-color-scheme: dark)',
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn((_type: string, listener: (event: MediaQueryListEvent) => void) => listeners.add(listener)),
    removeEventListener: vi.fn((_type: string, listener: (event: MediaQueryListEvent) => void) => listeners.delete(listener)),
    dispatchEvent: vi.fn(),
  }
  vi.mocked(window.matchMedia).mockImplementation(() => media as unknown as MediaQueryList)
  return {
    setDark(next: boolean) {
      dark = next
      listeners.forEach((listener) => listener({ matches: next, media: media.media } as MediaQueryListEvent))
    },
  }
}

describe('global theme', () => {
  beforeEach(() => {
    window.localStorage.clear()
    delete document.documentElement.dataset.theme
    delete document.documentElement.dataset.themePreference
    document.documentElement.style.colorScheme = ''
  })

  it('follows the system preference on first visit and reacts to changes', async () => {
    const system = installMatchMedia(true)
    render(<ThemeProvider><ThemeControl /></ThemeProvider>)

    expect(screen.getByRole('combobox', { name: 'Color theme' })).toHaveValue('SYSTEM')
    await waitFor(() => expect(document.documentElement).toHaveAttribute('data-theme', 'dark'))
    expect(document.documentElement.style.colorScheme).toBe('dark')

    system.setDark(false)
    await waitFor(() => expect(document.documentElement).toHaveAttribute('data-theme', 'light'))
  })

  it('applies and persists explicit theme selection immediately', async () => {
    installMatchMedia(false)
    const { unmount } = render(<ThemeProvider><ThemeControl /></ThemeProvider>)
    const control = screen.getByRole('combobox', { name: 'Color theme' })

    expect(control).toHaveAccessibleName('Color theme')
    expect(screen.getByRole('option', { name: 'Light' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Dark' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'System' })).toBeInTheDocument()

    await userEvent.selectOptions(control, 'DARK')
    await waitFor(() => expect(document.documentElement).toHaveAttribute('data-theme', 'dark'))
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('DARK')
    expect(document.documentElement).toHaveAttribute('data-theme-preference', 'dark')

    unmount()
    document.documentElement.dataset.theme = 'light'
    render(<ThemeProvider><ThemeControl /></ThemeProvider>)
    expect(screen.getByRole('combobox', { name: 'Color theme' })).toHaveValue('DARK')
    await waitFor(() => expect(document.documentElement).toHaveAttribute('data-theme', 'dark'))
  })
})
