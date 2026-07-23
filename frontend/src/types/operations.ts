import type { PageResponse } from './management'

export type Connectivity = 'UNKNOWN' | 'ONLINE' | 'OFFLINE'
export type MonitoringEventType =
  | 'TAB_HIDDEN' | 'WINDOW_BLUR' | 'FULLSCREEN_EXIT' | 'COPY_ATTEMPT'
  | 'PASTE_ATTEMPT' | 'DEVTOOLS_SUSPECTED' | 'NETWORK_DISCONNECTED'
  | 'NETWORK_RECONNECTED' | 'HEARTBEAT_MISSED'

export type MonitoringRow = {
  attemptId: string; candidateId: string; firstName: string; lastName: string
  registrationNumber: string | null; candidateStatus: string; attemptStatus: string
  lastHeartbeatAt: string | null; connectivity: Connectivity; focused: boolean | null
  fullscreen: boolean | null; eventCount: number; riskScore: number
}
export type MonitoringEvent = {
  id: string; clientEventId: string; eventType: MonitoringEventType; occurredAt: string
  receivedAt: string; metadata: Record<string, string>; riskWeight: number; riskPointsApplied: number
}
export type LiveMonitoringUpdate = {
  attemptId: string; candidateId: string; candidateFirstName: string; candidateLastName: string
  registrationNumber: string | null; candidateStatus: string; attemptStatus: string
  lastHeartbeat: string | null; connectivity: Connectivity; focused: boolean | null
  fullscreen: boolean | null; eventCount: number; riskScore: number
  updateType: 'HEARTBEAT' | 'MONITORING_EVENTS' | 'HEARTBEAT_MISSED' | 'HEARTBEAT_RESTORED'
  serverTimestamp: string
}

export const auditActions = [
  'INSTITUTION_CREATED', 'INSTITUTION_UPDATED', 'INSTITUTION_STATUS_CHANGED',
  'USER_CREATED', 'USER_UPDATED', 'USER_STATUS_CHANGED', 'SUBJECT_CREATED',
  'SUBJECT_UPDATED', 'SUBJECT_STATUS_CHANGED', 'QUESTION_CREATED', 'QUESTION_UPDATED',
  'QUESTION_STATUS_CHANGED', 'QUESTION_PUBLISHED', 'EXAM_CREATED', 'EXAM_UPDATED',
  'EXAM_PUBLISHED', 'EXAM_CANCELLED', 'EXAM_CLOSED', 'EXAM_ACCESS_PIN_ROTATED',
  'EXAM_CANDIDATES_ASSIGNED', 'EXAM_CANDIDATE_REMOVED', 'ATTEMPT_SUBMITTED',
  'ATTEMPT_AUTO_SUBMITTED', 'WEBHOOK_SUBSCRIPTION_CREATED',
  'WEBHOOK_SUBSCRIPTION_STATUS_CHANGED', 'WEBHOOK_SECRET_ROTATED',
  'WEBHOOK_DELIVERY_RETRIED', 'MONITORING_EVENTS_RECORDED', 'HEARTBEAT_MISSED_RECORDED',
] as const
export const auditResourceTypes = [
  'INSTITUTION', 'USER', 'SUBJECT', 'QUESTION', 'EXAM', 'EXAM_CANDIDATE',
  'ATTEMPT', 'WEBHOOK_SUBSCRIPTION', 'WEBHOOK_DELIVERY', 'MONITORING_STATE',
] as const
export type AuditEvent = {
  id: string; institutionId: string; actorId: string | null; actorRoles: string
  action: typeof auditActions[number]; resourceType: typeof auditResourceTypes[number]
  resourceId: string | null; outcome: 'SUCCESS' | 'FAILURE'; occurredAt: string
  requestId: string; metadata: Record<string, unknown>
}

export type WebhookSubscription = {
  id: string; institutionId: string; name: string; destinationUrl: string
  status: 'ACTIVE' | 'PAUSED'; allEventTypes: boolean; eventTypes: MonitoringEventType[]
  secretVersion: number; createdBy: string; updatedBy: string; createdAt: string
  updatedAt: string; version: number
}
export type WebhookSecret = { subscription: WebhookSubscription; secret: string }
export type WebhookDeliveryStatus = 'PENDING' | 'IN_FLIGHT' | 'SUCCEEDED' | 'FAILED' | 'DEAD_LETTER'
export type WebhookDelivery = {
  id: string; subscriptionId: string; eventId: string; eventType: MonitoringEventType
  status: WebhookDeliveryStatus; attemptCount: number; nextAttemptAt: string | null
  responseStatus: number | null; failureReason: string | null; deliveredAt: string | null
  createdAt: string; updatedAt: string; version: number
}

export type OperationsPage<T> = PageResponse<T>
