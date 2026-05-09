import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createTransaction } from '~/test/fixtures/transaction';
import { render } from '~/test/render';
import { TransactionDetail } from './transaction-detail';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe('TransactionDetail', () => {
  it('renders the hero card with amount and status badge', () => {
    // arrange
    const txn = createTransaction({
      ref: 'TXN-500',
      amount: { amount: 250_000, currency: 'USDC' },
      internal_status: 'SCREENING_IN_PROGRESS',
    });

    // act
    render(<TransactionDetail txnRef="TXN-500" initialData={txn} />);

    // assert
    expect(screen.getByTestId('hero-card')).toBeInTheDocument();
    expect(screen.getByTestId('amount')).toBeInTheDocument();
    expect(screen.getByTestId('status-badge')).toBeInTheDocument();
  });

  it('renders the 8-cell metadata grid', () => {
    // arrange
    const txn = createTransaction({
      ref: 'TXN-501',
      customer_id: 'CUST-ABC',
      flow_id: 'FLOW-XYZ',
      counterparty: 'Alice Corp',
    });

    // act
    render(<TransactionDetail txnRef="TXN-501" initialData={txn} />);

    // assert
    const grid = screen.getByTestId('metadata-grid');
    expect(grid).toBeInTheDocument();

    const labels = [
      'customer_id',
      'account_id',
      'counterparty',
      'flow_type (topic)',
      'currency_code',
      'provider / chain',
      'created',
    ];
    for (const label of labels) {
      expect(within(grid).getByText(label)).toBeInTheDocument();
    }
    expect(within(grid).getByText('flow_id')).toBeInTheDocument();
  });

  it('renders the state-machine timeline', () => {
    // arrange
    const txn = createTransaction({
      ref: 'TXN-502',
      type: 'FIAT',
      direction: 'PAYIN',
      internal_status: 'MATCHED',
    });

    // act
    render(<TransactionDetail txnRef="TXN-502" initialData={txn} />);

    // assert
    expect(screen.getByTestId('timeline-card')).toBeInTheDocument();
    expect(screen.getByTestId('timeline')).toBeInTheDocument();
    expect(screen.getByText('State-machine timeline')).toBeInTheDocument();
  });

  it('renders the EventEnvelope card with KV rows', () => {
    // arrange
    const txn = createTransaction({
      ref: 'TXN-503',
      event_id: 'EVT-001',
      correlation_id: 'CORR-001',
      trace_id: 'TRACE-001',
      schema_version: '1.0.0',
      source_topic: 'payment.fiat.payin.v1',
    });

    // act
    render(<TransactionDetail txnRef="TXN-503" initialData={txn} />);

    // assert
    const envelope = screen.getByTestId('envelope-card');
    expect(envelope).toBeInTheDocument();
    expect(within(envelope).getByText('EventEnvelope fields')).toBeInTheDocument();
    expect(within(envelope).getByText('EVT-001')).toBeInTheDocument();
    expect(within(envelope).getByText('CORR-001')).toBeInTheDocument();
    expect(within(envelope).getByText('TRACE-001')).toBeInTheDocument();
    expect(within(envelope).getByText('1.0.0')).toBeInTheDocument();
    expect(within(envelope).getByText('payment.fiat.payin.v1')).toBeInTheDocument();
  });

  it('shows dash for missing optional EventEnvelope fields', () => {
    // arrange
    const txn = createTransaction({ ref: 'TXN-504' });

    // act
    render(<TransactionDetail txnRef="TXN-504" initialData={txn} />);

    // assert
    const kvRows = screen.getAllByTestId('kv-row');
    const dashValues = kvRows.filter((row) => row.textContent?.includes('—'));
    expect(dashValues.length).toBeGreaterThanOrEqual(4);
  });

  it('shows polling active for non-terminal status', () => {
    // arrange
    const txn = createTransaction({ ref: 'TXN-505', internal_status: 'INITIATED' });

    // act
    render(<TransactionDetail txnRef="TXN-505" initialData={txn} />);

    // assert
    expect(screen.getByTestId('polling-footer')).toHaveTextContent('3s polling active');
  });

  it('shows terminal state reached for terminal status', () => {
    // arrange
    const txn = createTransaction({ ref: 'TXN-506', internal_status: 'COMPLETED' });

    // act
    render(<TransactionDetail txnRef="TXN-506" initialData={txn} />);

    // assert
    expect(screen.getByTestId('polling-footer')).toHaveTextContent('Terminal state reached');
  });

  it('renders page header with transaction ref and back button', () => {
    // arrange
    const txn = createTransaction({ ref: 'TXN-507' });

    // act
    render(<TransactionDetail txnRef="TXN-507" initialData={txn} />);

    // assert
    expect(screen.getByTestId('page-header')).toBeInTheDocument();
    expect(screen.getByText('Transaction TXN-507')).toBeInTheDocument();
    expect(screen.getByTestId('page-header-back')).toBeInTheDocument();
  });
});
