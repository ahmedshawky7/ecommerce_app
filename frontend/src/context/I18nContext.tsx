import React, { createContext, useContext, useEffect, useState } from 'react';
import { Language, translations } from '../i18n/translations';

interface I18nContextValue {
  language: Language;
  isRtl: boolean;
  locale: string;
  setLanguage: (lang: Language) => void;
  toggleLanguage: () => void;
  t: (key: string, params?: Record<string, string | number>) => string;
}

const STORAGE_KEY = 'novacart_lang';

const I18nContext = createContext<I18nContextValue | undefined>(undefined);

function getInitialLanguage(): Language {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved === 'ar' || saved === 'en') return saved;
  } catch {
    // ignore
  }
  return 'en';
}

function resolveKey(obj: unknown, path: string): string | undefined {
  const parts = path.split('.');
  let current: unknown = obj;
  for (const part of parts) {
    if (current && typeof current === 'object' && part in current) {
      current = (current as Record<string, unknown>)[part];
    } else {
      return undefined;
    }
  }
  return typeof current === 'string' ? current : undefined;
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [language, setLanguageState] = useState<Language>(getInitialLanguage);

  useEffect(() => {
    const root = document.documentElement;
    root.lang = language;
    root.dir = language === 'ar' ? 'rtl' : 'ltr';
    try {
      localStorage.setItem(STORAGE_KEY, language);
    } catch {
      // ignore
    }
  }, [language]);

  const setLanguage = (lang: Language) => setLanguageState(lang);
  const toggleLanguage = () => setLanguageState((prev) => (prev === 'en' ? 'ar' : 'en'));

  const t = (key: string, params?: Record<string, string | number>): string => {
    const raw =
      resolveKey(translations[language], key) ??
      resolveKey(translations.en, key) ??
      key;

    if (!params) return raw;
    return Object.entries(params).reduce(
      (acc, [k, v]) => acc.replace(new RegExp(`\\{\\{${k}\\}\\}`, 'g'), String(v)),
      raw
    );
  };

  return (
    <I18nContext.Provider
      value={{
        language,
        isRtl: language === 'ar',
        locale: language === 'ar' ? 'ar-EG-u-nu-latn' : 'en-US',
        setLanguage,
        toggleLanguage,
        t,
      }}
    >
      {children}
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error('useI18n must be used inside I18nProvider');
  return ctx;
}