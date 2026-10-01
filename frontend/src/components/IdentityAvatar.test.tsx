import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { IdentityAvatar } from './IdentityAvatar'
afterEach(cleanup)
it('falls back to initials for a broken photo and retries a changed photo URL', () => {
  const { rerender } = render(<IdentityAvatar name="Anita Rao" imageUrl="/missing.png" />)
  fireEvent.error(screen.getByRole('img', { name: 'Anita Rao profile' }))
  expect(screen.getByLabelText('Anita Rao avatar')).toHaveTextContent('AR')
  rerender(<IdentityAvatar name="Anita Rao" imageUrl="/replacement.png" />)
  expect(screen.getByRole('img', { name: 'Anita Rao profile' })).toHaveAttribute('src', '/replacement.png')
})
it('uses a neutral fallback when identity data is unavailable', () => {
  render(<IdentityAvatar />)
  expect(screen.getByLabelText('Private profile avatar')).toBeInTheDocument()
  expect(screen.queryByRole('img')).not.toBeInTheDocument()
})
it('normalizes recorded gender and handles unexpected values without guessing from names', () => {
  const { rerender } = render(<IdentityAvatar name="Anita Rao" gender="female" />)
  expect(screen.getByLabelText('Anita Rao avatar')).toHaveClass('text-violet-800')
  rerender(<IdentityAvatar name="Anita Rao" gender="not-recorded" />)
  expect(screen.getByLabelText('Anita Rao avatar')).toHaveClass('text-slate-700')
  rerender(<IdentityAvatar name={null} gender={null} />)
  expect(screen.getByLabelText('Private profile avatar')).toHaveClass('text-slate-700')
})
