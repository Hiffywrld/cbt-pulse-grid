const DEVICE_KEY = 'cbt-pulse-grid.device-id'

export const getDeviceId = () => {
  const existing = localStorage.getItem(DEVICE_KEY)
  if (existing) return existing
  const deviceId = crypto.randomUUID()
  localStorage.setItem(DEVICE_KEY, deviceId)
  return deviceId
}

