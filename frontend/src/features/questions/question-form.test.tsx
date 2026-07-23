import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { QuestionForm } from './question-form'

const subjects = [{
  id: 'subject-1', institutionId: 'institution-1', code: 'CSC-101', name: 'Computing',
  description: null, status: 'ACTIVE' as const, createdAt: '', updatedAt: '', version: 0,
}]

const fillBasics = async () => {
  await userEvent.type(screen.getByLabelText('Question text'), 'Which values are prime?')
  await userEvent.clear(screen.getByLabelText('Marks'))
  await userEvent.type(screen.getByLabelText('Marks'), '2')
  await userEvent.type(screen.getByLabelText('Option 1'), '2')
  await userEvent.type(screen.getByLabelText('Option 2'), '4')
}

describe('QuestionForm', () => {
  it('enforces exactly one correct answer for single choice', async () => {
    const submit = vi.fn()
    render(<QuestionForm subjects={subjects} onSubmit={submit} onCancel={vi.fn()} />)
    await fillBasics()
    await userEvent.click(screen.getByRole('button', { name: 'Create draft' }))
    expect(await screen.findByText('Choose exactly one correct answer')).toBeVisible()
    expect(submit).not.toHaveBeenCalled()
  })

  it('enforces at least two correct answers for multiple choice', async () => {
    render(<QuestionForm subjects={subjects} onSubmit={vi.fn()} onCancel={vi.fn()} />)
    await userEvent.selectOptions(screen.getByLabelText('Question type'), 'MULTIPLE_CHOICE')
    await fillBasics()
    await userEvent.click(screen.getAllByRole('checkbox', { name: 'Correct' })[0])
    await userEvent.click(screen.getByRole('button', { name: 'Create draft' }))
    expect(await screen.findByText('Choose at least two correct answers')).toBeVisible()
  })

  it('submits ordered options for a valid true/false question', async () => {
    const submit = vi.fn().mockResolvedValue(undefined)
    render(<QuestionForm subjects={subjects} onSubmit={submit} onCancel={vi.fn()} />)
    await userEvent.selectOptions(screen.getByLabelText('Question type'), 'TRUE_FALSE')
    await userEvent.type(screen.getByLabelText('Question text'), 'The earth is round.')
    await userEvent.click(screen.getByRole('button', { name: 'Create draft' }))
    await waitFor(() => expect(submit).toHaveBeenCalledWith(expect.objectContaining({
      type: 'TRUE_FALSE',
      options: [
        { optionText: 'True', correct: true, displayOrder: 1 },
        { optionText: 'False', correct: false, displayOrder: 2 },
      ],
    })))
  })
})
