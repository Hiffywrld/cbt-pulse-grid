import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '../lib/api/client'
import { institutionsApi } from './institutions/institutions-api'
import { studentExamsApi } from './student/student-exams-api'
import { usersApi } from './users/users-api'

describe('Phase 2 backend API contracts', () => {
  beforeEach(() => vi.spyOn(apiClient, 'request').mockResolvedValue({}))

  it('uses the institution list and lifecycle endpoints exactly', async () => {
    await institutionsApi.list({ search: 'niit', status: 'ACTIVE', page: 1, size: 20 })
    await institutionsApi.create({ name: 'NIIT Lagos Campus', code: 'NIIT-LAGOS' })
    await institutionsApi.update('institution-1', { name: 'NIIT Lagos Campus' })
    await institutionsApi.changeStatus('institution-1', 'SUSPENDED')

    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/institutions?search=niit&status=ACTIVE&page=1&size=20')
    expect(apiClient.request).toHaveBeenNthCalledWith(2, '/api/v1/institutions', { method: 'POST', body: { name: 'NIIT Lagos Campus', code: 'NIIT-LAGOS' } })
    expect(apiClient.request).toHaveBeenNthCalledWith(3, '/api/v1/institutions/institution-1', { method: 'PUT', body: { name: 'NIIT Lagos Campus' } })
    expect(apiClient.request).toHaveBeenNthCalledWith(4, '/api/v1/institutions/institution-1/status', { method: 'PATCH', body: { status: 'SUSPENDED' } })
  })

  it('uses tenant-safe user list, creation, update and status endpoints', async () => {
    await usersApi.list({ search: 'amina', role: 'STUDENT', status: 'ACTIVE', page: 0, size: 20 })
    await usersApi.create({ firstName: 'Amina', lastName: 'Okafor', email: 'amina@example.edu', password: 'TestPassword1!', roles: ['STUDENT'], registrationNumber: 'REG-1' })
    await usersApi.update('user-1', { firstName: 'Amina', lastName: 'Okafor', registrationNumber: 'REG-1' })
    await usersApi.changeStatus('user-1', 'INACTIVE')

    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/users?search=amina&role=STUDENT&status=ACTIVE&page=0&size=20')
    expect(apiClient.request).toHaveBeenNthCalledWith(2, '/api/v1/users', expect.objectContaining({ method: 'POST' }))
    expect(apiClient.request).toHaveBeenNthCalledWith(3, '/api/v1/users/user-1', expect.objectContaining({ method: 'PUT' }))
    expect(apiClient.request).toHaveBeenNthCalledWith(4, '/api/v1/users/user-1/status', { method: 'PATCH', body: { status: 'INACTIVE' } })
  })

  it('uses only candidate-safe assigned exam endpoints', async () => {
    await studentExamsApi.list()
    await studentExamsApi.get('exam-1')
    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/student/exams')
    expect(apiClient.request).toHaveBeenNthCalledWith(2, '/api/v1/student/exams/exam-1')
  })
})
