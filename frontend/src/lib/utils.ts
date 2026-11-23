import { OrderStatus } from '../types/api';
import { Language } from '../i18n/translations';

export type CurrencyCode = 'USD' | 'EGP' | 'SAR' | 'AED';

export interface CurrencyConfig {
  code: CurrencyCode;
  /** Indicative reference rate relative to 1 USD (display only, NOT live FX). */
  indicativeRate: number;
  symbolEn: string;
  symbolAr: string;
}

export const BASE_CURRENCY: CurrencyCode = 'USD';
export const PAYMENT_CURRENCY: CurrencyCode = 'USD';

export const CURRENCIES: Record<CurrencyCode, CurrencyConfig> = {
  USD: { code: 'USD', indicativeRate: 1, symbolEn: '$', symbolAr: '$' },
  EGP: { code: 'EGP', indicativeRate: 50, symbolEn: 'EGP', symbolAr: 'ج.م' },
  SAR: { code: 'SAR', indicativeRate: 3.75, symbolEn: 'SAR', symbolAr: 'ر.س' },
  AED: { code: 'AED', indicativeRate: 3.67, symbolEn: 'AED', symbolAr: 'د.إ' },
};

export function formatNumber(
  value?: number | null,
  options?: Intl.NumberFormatOptions
): string {
  const num = typeof value === 'number' ? value : Number(value ?? 0);
  return new Intl.NumberFormat('en-US', options).format(Number.isFinite(num) ? num : 0);
}

/**
 * Formats an authoritative USD amount into the selected display currency.
 * Always uses Western digits (0-9).
 */
export function formatPrice(
  amountInUsd?: number | null,
  displayCurrency: CurrencyCode = 'USD',
  language: Language = 'en'
): string {
  const config = CURRENCIES[displayCurrency] ?? CURRENCIES.USD;
  const raw = typeof amountInUsd === 'number' ? amountInUsd : Number(amountInUsd ?? 0);
  const converted = (Number.isFinite(raw) ? raw : 0) * config.indicativeRate;

  const isWholeEgp =
    displayCurrency === 'EGP' && Math.abs(converted - Math.round(converted)) < 0.005;

  const formattedNum = formatNumber(converted, {
    minimumFractionDigits: isWholeEgp ? 0 : 2,
    maximumFractionDigits: 2,
  });

  if (displayCurrency === 'USD') {
    return `$${formattedNum}`;
  }

  const symbol = language === 'ar' ? config.symbolAr : config.symbolEn;
  return `${formattedNum} ${symbol}`;
}

/** Formats authoritative USD amount for billing, Stripe invoices, and admin accounting. */
export function formatBaseUsd(amountInUsd?: number | null): string {
  return formatPrice(amountInUsd, 'USD', 'en');
}

export function formatDate(dateStr?: string | null, locale = 'en-US'): string {
  if (!dateStr) return '-';
  const effectiveLocale = locale.startsWith('ar') ? 'ar-EG-u-nu-latn' : 'en-US';
  return new Intl.DateTimeFormat(effectiveLocale, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(dateStr));
}

export function generateIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

export function getApiErrorMessage(err: unknown, fallback = 'Something went wrong'): string {
  const error = err as {
    response?: { data?: { message?: string; details?: Record<string, string> } };
    message?: string;
  };
  const details = error?.response?.data?.details;
  if (details && typeof details === 'object') {
    const firstMsg = Object.values(details)[0];
    if (firstMsg) return firstMsg;
  }
  return error?.response?.data?.message || error?.message || fallback;
}

export const ORDER_STATUS_STYLES: Record<OrderStatus, string> = {
  PENDING:
    'bg-amber-50 text-amber-800 border-amber-200 dark:bg-amber-500/15 dark:text-amber-300 dark:border-amber-500/30',
  CONFIRMED:
    'bg-sky-50 text-sky-800 border-sky-200 dark:bg-sky-500/15 dark:text-sky-300 dark:border-sky-500/30',
  PROCESSING:
    'bg-blue-50 text-blue-800 border-blue-200 dark:bg-blue-500/15 dark:text-blue-300 dark:border-blue-500/30',
  SHIPPED:
    'bg-cyan-50 text-cyan-800 border-cyan-200 dark:bg-cyan-500/15 dark:text-cyan-300 dark:border-cyan-500/30',
  DELIVERED:
    'bg-emerald-50 text-emerald-800 border-emerald-200 dark:bg-emerald-500/15 dark:text-emerald-300 dark:border-emerald-500/30',
  CANCELLED:
    'bg-rose-50 text-rose-800 border-rose-200 dark:bg-rose-500/15 dark:text-rose-300 dark:border-rose-500/30',
};