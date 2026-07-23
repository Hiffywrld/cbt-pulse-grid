import { z } from 'zod'

const httpUrl = z.string().url().refine(
  (value) => value.startsWith('http://') || value.startsWith('https://'),
  'must use http or https',
)

const websocketUrl = z.string().url().refine(
  (value) => value.startsWith('ws://') || value.startsWith('wss://'),
  'must use ws or wss',
)

const environmentSchema = z.object({
  VITE_API_BASE_URL: httpUrl,
  VITE_WS_URL: websocketUrl,
})

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
  VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  VITE_WS_URL: import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws',
})
