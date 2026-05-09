import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createDlqEntry, createDlqPage, createDlqSummary } from '~/test/fixtures/dlq';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { DlqList } from './dlq-list';

const pushMock = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock }),
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
    <button data-testid={`replay-${dlqId}`}>
      Replay {errorClass} {retryCount}
    </button>
  ),
}));

describe('DlqList', () => {
  beforeEach(() => {
    pushMock.mockClear();
    server.resetHandlers();
  });

  it('renders page header with title', () => {
    // arrange
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(createDlqPage())),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())),
    );

    // act
    render(<DlqList />);

    // assert
    expect(screen.getByText('DLQ Inspector')).toBeInTheDocument();
    expect(screen.getByText('Admin')).toBeInTheDocument();
  });

  it('renders 4-class breakdown cards with counts', async () => {
    // arrange
    const summary = createDlqSummary({
      by_error_class: {
        SCHEMA_INVALID: 5,
        PROCESSING_FAILED: 3,
        SINK_FAILURE: 2,
        LATE_EVENT: 1,
      },
    });
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(createDlqPage())),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(summary)),
    );

    // act
    render(<DlqList />);

    // assert
    const breakdown = screen.getByTestId('dlq-breakdown');
    await vi.waitFor(() => {
      expect(within(breakdown).getByText('5')).toBeInTheDocument();
    });
    expect(within(breakdown).getByText('3')).toBeInTheDocument();
    expect(within(breakdown).getByText('2')).toBeInTheDocument();
    expect(within(breakdown).getByText('1')).toBeInTheDocument();
  });

  it('renders breakdown cards with zero when no data', () => {
    // arrange
    const summary = createDlqSummary({ by_error_class: {} });
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(createDlqPage())),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(summary)),
    );

    // act
    render(<DlqList />);

    // assert
    const breakdown = screen.getByTestId('dlq-breakdown');
    const zeros = within(breakdown).getAllByText('0');
    expect(zeros).toHaveLength(4);
  });

  it('renders search bar', () => {
    // arrange
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(createDlqPage())),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())),
    );

    // act
    render(<DlqList />);

    // assert
    expect(screen.getByTestId('search-bar')).toBeInTheDocument();
  });

  it('renders DLQ entries in data table', async () => {
    // arrange
    const page = createDlqPage({
      data: [
        createDlqEntry({ id: 'DLQ-100', error_class: 'PROCESSING_FAILED', topic: 'fiat.payin.events.v1' }),
        createDlqEntry({ id: 'DLQ-200', error_class: 'LATE_EVENT', topic: 'crypto.payout.events.v1' }),
      ],
    });
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(page)),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())),
    );

    // act
    render(<DlqList />);

    // assert
    expect(await screen.findByText('DLQ-100')).toBeInTheDocument();
    expect(screen.getByText('DLQ-200')).toBeInTheDocument();
  });

  it('navigates to detail page on row click', async () => {
    // arrange
    const user = userEvent.setup();
    const page = createDlqPage({
      data: [createDlqEntry({ id: 'DLQ-NAV' })],
    });
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(page)),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())),
    );
    render(<DlqList />);

    // act
    const cell = await screen.findByText('DLQ-NAV');
    const row = cell.closest('tr');
    expect(row).toBeTruthy();
    await user.click(row as HTMLElement);

    // assert
    expect(pushMock).toHaveBeenCalledWith('/admin/dlq/DLQ-NAV');
  });

  it('filters rows by search input', async () => {
    // arrange
    const user = userEvent.setup();
    const page = createDlqPage({
      data: [
        createDlqEntry({ id: 'DLQ-MATCH', error_class: 'PROCESSING_FAILED' }),
        createDlqEntry({ id: 'DLQ-NOPE', error_class: 'LATE_EVENT' }),
      ],
    });
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(page)),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())),
    );
    render(<DlqList />);

    // act
    const input = screen.getByPlaceholderText('Search by DLQ ID, error class, or topic…');
    await user.type(input, 'MATCH');

    // assert — debounced, so wait
    await vi.waitFor(() => {
      expect(screen.getByText('DLQ-MATCH')).toBeInTheDocument();
      expect(screen.queryByText('DLQ-NOPE')).not.toBeInTheDocument();
    });
  });

  it('shows empty state when no DLQ entries', async () => {
    // arrange
    const page = createDlqPage({ data: [] });
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(page)),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())),
    );

    // act
    render(<DlqList />);

    // assert
    expect(await screen.findByText('No DLQ entries')).toBeInTheDocument();
  });

  it('renders replay button per row via ReplayButton mock', async () => {
    // arrange
    const page = createDlqPage({
      data: [createDlqEntry({ id: 'DLQ-RPL', error_class: 'PROCESSING_FAILED', retry_count: 1 })],
    });
    server.use(
      http.get('/api/v1/admin/dlq', () => HttpResponse.json(page)),
      http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())),
    );

    // act
    render(<DlqList />);

    // assert
    expect(await screen.findByTestId('replay-DLQ-RPL')).toBeInTheDocument();
  });
});
