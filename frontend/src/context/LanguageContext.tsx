import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

export type AppLanguage = 'en' | 'hi'

const storageKey = 'smartcare-language'

type LanguageContextValue = {
  language: AppLanguage
  setLanguage: (language: AppLanguage) => void
  text: (english: string, hindi: string) => string
}

const LanguageContext = createContext<LanguageContextValue>({
  language: 'en',
  setLanguage: () => undefined,
  text: (english) => english,
})

function initialLanguage(): AppLanguage {
  if (typeof window === 'undefined') return 'en'
  return window.localStorage.getItem(storageKey) === 'hi' ? 'hi' : 'en'
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguage] = useState<AppLanguage>(initialLanguage)

  useEffect(() => {
    window.localStorage.setItem(storageKey, language)
    document.documentElement.lang = language === 'hi' ? 'hi' : 'en'
  }, [language])

  const value = useMemo<LanguageContextValue>(() => ({
    language,
    setLanguage,
    text: (english, hindi) => language === 'hi' ? hindi : english,
  }), [language])

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export function useLanguage() {
  return useContext(LanguageContext)
}
