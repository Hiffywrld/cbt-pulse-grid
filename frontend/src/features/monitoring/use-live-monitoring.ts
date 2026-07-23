import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { authApi } from '../auth/auth-api'
import { browserSessionStore } from '../../lib/storage/session-storage'
import { environment } from '../../lib/env'
import type { LiveMonitoringUpdate, MonitoringRow, OperationsPage } from '../../types/operations'

export type LiveConnection = 'connecting' | 'connected' | 'reconnecting' | 'offline'

export const applyLiveUpdate = (
  page: OperationsPage<MonitoringRow> | undefined,
  update: LiveMonitoringUpdate,
): OperationsPage<MonitoringRow> | undefined => page && ({
  ...page,
  content: page.content.map((row) => row.attemptId === update.attemptId ? {
    ...row,
    candidateId: update.candidateId,
    firstName: update.candidateFirstName,
    lastName: update.candidateLastName,
    registrationNumber: update.registrationNumber,
    candidateStatus: update.candidateStatus,
    attemptStatus: update.attemptStatus,
    lastHeartbeatAt: update.lastHeartbeat,
    connectivity: update.connectivity,
    focused: update.focused,
    fullscreen: update.fullscreen,
    eventCount: update.eventCount,
    riskScore: update.riskScore,
  } : row),
})

export const useLiveMonitoring = (examId: string) => {
  const queryClient = useQueryClient()
  const [connection, setConnection] = useState<LiveConnection>(navigator.onLine ? 'connecting' : 'offline')
  const latest = useRef(new Map<string, string>())

  useEffect(() => {
    if (!examId) return
    let subscription: StompSubscription | undefined
    let stopped = false
    const client = new Client({
      brokerURL: environment.websocketUrl,
      reconnectDelay: 5_000,
      debug: () => undefined,
      beforeConnect: async () => {
        setConnection(navigator.onLine ? 'connecting' : 'offline')
        await authApi.me()
        const token = browserSessionStore.read()?.accessToken
        if (!token) throw new Error('Authenticated session is unavailable')
        client.connectHeaders = { Authorization: `Bearer ${token}` }
      },
      onConnect: () => {
        if (stopped) return
        setConnection('connected')
        subscription = client.subscribe(`/topic/exams/${examId}/monitoring`, (message: IMessage) => {
          let update: LiveMonitoringUpdate
          try { update = JSON.parse(message.body) as LiveMonitoringUpdate } catch { return }
          const prior = latest.current.get(update.attemptId)
          if (prior && prior >= update.serverTimestamp) return
          latest.current.set(update.attemptId, update.serverTimestamp)
          queryClient.setQueriesData<OperationsPage<MonitoringRow>>(
            { queryKey: ['monitoring', 'dashboard', examId] },
            (page) => applyLiveUpdate(page, update),
          )
        })
      },
      onWebSocketClose: () => {
        if (!stopped) {
          setConnection(navigator.onLine ? 'reconnecting' : 'offline')
          void queryClient.invalidateQueries({ queryKey: ['monitoring', 'dashboard', examId] })
        }
      },
      onStompError: () => setConnection('reconnecting'),
    })
    client.activate()
    return () => {
      stopped = true
      subscription?.unsubscribe()
      void client.deactivate()
    }
  }, [examId, queryClient])

  useEffect(() => {
    if (!examId || connection === 'connected') return
    const timer = window.setInterval(() => {
      void queryClient.invalidateQueries({ queryKey: ['monitoring', 'dashboard', examId] })
    }, 15_000)
    return () => window.clearInterval(timer)
  }, [connection, examId, queryClient])

  return connection
}
