import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createStuckPayment } from '~/test/fixtures/stuck';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { StuckList } from './stuck-list';

vi.mock('~/components/stuck/refund-confirmation-dialog', () => ({
  RefundConfirmationDialog: ({ stuckPayment }: { stuckPayment: { transaction_ref: string } }) => (
    <button data-testid={`refund-dialog-${stuckPayment.transaction_ref}`}>Trigger refund</button>
  ),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

const now = new Date('2026-01-15T12:00:00Z');

describe('StuckList', () => {
  beforeEach(() => {
    vi.useFakeTimers({ now, shouldAdvanceTime: true });
    server.resetHandlers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  function setupHandler(payments = [createStuckPayment()]) {
    server.use(http.get('/api/v1/admin/stuck', () => HttpResponse.json(payments)));
  }

  it('renders page header', () => {
    // arrange
    setupHandler();

    // act
    render(<StuckList />);

    // assert
    expect(screen.getByText('Stuck Payments')).toBeInTheDocument();
    expect(screen.getByText('Admin')).toBeInTheDocument();
  });

  it('renders banner with total count', async () => {
    // arrange
    setupHandler([createStuckPayment(), createStuckPayment({ transaction_ref: 'TXN-STUCK-002' })]);

    // act
    render(<StuckList />);

    // assert
    const banner = await screen.findByTestId('stuck-banner');
    expect(banner).toHaveTextContent('2 stuck payments');
  });

  it('renders banner with critical threshold count for payments stuck >= 24h', async () => {
    // arrange
    setupHandler([
      createStuckPayment({ stuck_since: '2026-01-14T08:00:00Z' }),
      createStuckPayment({
        transaction_ref: 'TXN-STUCK-RECENT',
        stuck_since: '2026-01-15T11:00:00Z',
      }),
    ]);

    // act
    render(<StuckList />);

    // assert
    const banner = await screen.findByTestId('stuck-banner');
    expect(banner).toHaveTextContent('1 exceeds 24h threshold');
  });

  it('renders expandable cards for each stuck payment', async () => {
    // arrange
    setupHandler([
      createStuckPayment({ transaction_ref: 'TXN-CARD-1' }),
      createStuckPayment({ transaction_ref: 'TXN-CARD-2' }),
    ]);

    // act
    render(<StuckList />);

    // assert
    expect(await screen.findByTestId('stuck-card-TXN-CARD-1')).toBeInTheDocument();
    expect(screen.getByTestId('stuck-card-TXN-CARD-2')).toBeInTheDocument();
  });

  it('expands card on click to reveal reason and actions', async () => {
    // arrange
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    setupHandler([
      createStuckPayment({
        transaction_ref: 'TXN-EXPAND',
        stuck_reason: 'Partner timeout after 24h',
      }),
    ]);
    render(<StuckList />);

    // act
    const card = await screen.findByTestId('stuck-card-TXN-EXPAND');
    await user.click(card);

    // assert
    const detail = screen.getByTestId('stuck-card-detail-TXN-EXPAND');
    expect(detail).toHaveTextContent('Partner timeout after 24h');
    expect(within(detail).getByTestId('view-transaction-link')).toBeInTheDocument();
    expect(within(detail).getByTestId('refund-dialog-TXN-EXPAND')).toBeInTheDocument();
  });

  it('collapses card on second click', async () => {
    // arrange
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    setupHandler([createStuckPayment({ transaction_ref: 'TXN-COLLAPSE' })]);
    render(<StuckList />);

    // act — expand then collapse
    const card = await screen.findByTestId('stuck-card-TXN-COLLAPSE');
    await user.click(card);
    expect(screen.getByTestId('stuck-card-detail-TXN-COLLAPSE')).toBeInTheDocument();
    await user.click(card);

    // assert
    expect(screen.queryByTestId('stuck-card-detail-TXN-COLLAPSE')).not.toBeInTheDocument();
  });

  it('shows critical badge for payments stuck >= 24h', async () => {
    // arrange
    setupHandler([createStuckPayment({ stuck_since: '2026-01-14T08:00:00Z' })]);

    // act
    render(<StuckList />);

    // assert
    expect(await screen.findByTestId('critical-badge')).toBeInTheDocument();
  });

  it('does not show critical badge for payments stuck < 24h', async () => {
    // arrange
    setupHandler([createStuckPayment({ stuck_since: '2026-01-15T11:30:00Z' })]);

    // act
    render(<StuckList />);

    // assert
    await screen.findByTestId('stuck-cards');
    expect(screen.queryByTestId('critical-badge')).not.toBeInTheDocument();
  });

  it('does not render an Escalate button', async () => {
    // arrange
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
    setupHandler([createStuckPayment({ transaction_ref: 'TXN-NO-ESC' })]);
    render(<StuckList />);

    // act
    const card = await screen.findByTestId('stuck-card-TXN-NO-ESC');
    await user.click(card);

    // assert
    expect(screen.queryByText('Escalate')).not.toBeInTheDocument();
  });

  it('renders agg_stuck_withdrawals table with Trino eyebrow', async () => {
    // arrange
    setupHandler([createStuckPayment()]);

    // act
    render(<StuckList />);

    // assert
    expect(await screen.findByText('Trino · iceberg catalog')).toBeInTheDocument();
    expect(screen.getByText('agg_stuck_withdrawals')).toBeInTheDocument();
  });

  it('renders agg table with stuck payment data', async () => {
    // arrange
    setupHandler([createStuckPayment({ transaction_ref: 'TXN-AGG-001' })]);

    // act
    render(<StuckList />);

    // assert
    const table = screen.getByTestId('data-table');
    expect(await within(table).findByText('TXN-AGG-001')).toBeInTheDocument();
  });

  it('shows empty state when no stuck payments', async () => {
    // arrange
    setupHandler([]);

    // act
    render(<StuckList />);

    // assert
    expect(await screen.findByText('No stuck payments')).toBeInTheDocument();
    expect(screen.queryByTestId('stuck-banner')).not.toBeInTheDocument();
  });
});
