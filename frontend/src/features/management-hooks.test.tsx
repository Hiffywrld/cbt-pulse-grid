import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useInstitutionMutations } from './institutions/institution-hooks'
import { institutionsApi } from './institutions/institutions-api'
import { useUserMutations } from './users/user-hooks'
import { usersApi } from './users/users-api'

vi.mock('./institutions/institutions-api', () => ({
  institutionsApi: {
    create: vi.fn(),
    update: vi.fn(),
    changeStatus: vi.fn(),
  },
}))
vi.mock('./users/users-api', () => ({
  usersApi: {
    create: vi.fn(),
    update: vi.fn(),
    changeStatus: vi.fn(),
  },
}))

describe('management mutation refresh', () => {
  beforeEach(() => vi.clearAllMocks())

  it('refreshes institution lists after successful creation', async () => {
    vi.mocked(institutionsApi.create).mockResolvedValue({} as never)
    const client = new QueryClient()
    const invalidate = vi.spyOn(client, 'invalidateQueries')
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    )
    const { result } = renderHook(() => useInstitutionMutations(), { wrapper })

    await act(() => result.current.create.mutateAsync({ name: 'NIIT Lagos Campus', code: 'NIIT-LAGOS' }))

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['institutions'] })
  })

  it('refreshes user lists after successful account creation', async () => {
    vi.mocked(usersApi.create).mockResolvedValue({} as never)
    const client = new QueryClient()
    const invalidate = vi.spyOn(client, 'invalidateQueries')
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    )
    const { result } = renderHook(() => useUserMutations(), { wrapper })

    await act(() => result.current.create.mutateAsync({
      firstName: 'Amina',
      lastName: 'Okafor',
      email: 'amina@niitlagos.local',
      password: 'Secure123!',
      institutionId: 'institution-1',
      roles: ['INSTITUTION_ADMIN'],
    }))

    expect(invalidate).toHaveBeenCalledWith({ queryKey: ['users'] })
  })
})
