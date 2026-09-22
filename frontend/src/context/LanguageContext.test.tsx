import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { LanguageProvider, useLanguage } from './LanguageContext'

function LanguageExample() {
  const { language, setLanguage, text } = useLanguage()
  return <><p>{text('My care', 'मेरी देखभाल')}</p><button onClick={() => setLanguage('hi')}>{language}</button></>
}

describe('LanguageProvider', () => {
  beforeEach(() => window.localStorage.clear())

  it('switches to Hindi and remembers the selection', () => {
    render(<LanguageProvider><LanguageExample /></LanguageProvider>)
    fireEvent.click(screen.getByRole('button', { name: 'en' }))
    expect(screen.getByText('मेरी देखभाल')).toBeInTheDocument()
    expect(window.localStorage.getItem('smartcare-language')).toBe('hi')
    expect(document.documentElement.lang).toBe('hi')
  })
})
