export type ApiErrorBody = {
  timestamp?: string
  status: number
  error: string
  message: string
  path?: string
  requestId?: string
  validationErrors?: Record<string, string>
}

export type ResponseType = 'json' | 'blob' | 'text'
