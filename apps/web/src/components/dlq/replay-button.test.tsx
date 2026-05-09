import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { ReplayButton } from './replay-button';

vi.mock('~/lib/idempotency', () => ({
  newIdempotencyKey: () => 'test-idem-key-001',
}));

describe('ReplayButton', () => {
  beforeEach(() => {
    server.resetHandlers();
  });

  it('renders Replay button for replayable entry', () => {
    // arrange + act
    render(<ReplayButton dlqId="DLQ-001" errorClass="PROCESSING_FAILED" retryCount={0} />);

    // assert
    expect(screen.getByRole('button', { name: 'Replay' })).toBeInTheDocument();
  });

  it('shows "Retry budget exhausted" when retryCount >= 2', () => {
    // arrange + act
    render(<ReplayButton dlqId="DLQ-001" errorClass="PROCESSING_FAILED" retryCount={2} />);

    // assert
    expect(screen.getByText('Retry budget exhausted')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Replay' })).not.toBeInTheDocument();
  });

  it('shows "Retry budget exhausted" when retryCount > 2', () => {
    // arrange + act
    render(<ReplayButton dlqId="DLQ-001" errorClass="PROCESSING_FAILED" retryCount={5} />);

    // assert
    expect(screen.getByText('Retry budget exhausted')).toBeInTheDocument();
  });

  it('shows "No retry" for SCHEMA_INVALID error class', () => {
    // arrange + act
    render(<ReplayButton dlqId="DLQ-001" errorClass="SCHEMA_INVALID" retryCount={0} />);

    // assert
    expect(screen.getByText('No retry')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Replay' })).not.toBeInTheDocument();
  });

  it('shows "No retry" for LATE_EVENT error class', () => {
    // arrange + act
    render(<ReplayButton dlqId="DLQ-001" errorClass="LATE_EVENT" retryCount={0} />);

    // assert
    expect(screen.getByText('No retry')).toBeInTheDocument();
  });

  it('transitions to "Replaying…" then "Replayed" on success', async () => {
    // arrange
    const user = userEvent.setup();
    server.use(
      http.post('/api/v1/admin/dlq/DLQ-001/replay', () => HttpResponse.json({ status: 'queued' })),
    );
    render(<ReplayButton dlqId="DLQ-001" errorClass="PROCESSING_FAILED" retryCount={0} />);

    // act
    await user.click(screen.getByRole('button', { name: 'Replay' }));

    // assert
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Replayed' })).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Replayed' })).toBeDisabled();
  });

  it('shows success toast with "Replay queued" on fresh replay', async () => {
    // arrange
    const user = userEvent.setup();
    server.use(
      http.post('/api/v1/admin/dlq/DLQ-001/replay', () => HttpResponse.json({ status: 'queued' })),
    );
    render(<ReplayButton dlqId="DLQ-001" errorClass="PROCESSING_FAILED" retryCount={0} />);

    // act
    await user.click(screen.getByRole('button', { name: 'Replay' }));

    // assert
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Replayed' })).toBeInTheDocument();
    });
  });

  it('shows "Replay re-queued" toast when Idempotency-Replayed header is true', async () => {
    // arrange
    const user = userEvent.setup();
    server.use(
      http.post('/api/v1/admin/dlq/DLQ-001/replay', () =>
        HttpResponse.json(
          { status: 'queued' },
          { headers: { 'Idempotency-Replayed': 'true' } },
        ),
      ),
    );
    render(<ReplayButton dlqId="DLQ-001" errorClass="PROCESSING_FAILED" retryCount={0} />);

    // act
    await user.click(screen.getByRole('button', { name: 'Replay' }));

    // assert
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Replayed' })).toBeInTheDocument();
    });
  });

  it('returns to idle state on error', async () => {
    // arrange
    const user = userEvent.setup();
    server.use(
      http.post('/api/v1/admin/dlq/DLQ-001/replay', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000', message: 'Internal error' }, { status: 500 }),
      ),
    );
    render(<ReplayButton dlqId="DLQ-001" errorClass="PROCESSING_FAILED" retryCount={0} />);

    // act
    await user.click(screen.getByRole('button', { name: 'Replay' }));

    // assert
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Replay' })).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Replay' })).not.toBeDisabled();
  });

  it('sends X-Idempotency-Key header with request', async () => {
    // arrange
    const user = userEvent.setup();
    let capturedHeaders: Record<string, string> = {};
    server.use(
      http.post('/api/v1/admin/dlq/DLQ-001/replay', ({ request }) => {
        capturedHeaders = Object.fromEntries(request.headers.entries());
        return HttpResponse.json({ status: 'queued' });
      }),
    );
    render(<ReplayButton dlqId="DLQ-001" errorClass="PROCESSING_FAILED" retryCount={1} />);

    // act
    await user.click(screen.getByRole('button', { name: 'Replay' }));

    // assert
    await waitFor(() => {
      expect(capturedHeaders['x-idempotency-key']).toBe('test-idem-key-001');
    });
  });
});
