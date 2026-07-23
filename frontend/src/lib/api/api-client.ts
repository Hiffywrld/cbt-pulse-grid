import type { ApiErrorBody, ResponseType } from '../../types/api'
import type { TokenResponse } from '../../types/auth'
import type { SessionStore } from '../storage/session-storage'
import { ApiClientError } from './api-error'

export type ApiRequestOptions = Omit<RequestInit, 'body'> & {
  auth?: boolean
  body?: unknown
  responseType?: ResponseType
}

type SessionExpiredHandler = () => void

const genericError = (status: number): ApiErrorBody => ({
  status,
  error: 'Request failed',
  message: status === 0 ? 'Unable to reach the server' : 'The request could not be completed',
  validationErrors: {},
})

export class ApiClient {
  private refreshPromise: Promise<TokenResponse> | null = null
  private readonly baseUrl: string
  private readonly sessionStore: SessionStore
  private readonly fetcher: typeof fetch
  private sessionExpiredHandler: SessionExpiredHandler = () => {
    if (window.location.pathname !== '/login') window.location.assign('/login')
  }

  constructor(
    baseUrl: string,
    sessionStore: SessionStore,
    fetcher: typeof fetch = fetch,
  ) {
    this.baseUrl = baseUrl
    this.sessionStore = sessionStore
    this.fetcher = fetcher
  }

  setSessionExpiredHandler(handler: SessionExpiredHandler) {
    this.sessionExpiredHandler = handler
  }

  async request<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
    return this.execute<T>(path, options, true)
  }

  private async execute<T>(
    path: string,
    options: ApiRequestOptions,
    allowRefresh: boolean,
  ): Promise<T> {
    const { auth = true, body, responseType = 'json', headers, ...requestInit } = options
    const requestHeaders = new Headers(headers)
    requestHeaders.set('Accept', responseType === 'json' ? 'application/json' : '*/*')
    if (body !== undefined) requestHeaders.set('Content-Type', 'application/json')

    const session = this.sessionStore.read()
    if (auth && session?.accessToken) {
      requestHeaders.set('Authorization', `Bearer ${session.accessToken}`)
    } else if (!auth) {
      requestHeaders.delete('Authorization')
    }

    let response: Response
    try {
      response = await this.fetcher.call(globalThis, `${this.baseUrl}${path}`, {
        ...requestInit,
        headers: requestHeaders,
        body: body === undefined ? undefined : JSON.stringify(body),
      })
    } catch {
      throw new ApiClientError(genericError(0))
    }

    if (response.status === 401 && auth && allowRefresh && session?.refreshToken) {
      const currentSession = this.sessionStore.read()
      if (
        currentSession?.refreshToken &&
        currentSession.refreshToken !== session.refreshToken
      ) {
        return this.execute<T>(path, options, false)
      }
      await this.refreshSession(session.refreshToken)
      return this.execute<T>(path, options, false)
    }

    if (!response.ok) throw await this.toError(response)
    if (response.status === 204 || response.status === 205) return undefined as T
    if (responseType === 'blob') return (await response.blob()) as T
    if (responseType === 'text') return (await response.text()) as T
    return (await response.json()) as T
  }

  private refreshSession(refreshToken: string): Promise<TokenResponse> {
    if (!this.refreshPromise) {
      this.refreshPromise = this.performRefresh(refreshToken).finally(() => {
        this.refreshPromise = null
      })
    }
    return this.refreshPromise
  }

  private async performRefresh(refreshToken: string): Promise<TokenResponse> {
    try {
      const response = await this.fetcher.call(globalThis, `${this.baseUrl}/api/v1/auth/refresh`, {
        method: 'POST',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      })
      if (!response.ok) throw await this.toError(response)
      const tokens = (await response.json()) as TokenResponse
      this.sessionStore.write(tokens)
      return tokens
    } catch (error) {
      this.sessionStore.clear()
      this.sessionExpiredHandler()
      if (error instanceof ApiClientError) throw error
      throw new ApiClientError(genericError(0))
    }
  }

  private async toError(response: Response): Promise<ApiClientError> {
    try {
      const body = (await response.json()) as Partial<ApiErrorBody>
      return new ApiClientError({
        ...genericError(response.status),
        ...body,
        status: response.status,
        validationErrors: body.validationErrors ?? {},
      })
    } catch {
      return new ApiClientError(genericError(response.status))
    }
  }
}
