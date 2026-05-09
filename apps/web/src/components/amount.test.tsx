import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Amount } from './amount';

describe('Amount', () => {
  it('formats USD with dollar sign prefix', () => {
    render(<Amount value={100_000_000} currency="USD" />);
    expect(screen.getByTestId('amount')).toHaveTextContent('$100.00');
  });

  it('formats EUR with euro sign prefix', () => {
    render(<Amount value={50_000_000} currency="EUR" />);
    expect(screen.getByTestId('amount')).toHaveTextContent('€50.00');
  });

  it('formats GBP with pound sign prefix', () => {
    render(<Amount value={75_000_000} currency="GBP" />);
    expect(screen.getByTestId('amount')).toHaveTextContent('£75.00');
  });

  it('formats INR with rupee sign and Indian locale grouping', () => {
    render(<Amount value={1_000_000_000_000} currency="INR" />);
    expect(screen.getByTestId('amount')).toHaveTextContent('₹10,00,000.00');
  });

  it('formats USDC with suffix and default 4 decimals', () => {
    render(<Amount value={500_000} currency="USDC" />);
    expect(screen.getByTestId('amount')).toHaveTextContent('0.5000 USDC');
  });

  it('formats SOL with suffix', () => {
    render(<Amount value={1_000_000_000} currency="SOL" />);
    expect(screen.getByTestId('amount')).toHaveTextContent('SOL');
  });

  it('formats ETH with suffix', () => {
    render(<Amount value={1_000_000_000_000_000_000} currency="ETH" />);
    expect(screen.getByTestId('amount')).toHaveTextContent('ETH');
  });

  it('applies sm size class', () => {
    render(<Amount value={100_000_000} currency="USD" size="sm" />);
    expect(screen.getByTestId('amount').className).toContain('text-[12px]');
  });

  it('applies xl size class', () => {
    render(<Amount value={100_000_000} currency="USD" size="xl" />);
    expect(screen.getByTestId('amount').className).toContain('text-[32px]');
  });

  it('defaults to USDC currency', () => {
    render(<Amount value={1_000_000} />);
    expect(screen.getByTestId('amount')).toHaveTextContent('USDC');
  });
});
