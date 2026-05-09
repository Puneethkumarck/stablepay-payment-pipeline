const MICROS_DIVISOR = 1_000_000;

interface CurrencyConfig {
  symbol: string;
  position: 'prefix' | 'suffix';
  decimals: number;
  fullPrecisionDecimals: number;
  locale: string;
}

const FIAT_CURRENCIES: Record<string, CurrencyConfig> = {
  USD: { symbol: '$', position: 'prefix', decimals: 2, fullPrecisionDecimals: 2, locale: 'en-US' },
  EUR: { symbol: '€', position: 'prefix', decimals: 2, fullPrecisionDecimals: 2, locale: 'en-US' },
  GBP: { symbol: '£', position: 'prefix', decimals: 2, fullPrecisionDecimals: 2, locale: 'en-US' },
  INR: {
    symbol: '₹',
    position: 'prefix',
    decimals: 2,
    fullPrecisionDecimals: 2,
    locale: 'en-IN',
  },
};

const CRYPTO_DECIMALS: Record<string, number> = {
  USDC: 6,
  USDT: 6,
  SOL: 9,
  ETH: 18,
  BTC: 8,
};

export type MoneyPrecision = 'default' | 'full';

export function formatMoney(
  amountMicros: number,
  currency: string,
  precision: MoneyPrecision = 'default',
): string {
  const fiatConfig = FIAT_CURRENCIES[currency];

  if (fiatConfig) {
    const value = amountMicros / MICROS_DIVISOR;
    const formatted = value.toLocaleString(fiatConfig.locale, {
      minimumFractionDigits: fiatConfig.decimals,
      maximumFractionDigits: fiatConfig.decimals,
    });
    return `${fiatConfig.symbol}${formatted}`;
  }

  const cryptoFullDecimals = CRYPTO_DECIMALS[currency] ?? 6;
  const displayDecimals = precision === 'full' ? cryptoFullDecimals : 4;
  const divisor = 10 ** cryptoFullDecimals;
  const value = amountMicros / divisor;
  const formatted = value.toLocaleString('en-US', {
    minimumFractionDigits: displayDecimals,
    maximumFractionDigits: displayDecimals,
  });

  return `${formatted} ${currency}`;
}
