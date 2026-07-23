import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ExamForm } from './exam-form'

const subjects = [{
  id: 'subject-1', institutionId: 'institution-1', code: 'CSC-101', name: 'Computing',
  description: null, status: 'ACTIVE' as const, createdAt: '', updatedAt: '', version: 0,
}]

const fillExam = async () => {
  await userEvent.type(screen.getByLabelText('Exam code'), 'csc-mid')
  await userEvent.type(screen.getByLabelText('Exam title'), 'Computing Midterm')
  await userEvent.clear(screen.getByLabelText('Starts at'))
  await userEvent.type(screen.getByLabelText('Starts at'), '2026-08-01T09:00')
  await userEvent.clear(screen.getByLabelText('Ends at'))
  await userEvent.type(screen.getByLabelText('Ends at'), '2026-08-01T11:00')
}

describe('ExamForm', () => {
  it('requires a six-digit PIN for creation', async () => {
    render(<ExamForm subjects={subjects} onSubmit={vi.fn()} onCancel={vi.fn()} />)
    await fillExam()
    await userEvent.type(screen.getByLabelText('Six-digit access PIN'), '123')
    await userEvent.click(screen.getByRole('button', { name: 'Create draft exam' }))
    expect(await screen.findByText('Enter exactly six digits')).toBeVisible()
  })

  it('rejects duplicate pool difficulties', async () => {
    render(<ExamForm subjects={subjects} onSubmit={vi.fn()} onCancel={vi.fn()} />)
    await fillExam()
    await userEvent.type(screen.getByLabelText('Six-digit access PIN'), '123456')
    await userEvent.click(screen.getByRole('button', { name: 'Add pool rule' }))
    await userEvent.selectOptions(screen.getByLabelText('Difficulty 2'), 'MEDIUM')
    await userEvent.click(screen.getByRole('button', { name: 'Create draft exam' }))
    expect(await screen.findByText('Each difficulty can appear only once')).toBeVisible()
  })

  it('submits ISO timestamps, pool rules and never invents tenant fields', async () => {
    const submit = vi.fn().mockResolvedValue(undefined)
    render(<ExamForm subjects={subjects} onSubmit={submit} onCancel={vi.fn()} />)
    await fillExam()
    await userEvent.type(screen.getByLabelText('Six-digit access PIN'), '123456')
    await userEvent.click(screen.getByRole('button', { name: 'Create draft exam' }))
    await waitFor(() => expect(submit).toHaveBeenCalled())
    const body = submit.mock.calls[0][0]
    expect(body).toEqual(expect.objectContaining({
      code: 'csc-mid', subjectId: 'subject-1', accessPin: '123456', passMarkPercentage: 50,
      poolRules: [{ difficulty: 'MEDIUM', questionCount: 10, marksPerQuestion: 1 }],
    }))
    expect(body.startsAt).toMatch(/Z$/)
    expect(body).not.toHaveProperty('institutionId')
  })
})
