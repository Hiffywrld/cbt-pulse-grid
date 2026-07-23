import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiClientError } from '../../lib/api/api-error'
import { PinForm } from './exam-detail-page'

describe('PinForm privacy', () => {
  it('submits once, clears the PIN, and explains that it cannot be retrieved', async () => {
    const submit = vi.fn().mockResolvedValue(undefined)
    const success = vi.fn()
    render(<PinForm onSubmit={submit} onSuccess={success} onCancel={vi.fn()} />)
    const input = screen.getByLabelText('New six-digit access PIN')
    await userEvent.type(input, '654321')
    await userEvent.click(screen.getByRole('button', { name: 'Rotate PIN' }))

    await waitFor(() => expect(submit).toHaveBeenCalledWith('654321'))
    await waitFor(() => expect(input).toHaveValue(''))
    expect(success).toHaveBeenCalledOnce()
    expect(screen.getByText(/stores only a hash/i)).toBeVisible()
    expect(screen.getByText(/cannot be retrieved or displayed later/i)).toBeVisible()
    expect(window.localStorage.length).toBe(0)
    expect(window.sessionStorage.length).toBe(0)
  })

  it('shows a safe backend error and does not report success', async () => {
    const submit = vi.fn().mockRejectedValue(new ApiClientError({
      status: 400,
      error: 'Bad Request',
      message: 'Only DRAFT exams may rotate an access PIN',
      validationErrors: {},
    }))
    const success = vi.fn()
    render(<PinForm onSubmit={submit} onSuccess={success} onCancel={vi.fn()} />)
    await userEvent.type(screen.getByLabelText('New six-digit access PIN'), '654321')
    await userEvent.click(screen.getByRole('button', { name: 'Rotate PIN' }))

    expect(await screen.findByText('Only DRAFT exams may rotate an access PIN')).toBeVisible()
    expect(success).not.toHaveBeenCalled()
  })
})
