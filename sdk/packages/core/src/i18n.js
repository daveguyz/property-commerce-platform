/**
 * Property Commerce Platform SDK — i18n.js
 *
 * A lean port of the currency/date formatting logic from the original
 * plugin-i18n.js (863 lines, Shopify-specific detection + selector UI).
 * The full detection/selector system is intentionally deferred —
 * the host site is expected to pass currency/locale in PCPConfig,
 * since on a generic website there's no Shopify "market" concept
 * to detect from. This module is what every widget needs on day one:
 * consistent number/date formatting given a known currency and locale.
 */

import { getConfig } from './config.js';

const CURRENCIES = {
  USD: { symbol: '$', name: 'US Dollar' },
  EUR: { symbol: '€', name: 'Euro' },
  GBP: { symbol: '£', name: 'British Pound' },
  JPY: { symbol: '¥', name: 'Japanese Yen' },
  AUD: { symbol: 'A$', name: 'Australian Dollar' },
  CAD: { symbol: 'C$', name: 'Canadian Dollar' },
  CHF: { symbol: 'Fr', name: 'Swiss Franc' },
  CNY: { symbol: '¥', name: 'Chinese Yuan' },
  INR: { symbol: '₹', name: 'Indian Rupee' },
  ZAR: { symbol: 'R', name: 'South African Rand' },
  AED: { symbol: 'د.إ', name: 'UAE Dirham' },
};

const LOCALE_MAP = {
  en: 'en-US', fr: 'fr-FR', es: 'es-ES', de: 'de-DE',
  pt: 'pt-BR', ar: 'ar-SA', zh: 'zh-CN',
};

export function currencySymbol(code) {
  return CURRENCIES[code]?.symbol || code;
}

/** Formats a number in the configured currency, e.g. fmt(450000) -> "$450,000". */
export function fmt(amount) {
  const { currency, locale } = getConfig();
  const bcp47 = LOCALE_MAP[locale] || 'en-US';
  try {
    return new Intl.NumberFormat(bcp47, {
      style: 'currency',
      currency,
      maximumFractionDigits: currency === 'JPY' ? 0 : 0,
    }).format(Number(amount) || 0);
  } catch (_) {
    return currencySymbol(currency) + Number(amount || 0).toLocaleString();
  }
}

/** Formats an ISO datetime string using the configured locale and the browser's timezone. */
export function fmtDate(iso, opts = {}) {
  if (!iso) return '';
  const { locale } = getConfig();
  const bcp47 = LOCALE_MAP[locale] || 'en-US';
  const tz = opts.timezone || Intl.DateTimeFormat().resolvedOptions().timeZone;
  try {
    return new Intl.DateTimeFormat(bcp47, {
      timeZone: tz,
      day: 'numeric',
      month: 'short',
      hour: opts.withTime ? '2-digit' : undefined,
      minute: opts.withTime ? '2-digit' : undefined,
      year: opts.withYear !== false ? 'numeric' : undefined,
    }).format(new Date(iso));
  } catch (_) {
    return new Date(iso).toLocaleString();
  }
}

export { CURRENCIES };
