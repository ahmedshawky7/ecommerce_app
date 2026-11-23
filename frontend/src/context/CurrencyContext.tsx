import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import {
  BASE_CURRENCY,
  CURRENCIES,
  CurrencyCode,
  CurrencyConfig,
  PAYMENT_CURRENCY,
  formatBaseUsd,
  formatPrice as formatPriceUtil,
} from '../lib/utils';
import { useI18n } from './I18nContext';

interface CurrencyContextValue {
  baseCurrency: CurrencyCode;
  paymentCurrency: CurrencyCode;
  displayCurrency: CurrencyCode;
  currencyConfig: CurrencyConfig;
  currencySymbol: string;
  isEstimatedDisplay: boolean;
  setDisplayCurrency: (code: CurrencyCode) => void;
  /** Formats price in selected displayCurrency (indicative estimate if != USD). */
  formatPrice: (amountInUsd?: number | null) => string;
  /** Formats exact authoritative USD price (billing / Stripe / admin). */
  formatBasePrice: (amountInUsd?: number | null) => string;
}

const STORAGE_KEY = 'novacart_display_currency';

const CurrencyContext = createContext<CurrencyContextValue | undefined>(undefined);

function getInitialCurrency(): CurrencyCode {
  try {
    const saved = localStorage.getItem(STORAGE_KEY) as CurrencyCode | null;
    if (saved && saved in CURRENCIES) return saved;
  } catch {
    // ignore
  }
  return BASE_CURRENCY;
}

export function CurrencyProvider({ children }: { children: React.ReactNode }) {
  const [displayCurrency, setDisplayCurrencyState] = useState<CurrencyCode>(getInitialCurrency);
  const { language } = useI18n();

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, displayCurrency);
    } catch {
      // ignore
    }
  }, [displayCurrency]);

  const setDisplayCurrency = (code: CurrencyCode) => setDisplayCurrencyState(code);
  const currencyConfig = CURRENCIES[displayCurrency];
  const currencySymbol =
    language === 'ar' ? currencyConfig.symbolAr : currencyConfig.symbolEn;

  const formatPrice = useCallback(
    (amountInUsd?: number | null) => formatPriceUtil(amountInUsd, displayCurrency, language),
    [displayCurrency, language]
  );

  const formatBasePrice = useCallback(
    (amountInUsd?: number | null) => formatBaseUsd(amountInUsd),
    []
  );

  return (
    <CurrencyContext.Provider
      value={{
        baseCurrency: BASE_CURRENCY,
        paymentCurrency: PAYMENT_CURRENCY,
        displayCurrency,
        currencyConfig,
        currencySymbol,
        isEstimatedDisplay: displayCurrency !== BASE_CURRENCY,
        setDisplayCurrency,
        formatPrice,
        formatBasePrice,
      }}
    >
      {children}
    </CurrencyContext.Provider>
  );
}

export function useCurrency() {
  const ctx = useContext(CurrencyContext);
  if (!ctx) throw new Error('useCurrency must be used inside CurrencyProvider');
  return ctx;
}