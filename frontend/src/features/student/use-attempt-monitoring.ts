import { useCallback, useEffect, useRef } from 'react'
import { attemptStorage } from '../../lib/storage/attempt-storage'
import { getDeviceId } from '../../lib/storage/device-identity'
import type { MonitoringEventType } from '../../types/attempt'
import { attemptApi } from './attempt-api'

export const useAttemptMonitoring = (attemptId: string, active: boolean) => {
  const mounted = useRef(true)
  const syncing = useRef(false)
  const queuedEvents = useRef<Array<{
    eventId: string
    eventType: MonitoringEventType
    occurredAt: string
  }>>([])

  const flushEvents = useCallback(async () => {
    if (!active || !navigator.onLine || syncing.current) return
    const queued = [...queuedEvents.current]
    if (!queued.length) return
    syncing.current = true
    try {
      await attemptApi.monitoringEvents(attemptId, crypto.randomUUID(), queued.map((event) => ({
        eventId: event.eventId,
        eventType: event.eventType,
        occurredAt: event.occurredAt,
        metadata: {},
      })))
      if (mounted.current) {
        const acknowledged = new Set(queued.map((event) => event.eventId))
        queuedEvents.current = queuedEvents.current.filter((event) => !acknowledged.has(event.eventId))
      }
    } catch {
      // Offline-first evidence remains queued without exposing transport details.
    } finally {
      syncing.current = false
    }
  }, [active, attemptId])

  const record = useCallback(async (eventType: MonitoringEventType) => {
    if (!active) return
    queuedEvents.current.push({
      eventId: crypto.randomUUID(),
      eventType,
      occurredAt: new Date().toISOString(),
    })
    await flushEvents()
  }, [active, flushEvents])

  const heartbeat = useCallback(async () => {
    if (!active || !navigator.onLine) return
    try {
      const sequence = await attemptStorage.nextHeartbeatSequence(attemptId)
      await attemptApi.heartbeat(attemptId, {
        heartbeatId: crypto.randomUUID(),
        clientSequence: sequence,
        clientTimestamp: new Date().toISOString(),
        deviceId: getDeviceId(),
        focused: document.hasFocus(),
        fullscreen: Boolean(document.fullscreenElement),
        online: navigator.onLine,
      })
    } catch {
      // A later heartbeat safely supersedes this one by client sequence.
    }
  }, [active, attemptId])

  useEffect(() => {
    mounted.current = true
    if (!active) return
    void heartbeat()
    void flushEvents()
    const heartbeatTimer = window.setInterval(() => void heartbeat(), 15_000)
    const hidden = () => { if (document.hidden) void record('TAB_HIDDEN') }
    const blurred = () => void record('WINDOW_BLUR')
    const fullscreen = () => { if (!document.fullscreenElement) void record('FULLSCREEN_EXIT') }
    const copied = () => void record('COPY_ATTEMPT')
    const pasted = () => void record('PASTE_ATTEMPT')
    const offline = () => void record('NETWORK_DISCONNECTED')
    const online = () => { void record('NETWORK_RECONNECTED'); void heartbeat(); void flushEvents() }
    document.addEventListener('visibilitychange', hidden)
    window.addEventListener('blur', blurred)
    document.addEventListener('fullscreenchange', fullscreen)
    document.addEventListener('copy', copied)
    document.addEventListener('paste', pasted)
    window.addEventListener('offline', offline)
    window.addEventListener('online', online)
    return () => {
      mounted.current = false
      window.clearInterval(heartbeatTimer)
      document.removeEventListener('visibilitychange', hidden)
      window.removeEventListener('blur', blurred)
      document.removeEventListener('fullscreenchange', fullscreen)
      document.removeEventListener('copy', copied)
      document.removeEventListener('paste', pasted)
      window.removeEventListener('offline', offline)
      window.removeEventListener('online', online)
    }
  }, [active, flushEvents, heartbeat, record])
}
