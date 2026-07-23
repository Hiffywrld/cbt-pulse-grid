import { render, screen } from '@testing-library/react'
import { fireEvent } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PasswordInput } from './password-input'

describe('PasswordInput', () => {
  it('keeps the field labelled and toggles password visibility accessibly', async () => {
    render(<PasswordInput label="Account password" name="password" />)
    const input = screen.getByLabelText('Account password')
    expect(input).toHaveAttribute('type', 'password')
    fireEvent.click(screen.getByRole('button', { name: 'Show password' }))
    expect(input).toHaveAttribute('type', 'text')
    expect(screen.getByRole('button', { name: 'Hide password' })).toBeInTheDocument()
  })
})
