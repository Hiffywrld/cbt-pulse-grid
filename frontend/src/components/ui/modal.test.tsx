import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Button } from './button'
import { Modal } from './modal'

const LongDialog = ({ onClose = vi.fn() }: { onClose?: () => void }) => <Modal open title="Long examination form" size="wide" onClose={onClose}>
  <form>
    {Array.from({ length: 8 }, (_, index) => <label key={index}>Option {index + 1}<input /></label>)}
    <div className="modal-actions"><Button type="button">Cancel</Button><Button type="submit">Save examination</Button></div>
  </form>
</Modal>

afterEach(() => {
  document.documentElement.removeAttribute('data-theme')
  document.body.style.overflow = ''
  document.body.style.paddingRight = ''
})

describe('Modal viewport and accessibility foundation', () => {
  it('keeps its header outside the scroll region and actions keyboard reachable', async () => {
    render(<LongDialog />)
    const dialog = screen.getByRole('dialog', { name: 'Long examination form' })
    const scrollRegion = dialog.querySelector('[data-scroll-region="true"]')
    expect(dialog).toHaveClass('modal--wide')
    expect(scrollRegion).toBeInTheDocument()
    expect(within(dialog).getByRole('heading', { name: 'Long examination form' })).not.toBe(scrollRegion)
    expect(within(scrollRegion as HTMLElement).getByRole('button', { name: 'Save examination' })).toBeVisible()

    const close = within(dialog).getByRole('button', { name: 'Close dialog' })
    expect(close).toHaveFocus()
    await userEvent.tab({ shift: true })
    expect(within(dialog).getByRole('button', { name: 'Save examination' })).toHaveFocus()
  })

  it('locks background scrolling and restores the prior page state and focus', async () => {
    const Trigger = () => {
      const [open, setOpen] = useState(false)
      return <><button onClick={() => setOpen(true)}>Open dialog</button><Modal open={open} title="Scroll lock" onClose={() => setOpen(false)}><button>Inside</button></Modal></>
    }
    document.body.style.overflow = 'scroll'
    render(<Trigger />)
    const trigger = screen.getByRole('button', { name: 'Open dialog' })
    trigger.focus()
    await userEvent.click(trigger)
    expect(document.body.style.overflow).toBe('hidden')
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(document.body.style.overflow).toBe('scroll')
    expect(trigger).toHaveFocus()
  })

  it.each(['light', 'dark'] as const)('remains accessible in %s mode at a short mobile viewport', (theme) => {
    document.documentElement.dataset.theme = theme
    Object.defineProperty(window, 'innerHeight', { configurable: true, value: 480 })
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 390 })
    render(<LongDialog />)
    const dialog = screen.getByRole('dialog', { name: 'Long examination form' })
    expect(dialog).toBeVisible()
    expect(dialog.querySelector('.modal__body')).toHaveAttribute('data-scroll-region', 'true')
    expect(screen.getByRole('button', { name: 'Save examination' })).toBeVisible()
  })
})
