import { environment } from '../env'
import { browserSessionStore } from '../storage/session-storage'
import { ApiClient } from './api-client'

export const apiClient = new ApiClient(environment.apiBaseUrl, browserSessionStore)
