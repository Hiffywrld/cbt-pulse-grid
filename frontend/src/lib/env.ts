import { z } from 'zod'

const httpUrl = z.string().refine((value) => {
  if (value === '') {
    return true
  }
  if (value.startsWith('/')) {
    return true
  }
  const parsed = z.string().url().safeParse(value)
  return parsed.success && (value.startsWith('http://') || value.startsWith('https://'))
}, 'must use http, https, an empty same-origin base or a same-origin path')

const websocketUrl = z.string().refine((value) => {
  if (value.startsWith('/')) {
    return true
  }
  const parsed = z.string().url().safeParse(value)
  return parsed.success && (value.startsWith('ws://') || value.startsWith('wss://'))
}, 'must use ws, wss or a same-origin path')

const environmentSchema = z.object({
  VITE_API_BASE_URL: httpUrl,
  VITE_WS_URL: websocketUrl,
})

declare global {
  interface Window {
    __CBT_PULSE_GRID_CONFIG__?: Partial<Record<'VITE_API_BASE_URL' | 'VITE_WS_URL', string>>
  }
}

export type FrontendEnvironment = {
  apiBaseUrl: string
  websocketUrl: string
}

export const parseEnvironment = (
  source: Record<string, string | boolean | undefined>,
): FrontendEnvironment => {
  const parsed = environmentSchema.safeParse(source)
  if (!parsed.success) {
    const fields = parsed.error.issues.map((issue) => issue.path.join('.')).join(', ')
    throw new Error(`Frontend environment configuration is invalid: ${fields}`)
  }
  return {
    apiBaseUrl: parsed.data.VITE_API_BASE_URL.replace(/\/$/, ''),
    websocketUrl: parsed.data.VITE_WS_URL,
  }
}

export const environment = parseEnvironment({
  VITE_API_BASE_URL:
    globalThis.window?.__CBT_PULSE_GRID_CONFIG__?.VITE_API_BASE_URL ??
    import.meta.env.VITE_API_BASE_URL ??
    'http://localhost:8080',
  VITE_WS_URL:
    globalThis.window?.__CBT_PULSE_GRID_CONFIG__?.VITE_WS_URL ??
    import.meta.env.VITE_WS_URL ??
    'ws://localhost:8080/ws',
})
