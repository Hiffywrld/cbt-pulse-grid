import type { ApiErrorBody } from '../../types/api'

export class ApiClientError extends Error {
  readonly status: number
  readonly requestId?: string
  readonly validationErrors: Record<string, string>

  constructor(body: ApiErrorBody) {
    const validationMessage = Object.values(body.validationErrors ?? {})[0]
    super(validationMessage ? `${body.message}: ${validationMessage}` : body.message)
    this.name = 'ApiClientError'
    this.status = body.status
    this.requestId = body.requestId
    this.validationErrors = body.validationErrors ?? {}
  }
}

export const isApiClientError = (error: unknown): error is ApiClientError =>
  error instanceof ApiClientError
