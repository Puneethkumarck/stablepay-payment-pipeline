import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { type LegState, LegStepper } from './leg-stepper';

describe('LegStepper', () => {
  const twoLegs: LegState[] = [
    { label: 'Fiat Payin', status: 'COMPLETED', detail: 'txn-abc' },
    { label: 'Crypto Payout', status: 'CONFIRMING' },
  ];

  const threeLegs: LegState[] = [
    { label: 'Crypto Payin', status: 'COMPLETED' },
    { label: 'Trade', status: 'COMPLETED' },
    { label: 'Crypto Payout', status: 'BROADCASTING' },
  ];

  it('renders 2-leg variant', () => {
    // act
    render(<LegStepper legs={twoLegs} />);

    // assert
    const cards = screen.getAllByTestId('leg-card');
    expect(cards.length).toBe(2);
  });

  it('renders 3-leg variant', () => {
    // act
    render(<LegStepper legs={threeLegs} />);

    // assert
    const cards = screen.getAllByTestId('leg-card');
    expect(cards.length).toBe(3);
  });

  it('renders leg labels', () => {
    // act
    render(<LegStepper legs={twoLegs} />);

    // assert
    expect(screen.getByText('Fiat Payin')).toBeInTheDocument();
    expect(screen.getByText('Crypto Payout')).toBeInTheDocument();
  });

  it('renders leg detail when provided', () => {
    // act
    render(<LegStepper legs={twoLegs} />);

    // assert
    expect(screen.getByText('txn-abc')).toBeInTheDocument();
  });

  it('marks failed legs with data-failed attribute', () => {
    // arrange
    const failedLegs: LegState[] = [
      { label: 'Payin', status: 'FAILED' },
      { label: 'Payout', status: 'INITIATED' },
    ];

    // act
    render(<LegStepper legs={failedLegs} />);

    // assert
    const cards = screen.getAllByTestId('leg-card');
    expect(cards[0]).toHaveAttribute('data-failed', 'true');
    expect(cards[1]).not.toHaveAttribute('data-failed');
  });

  it('marks legs after a failed leg as skipped', () => {
    // arrange
    const failedLegs: LegState[] = [
      { label: 'Payin', status: 'FAILED' },
      { label: 'Payout', status: 'INITIATED' },
    ];

    // act
    render(<LegStepper legs={failedLegs} />);

    // assert
    const cards = screen.getAllByTestId('leg-card');
    expect(cards[1]).toHaveAttribute('data-skipped', 'true');
    expect(screen.getByText('Skipped')).toBeInTheDocument();
  });

  it('shows compensation initiated banner', () => {
    // act
    render(<LegStepper legs={twoLegs} flowStatus="COMPENSATION_INITIATED" />);

    // assert
    expect(screen.getByText(/Compensation in progress/)).toBeInTheDocument();
  });

  it('shows compensation completed banner', () => {
    // act
    render(<LegStepper legs={twoLegs} flowStatus="COMPENSATION_COMPLETED" />);

    // assert
    expect(screen.getByText('Compensation complete')).toBeInTheDocument();
  });

  it('does not show skipped when compensation is completed', () => {
    // arrange
    const failedLegs: LegState[] = [
      { label: 'Payin', status: 'FAILED' },
      { label: 'Payout', status: 'INITIATED' },
    ];

    // act
    render(<LegStepper legs={failedLegs} flowStatus="COMPENSATION_COMPLETED" />);

    // assert
    expect(screen.queryByText('Skipped')).toBeNull();
  });
});
