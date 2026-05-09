import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Amount } from './amount';

describe('Amount', () => {
  it('formats USD with dollar sign prefix', () => {
    // arrange
    const value = 100_000_000;

    // act
    render(<Amount value={value} currency="USD" />);

    // assert
    expect(screen.getByText('$100.00')).toBeInTheDocument();
  });

  it('formats EUR with euro sign prefix', () => {
    // arrange
    const value = 50_000_000;

    // act
    render(<Amount value={value} currency="EUR" />);

    // assert
    expect(screen.getByText('€50.00')).toBeInTheDocument();
  });

  it('formats GBP with pound sign prefix', () => {
    // arrange
    const value = 75_000_000;

    // act
    render(<Amount value={value} currency="GBP" />);

    // assert
    expect(screen.getByText('£75.00')).toBeInTheDocument();
  });

  it('formats INR with rupee sign and Indian locale grouping', () => {
    // arrange
    const value = 1_000_000_000_000;

    // act
    render(<Amount value={value} currency="INR" />);

    // assert
    expect(screen.getByText('₹10,00,000.00')).toBeInTheDocument();
  });

  it('formats USDC with suffix and default 4 decimals', () => {
    // arrange
    const value = 500_000;

    // act
    render(<Amount value={value} currency="USDC" />);

    // assert
    expect(screen.getByText('0.5000 USDC')).toBeInTheDocument();
  });

  it('formats SOL with suffix', () => {
    // act
    render(<Amount value={1_000_000_000} currency="SOL" />);

    // assert
    expect(screen.getByTestId('amount')).toHaveTextContent('SOL');
  });

  it('formats ETH with suffix', () => {
    // act
    render(<Amount value={1_000_000_000_000_000_000} currency="ETH" />);

    // assert
    expect(screen.getByTestId('amount')).toHaveTextContent('ETH');
  });

  it('applies sm size via data attribute', () => {
    // act
    render(<Amount value={100_000_000} currency="USD" size="sm" />);

    // assert
    expect(screen.getByTestId('amount')).toHaveAttribute('data-size', 'sm');
  });

  it('applies xl size via data attribute', () => {
    // act
    render(<Amount value={100_000_000} currency="USD" size="xl" />);

    // assert
    expect(screen.getByTestId('amount')).toHaveAttribute('data-size', 'xl');
  });

  it('defaults to USDC currency', () => {
    // act
    render(<Amount value={1_000_000} />);

    // assert
    expect(screen.getByTestId('amount')).toHaveTextContent('USDC');
  });
});
