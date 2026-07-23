import { openDB, type DBSchema } from 'idb'
import type { QueuedAnswer } from '../../types/attempt'

type AttemptMeta = {
  attemptId: string
  examId: string
  heartbeatSequence: number
  updatedAt: string
}

type StoredAnswerSyncBatch = {
  syncId: string
  attemptId: string
  answers: QueuedAnswer[]
  createdAt: string
}

interface AttemptDatabase extends DBSchema {
  attempts: {
    key: string
    value: AttemptMeta
    indexes: { 'by-exam': string }
  }
  answers: {
    key: string
    value: QueuedAnswer
    indexes: { 'by-attempt': string }
  }
  answerSyncBatches: {
    key: string
    value: StoredAnswerSyncBatch
    indexes: { 'by-attempt': string }
  }
}

const database = () => openDB<AttemptDatabase>('cbt-pulse-grid-attempts', 2, {
  upgrade(db) {
    if (!db.objectStoreNames.contains('attempts')) {
      const attempts = db.createObjectStore('attempts', { keyPath: 'attemptId' })
      attempts.createIndex('by-exam', 'examId', { unique: true })
    }
    if (!db.objectStoreNames.contains('answers')) {
      const answers = db.createObjectStore('answers', { keyPath: 'key' })
      answers.createIndex('by-attempt', 'attemptId')
    }
    if (!db.objectStoreNames.contains('answerSyncBatches')) {
      const batches = db.createObjectStore('answerSyncBatches', { keyPath: 'syncId' })
      batches.createIndex('by-attempt', 'attemptId', { unique: true })
    }
  },
})

const answerKey = (attemptId: string, questionId: string) => `${attemptId}:${questionId}`
export const shouldReplaceQueuedAnswer = (currentSequence: number | undefined, nextSequence: number) =>
  currentSequence === undefined || nextSequence > currentSequence

export const attemptStorage = {
  async rememberAttempt(attemptId: string, examId: string) {
    const db = await database()
    const current = await db.get('attempts', attemptId)
    await db.put('attempts', {
      attemptId,
      examId,
      heartbeatSequence: current?.heartbeatSequence ?? 0,
      updatedAt: new Date().toISOString(),
    })
  },
  async attemptForExam(examId: string) {
    const db = await database()
    return (await db.getFromIndex('attempts', 'by-exam', examId))?.attemptId ?? null
  },
  async queueAnswer(answer: QueuedAnswer) {
    const db = await database()
    const key = answerKey(answer.attemptId, answer.attemptQuestionId)
    const current = await db.get('answers', key)
    if (!shouldReplaceQueuedAnswer(current?.clientSequence, answer.clientSequence)) return false
    await db.put('answers', { ...answer, key } as QueuedAnswer & { key: string })
    return true
  },
  async answers(attemptId: string) {
    const db = await database()
    return db.getAllFromIndex('answers', 'by-attempt', attemptId)
  },
  async answerBatch(attemptId: string) {
    const db = await database()
    const existing = await db.getFromIndex('answerSyncBatches', 'by-attempt', attemptId)
    if (existing) return existing
    const answers = await db.getAllFromIndex('answers', 'by-attempt', attemptId)
    if (!answers.length) return null
    const batch: StoredAnswerSyncBatch = {
      syncId: crypto.randomUUID(),
      attemptId,
      answers,
      createdAt: new Date().toISOString(),
    }
    await db.add('answerSyncBatches', batch)
    return batch
  },
  async acknowledgeAnswerBatch(syncId: string) {
    const db = await database()
    await db.delete('answerSyncBatches', syncId)
  },
  async acknowledgeAnswers(attemptId: string, sequences: Map<string, number>) {
    const db = await database()
    const transaction = db.transaction('answers', 'readwrite')
    const queued = await transaction.store.index('by-attempt').getAll(attemptId)
    await Promise.all(queued.map((answer) => {
      const acknowledged = sequences.get(answer.attemptQuestionId)
      return acknowledged !== undefined && acknowledged >= answer.clientSequence
        ? transaction.store.delete(answerKey(attemptId, answer.attemptQuestionId))
        : Promise.resolve()
    }))
    await transaction.done
  },
  async nextHeartbeatSequence(attemptId: string) {
    const db = await database()
    const transaction = db.transaction('attempts', 'readwrite')
    const current = await transaction.store.get(attemptId)
    if (!current) throw new Error('Attempt storage is unavailable')
    const sequence = current.heartbeatSequence + 1
    await transaction.store.put({ ...current, heartbeatSequence: sequence, updatedAt: new Date().toISOString() })
    await transaction.done
    return sequence
  },
  async clearAttempt(attemptId: string) {
    const db = await database()
    const transaction = db.transaction(['attempts', 'answers', 'answerSyncBatches'], 'readwrite')
    const answers = await transaction.objectStore('answers').index('by-attempt').getAllKeys(attemptId)
    const batches = await transaction.objectStore('answerSyncBatches').index('by-attempt').getAllKeys(attemptId)
    await Promise.all([
      transaction.objectStore('attempts').delete(attemptId),
      ...answers.map((key) => transaction.objectStore('answers').delete(key)),
      ...batches.map((key) => transaction.objectStore('answerSyncBatches').delete(key)),
    ])
    await transaction.done
  },
}
