import { screen, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createDlqSummary } from '~/test/fixtures/dlq';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { DlqSummaryCard } from './dlq-summary-card';

describe('DlqSummaryCard', () => {
  it('renders all 4 error class labels', async () => {
    // arrange
    server.use(http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())));

    // act
    render(<DlqSummaryCard />);

    // assert
    expect(await screen.findByText('Schema invalid')).toBeInTheDocument();
    expect(screen.getByText('Proc. failed')).toBeInTheDocument();
    expect(screen.getByText('Sink failure')).toBeInTheDocument();
    expect(screen.getByText('Late event')).toBeInTheDocument();
  });

  it('renders counts from the summary data', async () => {
    // arrange
    const summary = createDlqSummary({
      by_error_class: {
        SCHEMA_INVALID: 5,
        PROCESSING_FAILED: 2,
        SINK_FAILURE: 0,
        LATE_EVENT: 1,
      },
    });
    server.use(http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(summary)));

    // act
    render(<DlqSummaryCard />);

    // assert
    expect(await screen.findByText('5')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('renders DLQ inspector link', async () => {
    // arrange
    server.use(http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(createDlqSummary())));

    // act
    render(<DlqSummaryCard />);

    // assert
    expect(await screen.findByRole('link', { name: /open dlq inspector/i })).toHaveAttribute(
      'href',
      '/admin/dlq',
    );
  });

  it('renders zero counts when API returns error', async () => {
    // arrange
    server.use(
      http.get('/api/v1/admin/dlq/summary', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000' }, { status: 500 }),
      ),
    );

    // act
    render(<DlqSummaryCard />);

    // assert
    await waitFor(() => {
      expect(screen.getAllByText('0')).toHaveLength(4);
    });
  });
});
