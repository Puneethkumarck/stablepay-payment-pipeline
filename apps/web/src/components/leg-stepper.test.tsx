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
    render(<LegStepper legs={twoLegs} />);
    const cards = screen.getAllByTestId('leg-card');
    expect(cards.length).toBe(2);
  });

  it('renders 3-leg variant', () => {
    render(<LegStepper legs={threeLegs} />);
    const cards = screen.getAllByTestId('leg-card');
    expect(cards.length).toBe(3);
  });

  it('renders leg labels', () => {
    render(<LegStepper legs={twoLegs} />);
    expect(screen.getByTestId('leg-stepper')).toHaveTextContent('Fiat Payin');
    expect(screen.getByTestId('leg-stepper')).toHaveTextContent('Crypto Payout');
  });

  it('renders leg detail when provided', () => {
    render(<LegStepper legs={twoLegs} />);
    expect(screen.getByTestId('leg-stepper')).toHaveTextContent('txn-abc');
  });

  it('shows red border for failed legs', () => {
    const failedLegs: LegState[] = [
      { label: 'Payin', status: 'FAILED' },
      { label: 'Payout', status: 'INITIATED' },
    ];
    render(<LegStepper legs={failedLegs} />);
    const cards = screen.getAllByTestId('leg-card');
    expect(cards[0]?.className).toContain('border-[rgba(239,68,68,0.32)]');
  });

  it('shows skipped overlay for legs after a failed leg', () => {
    const failedLegs: LegState[] = [
      { label: 'Payin', status: 'FAILED' },
      { label: 'Payout', status: 'INITIATED' },
    ];
    render(<LegStepper legs={failedLegs} />);
    const cards = screen.getAllByTestId('leg-card');
    expect(cards[1]?.className).toContain('opacity-50');
    expect(screen.getByTestId('leg-stepper')).toHaveTextContent('Skipped');
  });

  it('shows compensation initiated banner', () => {
    render(<LegStepper legs={twoLegs} flowStatus="COMPENSATION_INITIATED" />);
    expect(screen.getByTestId('compensation-banner')).toHaveTextContent('Compensation in progress');
  });

  it('shows compensation completed banner', () => {
    render(<LegStepper legs={twoLegs} flowStatus="COMPENSATION_COMPLETED" />);
    expect(screen.getByTestId('compensation-banner')).toHaveTextContent('Compensation complete');
  });

  it('does not show skipped when compensation is completed', () => {
    const failedLegs: LegState[] = [
      { label: 'Payin', status: 'FAILED' },
      { label: 'Payout', status: 'INITIATED' },
    ];
    render(<LegStepper legs={failedLegs} flowStatus="COMPENSATION_COMPLETED" />);
    expect(screen.queryByText('Skipped')).toBeNull();
  });
});
