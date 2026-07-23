import { WifiOff } from 'lucide-react'
import { useEffect, useState } from 'react'

export const ConnectivityBanner = () => {
  const [online, setOnline] = useState(() => navigator.onLine)
  useEffect(() => {
    const markOnline = () => setOnline(true)
    const markOffline = () => setOnline(false)
    window.addEventListener('online', markOnline)
    window.addEventListener('offline', markOffline)
    return () => {
      window.removeEventListener('online', markOnline)
      window.removeEventListener('offline', markOffline)
    }
  }, [])
  if (online) return null
  return <div className="connectivity-banner" role="status"><WifiOff size={17} aria-hidden="true" />You are offline. Saved local work will synchronize when the server is reachable.</div>
}
