import { act, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ConnectivityBanner } from './connectivity-banner'

describe('ConnectivityBanner', () => {
  it('appears non-blockingly when the browser goes offline', () => {
    Object.defineProperty(navigator, 'onLine', { configurable: true, value: true })
    render(<ConnectivityBanner />)
    expect(screen.queryByText(/you are offline/i)).not.toBeInTheDocument()
    act(() => window.dispatchEvent(new Event('offline')))
    expect(screen.getByRole('status')).toHaveTextContent('You are offline')
  })
})
