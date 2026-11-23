import { Coins, Languages, Moon, Sun } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';
import { useI18n } from '../../context/I18nContext';
import { useCurrency } from '../../context/CurrencyContext';
import { CURRENCIES, CurrencyCode } from '../../lib/utils';

export function ThemeAndLangControls({ showCurrency = true }: { showCurrency?: boolean }) {
  const { isDark, toggleTheme } = useTheme();
  const { language, toggleLanguage, t } = useI18n();
  const { displayCurrency, setDisplayCurrency } = useCurrency();

  return (
    <div className="flex items-center gap-2">
      {showCurrency && (
        <div
          title={t('nav.displayCurrencyLabel')}
          className="inline-flex items-center rounded-xl border border-line bg-surface px-2.5 py-1.5 transition-colors hover:border-line-strong"
        >
          <Coins className="me-1.5 h-3.5 w-3.5 shrink-0 text-success" />
          <select
            aria-label={t('nav.displayCurrencyLabel')}
            value={displayCurrency}
            onChange={(e) => setDisplayCurrency(e.target.value as CurrencyCode)}
            className="cursor-pointer bg-transparent text-xs font-bold text-ink focus:outline-none"
          >
            {(Object.keys(CURRENCIES) as CurrencyCode[]).map((code) => (
              <option key={code} value={code} className="bg-surface text-ink">
                {code} ({language === 'ar' ? CURRENCIES[code].symbolAr : CURRENCIES[code].symbolEn})
              </option>
            ))}
          </select>
        </div>
      )}

      <button
        type="button"
        onClick={toggleLanguage}
        aria-label="Switch language"
        className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-surface px-2.5 py-2 text-xs font-bold text-ink transition-colors hover:border-line-strong hover:bg-subtle focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent"
        title="AR / EN"
      >
        <Languages className="h-3.5 w-3.5 text-accent" />
        <span>{language === 'en' ? 'AR' : 'EN'}</span>
      </button>

      <button
        type="button"
        onClick={toggleTheme}
        aria-label={isDark ? t('nav.switchToLight') : t('nav.switchToDark')}
        title={isDark ? t('nav.switchToLight') : t('nav.switchToDark')}
        className="inline-flex items-center justify-center rounded-xl border border-line bg-surface p-2 text-ink-secondary transition-colors hover:border-line-strong hover:bg-subtle hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent dark:text-amber-300"
      >
        {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
      </button>
    </div>
  );
}