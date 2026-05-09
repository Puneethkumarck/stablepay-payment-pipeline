import { describe, expect, it } from 'vitest';
import { formatMoney } from './money';

describe('formatMoney', () => {
  describe('fiat currencies', () => {
    it('formats USD with $ prefix and 2 decimals', () => {
      // act — 1500 USD = 1_500 * 1e6 micros
      const result = formatMoney(1_500_000_000, 'USD');

      // assert
      expect(result).toBe('$1,500.00');
    });

    it('formats EUR with € prefix and 2 decimals', () => {
      // act — 250 EUR = 250 * 1e6 micros
      const result = formatMoney(250_000_000, 'EUR');

      // assert
      expect(result).toBe('€250.00');
    });

    it('formats GBP with £ prefix and 2 decimals', () => {
      // act — 99990 GBP = 99_990 * 1e6 micros
      const result = formatMoney(99_990_000_000, 'GBP');

      // assert
      expect(result).toBe('£99,990.00');
    });

    it('formats INR with ₹ prefix and Indian-locale grouping', () => {
      // act — 100000 INR = 100_000 * 1e6 micros
      const result = formatMoney(100_000_000_000, 'INR');

      // assert
      expect(result).toBe('₹1,00,000.00');
    });

    it('handles zero amount', () => {
      // act
      const result = formatMoney(0, 'USD');

      // assert
      expect(result).toBe('$0.00');
    });
  });

  describe('crypto currencies', () => {
    it('formats USDC with suffix code and 4 decimals by default', () => {
      // act
      const result = formatMoney(200_000_000, 'USDC');

      // assert
      expect(result).toBe('200.0000 USDC');
    });

    it('formats USDT with suffix code and 4 decimals by default', () => {
      // act
      const result = formatMoney(1_000_000, 'USDT');

      // assert
      expect(result).toBe('1.0000 USDT');
    });

    it('formats SOL with 4 decimals by default', () => {
      // act
      const result = formatMoney(1_500_000_000, 'SOL');

      // assert
      expect(result).toBe('1.5000 SOL');
    });

    it('formats ETH with full precision when requested', () => {
      // act
      const result = formatMoney(1_000_000_000_000_000_000, 'ETH', 'full');

      // assert
      expect(result).toBe('1.000000000000000000 ETH');
    });

    it('formats BTC with full precision when requested', () => {
      // act
      const result = formatMoney(100_000_000, 'BTC', 'full');

      // assert
      expect(result).toBe('1.00000000 BTC');
    });

    it('formats unknown crypto with 6-decimal divisor and suffix', () => {
      // act
      const result = formatMoney(5_000_000, 'DOGE');

      // assert
      expect(result).toBe('5.0000 DOGE');
    });
  });
});
