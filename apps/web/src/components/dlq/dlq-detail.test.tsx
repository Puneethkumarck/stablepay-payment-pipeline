import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createDlqEntry } from '~/test/fixtures/dlq';
import { render } from '~/test/render';
import { DlqDetail } from './dlq-detail';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('~/components/dlq/replay-button', () => ({
  ReplayButton: ({
    dlqId,
    errorClass,
    retryCount,
  }: {
    dlqId: string;
    errorClass: string;
    retryCount: number;
  }) => (
    <button data-testid="replay-button-mock">
      {dlqId} {errorClass} {retryCount}
    </button>
  ),
}));

vi.mock('~/components/dlq-error-block', () => ({
  DlqErrorBlock: ({ message }: { message: string }) => (
    <pre data-testid="dlq-error-block">{message}</pre>
  ),
}));

vi.mock('~/components/status-badge', () => ({
  StatusBadge: ({ status }: { status: string }) => <span data-testid="status-badge">{status}</span>,
}));

describe('DlqDetail', () => {
  it('renders back link to DLQ list', () => {
    // arrange
    const entry = createDlqEntry();

    // act
    render(<DlqDetail dlqId={entry.id} initialData={entry} />);

    // assert
    const link = screen.getByTestId('dlq-back-link');
    expect(link).toHaveAttribute('href', '/admin/dlq');
  });

  it('renders DLQ ID as page title', () => {
    // arrange
    const entry = createDlqEntry({ id: 'DLQ-TITLE' });

    // act
    render(<DlqDetail dlqId={entry.id} initialData={entry} />);

    // assert
    const heading = screen.getByRole('heading', { level: 1 });
    expect(heading).toHaveTextContent('DLQ-TITLE');
  });

  it('renders KV grid with all entry fields', () => {
    // arrange
    const entry = createDlqEntry({
      id: 'DLQ-KV',
      topic: 'fiat.payin.events.v1',
      event_key: 'TXN-FAIL-001',
      retry_count: 1,
    });

    // act
    render(<DlqDetail dlqId={entry.id} initialData={entry} />);

    // assert
    const grid = screen.getByTestId('dlq-kv-grid');
    expect(within(grid).getByText('DLQ-KV')).toBeInTheDocument();
    expect(within(grid).getByText('fiat.payin.events.v1')).toBeInTheDocument();
    expect(within(grid).getByText('TXN-FAIL-001')).toBeInTheDocument();
    expect(within(grid).getByText('1')).toBeInTheDocument();
  });

  it('renders error class via StatusBadge', () => {
    // arrange
    const entry = createDlqEntry({ error_class: 'PROCESSING_FAILED' });

    // act
    render(<DlqDetail dlqId={entry.id} initialData={entry} />);

    // assert
    const badge = screen.getByTestId('status-badge');
    expect(badge).toHaveTextContent('PROCESSING_FAILED');
  });

  it('renders error message block', () => {
    // arrange
    const entry = createDlqEntry({ error_message: 'Some detailed error' });

    // act
    render(<DlqDetail dlqId={entry.id} initialData={entry} />);

    // assert
    const blocks = screen.getAllByTestId('dlq-error-block');
    expect(blocks[0]).toHaveTextContent('Some detailed error');
  });

  it('renders event payload block', () => {
    // arrange
    const entry = createDlqEntry({ event_payload: '{"ref":"TXN-001"}' });

    // act
    render(<DlqDetail dlqId={entry.id} initialData={entry} />);

    // assert
    const blocks = screen.getAllByTestId('dlq-error-block');
    expect(blocks[1]).toHaveTextContent('{"ref":"TXN-001"}');
  });

  it('renders replay section with ReplayButton', () => {
    // arrange
    const entry = createDlqEntry({ id: 'DLQ-RPL', error_class: 'SINK_FAILED', retry_count: 0 });

    // act
    render(<DlqDetail dlqId={entry.id} initialData={entry} />);

    // assert
    const section = screen.getByTestId('dlq-replay-section');
    expect(within(section).getByTestId('replay-button-mock')).toHaveTextContent(
      'DLQ-RPL SINK_FAILED 0',
    );
  });

  it('renders replay section description text', () => {
    // arrange
    const entry = createDlqEntry();

    // act
    render(<DlqDetail dlqId={entry.id} initialData={entry} />);

    // assert
    const section = screen.getByTestId('dlq-replay-section');
    expect(
      within(section).getByText('Re-publish this event to the source topic for reprocessing.'),
    ).toBeInTheDocument();
  });
});
