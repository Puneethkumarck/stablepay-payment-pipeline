import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createFlow } from '~/test/fixtures/flow';
import { render } from '~/test/render';
import { FlowDetail } from './flow-detail';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe('FlowDetail', () => {
  it('renders 2 leg cards for a 2-leg FIAT_PAYOUT flow', () => {
    // arrange
    const flow = createFlow({ flow_type: 'FIAT_PAYOUT' });

    // act
    render(<FlowDetail flowId="FLOW-001" initialData={flow} />);

    // assert
    const legCards = screen.getAllByTestId('leg-card');
    expect(legCards).toHaveLength(2);
  });

  it('renders 3 leg cards for a CRYPTO_TO_CRYPTO flow', () => {
    // arrange
    const flow = createFlow({
      flow_type: 'CRYPTO_TO_CRYPTO',
      legs: [
        { leg_index: 0, transaction_ref: 'TXN-A', direction: 'PAYIN', type: 'CRYPTO', status: 'COMPLETED', amount: { amount: 50_000, currency: 'USDC' } },
        { leg_index: 1, transaction_ref: 'TXN-B', direction: 'PAYOUT', type: 'CRYPTO', status: 'COMPLETED', amount: { amount: 50_000, currency: 'USDC' } },
        { leg_index: 2, transaction_ref: 'TXN-C', direction: 'PAYOUT', type: 'CRYPTO', status: 'COMPLETED', amount: { amount: 49_000, currency: 'ETH' } },
      ],
    });

    // act
    render(<FlowDetail flowId="FLOW-002" initialData={flow} />);

    // assert
    const legCards = screen.getAllByTestId('leg-card');
    expect(legCards).toHaveLength(3);
  });

  it('renders failed leg with red border and danger badge', () => {
    // arrange
    const flow = createFlow({
      status: 'FAILED',
      legs: [
        { leg_index: 0, transaction_ref: 'TXN-001', direction: 'PAYIN', type: 'FIAT', status: 'COMPLETED', amount: { amount: 100_000, currency: 'USD' } },
        { leg_index: 1, transaction_ref: 'TXN-002', direction: 'PAYOUT', type: 'FIAT', status: 'FAILED', amount: { amount: 92_000, currency: 'EUR' } },
      ],
    });

    // act
    render(<FlowDetail flowId="FLOW-003" initialData={flow} />);

    // assert
    const legCards = screen.getAllByTestId('leg-card');
    const failedCard = legCards[1];
    expect(failedCard).toHaveAttribute('data-failed', 'true');
  });

  it('renders skipped leg with 0.5 opacity and Skipped text after a failed leg', () => {
    // arrange
    const flow = createFlow({
      status: 'FAILED',
      legs: [
        { leg_index: 0, transaction_ref: 'TXN-A', direction: 'PAYIN', type: 'CRYPTO', status: 'FAILED', amount: { amount: 50_000, currency: 'USDC' } },
        { leg_index: 1, transaction_ref: 'TXN-B', direction: 'PAYOUT', type: 'CRYPTO', status: 'PENDING', amount: { amount: 50_000, currency: 'USDC' } },
        { leg_index: 2, transaction_ref: 'TXN-C', direction: 'PAYOUT', type: 'CRYPTO', status: 'PENDING', amount: { amount: 49_000, currency: 'ETH' } },
      ],
    });

    // act
    render(<FlowDetail flowId="FLOW-004" initialData={flow} />);

    // assert
    const legCards = screen.getAllByTestId('leg-card');
    expect(legCards[1]).toHaveAttribute('data-skipped', 'true');
    expect(legCards[2]).toHaveAttribute('data-skipped', 'true');
    expect(screen.getAllByText('Skipped')).toHaveLength(2);
  });

  it('renders COMPENSATION_INITIATED warning banner', () => {
    // arrange
    const flow = createFlow({
      status: 'COMPENSATION_INITIATED',
      legs: [
        { leg_index: 0, transaction_ref: 'TXN-001', direction: 'PAYIN', type: 'FIAT', status: 'COMPLETED', amount: { amount: 100_000, currency: 'USD' } },
        { leg_index: 1, transaction_ref: 'TXN-002', direction: 'PAYOUT', type: 'FIAT', status: 'FAILED', amount: { amount: 92_000, currency: 'EUR' } },
      ],
    });

    // act
    render(<FlowDetail flowId="FLOW-005" initialData={flow} />);

    // assert
    const banner = screen.getByTestId('compensation-banner');
    expect(banner).toHaveTextContent('Compensation in progress — partner refund initiated');
  });

  it('renders COMPENSATION_COMPLETED success banner', () => {
    // arrange
    const flow = createFlow({
      status: 'COMPENSATION_COMPLETED',
      legs: [
        { leg_index: 0, transaction_ref: 'TXN-001', direction: 'PAYIN', type: 'FIAT', status: 'COMPLETED', amount: { amount: 100_000, currency: 'USD' } },
        { leg_index: 1, transaction_ref: 'TXN-002', direction: 'PAYOUT', type: 'FIAT', status: 'FAILED', amount: { amount: 92_000, currency: 'EUR' } },
      ],
    });

    // act
    render(<FlowDetail flowId="FLOW-006" initialData={flow} />);

    // assert
    const banner = screen.getByTestId('compensation-banner');
    expect(banner).toHaveTextContent('Compensation complete');
  });

  it('shows polling active for non-terminal flow status', () => {
    // arrange
    const flow = createFlow({ status: 'IN_PROGRESS' });

    // act
    render(<FlowDetail flowId="FLOW-007" initialData={flow} />);

    // assert
    expect(screen.getByTestId('polling-footer')).toHaveTextContent('3s polling active');
  });

  it('shows terminal state reached for terminal flow status', () => {
    // arrange
    const flow = createFlow({ status: 'COMPLETED' });

    // act
    render(<FlowDetail flowId="FLOW-008" initialData={flow} />);

    // assert
    expect(screen.getByTestId('polling-footer')).toHaveTextContent('Terminal state reached');
  });

  it('renders page header with flow id and back button', () => {
    // arrange
    const flow = createFlow({ id: 'FLOW-099' });

    // act
    render(<FlowDetail flowId="FLOW-099" initialData={flow} />);

    // assert
    expect(screen.getByTestId('page-header')).toBeInTheDocument();
    expect(screen.getByText('Flow FLOW-099')).toBeInTheDocument();
    expect(screen.getByTestId('page-header-back')).toBeInTheDocument();
  });

  it('renders the 8-cell metadata grid', () => {
    // arrange
    const flow = createFlow();

    // act
    render(<FlowDetail flowId="FLOW-001" initialData={flow} />);

    // assert
    const grid = screen.getByTestId('metadata-grid');
    expect(grid).toBeInTheDocument();

    const labels = [
      'flow_id',
      'customer_id',
      'flow_type',
      'status',
      'source_amount',
      'destination_amount',
      'created',
      'updated',
    ];
    for (const label of labels) {
      expect(within(grid).getByText(label)).toBeInTheDocument();
    }
  });

  it('shows dash for missing destination_amount', () => {
    // arrange
    const flow = createFlow({ destination_amount: undefined });

    // act
    render(<FlowDetail flowId="FLOW-010" initialData={flow} />);

    // assert
    const grid = screen.getByTestId('metadata-grid');
    expect(within(grid).getByText('—')).toBeInTheDocument();
  });

  it('renders flow_type eyebrow with underscores replaced by spaces', () => {
    // arrange
    const flow = createFlow({ flow_type: 'CRYPTO_TO_CRYPTO' });

    // act
    render(<FlowDetail flowId="FLOW-011" initialData={flow} />);

    // assert
    expect(screen.getByText('CRYPTO TO CRYPTO')).toBeInTheDocument();
  });
});
