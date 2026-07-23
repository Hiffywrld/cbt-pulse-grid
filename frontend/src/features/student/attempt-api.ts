import { apiClient } from '../../lib/api/client'
import type {
  AttemptPackage, AttemptResult, MonitoringEventType, SavedAnswer,
} from '../../types/attempt'

export type AnswerSync = {
  attemptQuestionId: string
  selectedOptionIds: string[]
  clientSequence: number
}

export type SyncAnswersResponse = {
  acknowledgedSyncId: string
  savedAnswers: SavedAnswer[]
  lastSavedAt: string
  status: AttemptPackage['status']
}

export const attemptApi = {
  start: (examId: string, accessPin: string, deviceId: string) =>
    apiClient.request<AttemptPackage>(`/api/v1/student/exams/${examId}/attempts`, {
      method: 'POST',
      body: { accessPin, deviceId },
    }),
  get: (attemptId: string) =>
    apiClient.request<AttemptPackage>(`/api/v1/student/attempts/${attemptId}`),
  syncAnswers: (attemptId: string, syncId: string, answers: AnswerSync[]) =>
    apiClient.request<SyncAnswersResponse>(`/api/v1/student/attempts/${attemptId}/answers`, {
      method: 'PUT',
      body: { syncId, answers },
    }),
  submit: (attemptId: string) =>
    apiClient.request<AttemptResult>(`/api/v1/student/attempts/${attemptId}/submit`, { method: 'POST' }),
  result: (attemptId: string) =>
    apiClient.request<AttemptResult>(`/api/v1/student/attempts/${attemptId}/result`),
  heartbeat: (attemptId: string, body: {
    heartbeatId: string
    clientSequence: number
    clientTimestamp: string
    deviceId: string
    focused: boolean
    fullscreen: boolean
    online: boolean
  }) => apiClient.request(`/api/v1/student/attempts/${attemptId}/heartbeat`, { method: 'POST', body }),
  monitoringEvents: (attemptId: string, syncId: string, events: {
    eventId: string
    eventType: MonitoringEventType
    occurredAt: string
    metadata: Record<string, string>
  }[]) => apiClient.request(`/api/v1/student/attempts/${attemptId}/monitoring-events`, {
    method: 'POST',
    body: { syncId, events },
  }),
}

