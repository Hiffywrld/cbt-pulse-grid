import { render } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { attemptStorage } from '../../lib/storage/attempt-storage'
import { attemptApi } from './attempt-api'
import { useAttemptMonitoring } from './use-attempt-monitoring'

vi.mock('./attempt-api', () => ({ attemptApi: { heartbeat: vi.fn(), monitoringEvents: vi.fn() } }))
vi.mock('../../lib/storage/device-identity', () => ({ getDeviceId: () => 'device-local' }))
vi.mock('../../lib/storage/attempt-storage', () => ({ attemptStorage: {
  nextHeartbeatSequence: vi.fn(),
} }))

const Harness = ({ active }: { active: boolean }) => {
  useAttemptMonitoring('attempt-1', active)
  return null
}

describe('attempt monitoring lifecycle', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true })
    vi.mocked(attemptStorage.nextHeartbeatSequence).mockResolvedValue(3)
    vi.mocked(attemptApi.heartbeat).mockResolvedValue({})
    vi.mocked(attemptApi.monitoringEvents).mockResolvedValue({})
  })

  it('sends a backend-owned heartbeat and removes listeners on cleanup', async () => {
    const removeWindow = vi.spyOn(window, 'removeEventListener')
    const removeDocument = vi.spyOn(document, 'removeEventListener')
    const view = render(<Harness active />)
    await vi.waitFor(() => expect(attemptApi.heartbeat).toHaveBeenCalledWith('attempt-1', expect.objectContaining({
      clientSequence: 3,
      deviceId: 'device-local',
    })))
    view.unmount()
    expect(removeWindow).toHaveBeenCalledWith('blur', expect.any(Function))
    expect(removeWindow).toHaveBeenCalledWith('online', expect.any(Function))
    expect(removeDocument).toHaveBeenCalledWith('copy', expect.any(Function))
  })

  it('keeps supported offline events in memory and batches them after reconnect', async () => {
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: false })
    render(<Harness active />)
    document.dispatchEvent(new Event('copy'))
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true })
    window.dispatchEvent(new Event('online'))
    await vi.waitFor(() => expect(attemptApi.monitoringEvents).toHaveBeenCalledWith(
      'attempt-1',
      expect.any(String),
      expect.arrayContaining([
        expect.objectContaining({ eventType: 'COPY_ATTEMPT' }),
        expect.objectContaining({ eventType: 'NETWORK_RECONNECTED' }),
      ]),
    ))
  })
})
