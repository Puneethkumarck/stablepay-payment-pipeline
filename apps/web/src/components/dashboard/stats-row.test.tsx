import { screen, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createDashboardStats } from '~/test/fixtures/dashboard';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { StatsRow } from './stats-row';

describe('StatsRow', () => {
  it('renders all 4 stat cards with data', async () => {
    // arrange
    const stats = createDashboardStats({
      volume_24h: { amount: 1_250_000, currency: 'USD' },
      success_rate_24h: 0.95,
      dlq_count: 3,
      stuck_count: 1,
      transaction_count_24h: 42,
    });
    server.use(http.get('/api/v1/dashboard/stats', () => HttpResponse.json(stats)));

    // act
    render(<StatsRow />);

    // assert
    expect(await screen.findByText('$1.25')).toBeInTheDocument();
    expect(screen.getByText('Volume (24h)')).toBeInTheDocument();
    expect(screen.getByText('Success rate')).toBeInTheDocument();
    expect(screen.getByText('DLQ events')).toBeInTheDocument();
    expect(screen.getByText('Stuck payments')).toBeInTheDocument();
    expect(screen.getByText('95.0%')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('renders dash values when API returns error', async () => {
    // arrange
    server.use(
      http.get('/api/v1/dashboard/stats', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000' }, { status: 500 }),
      ),
    );

    // act
    render(<StatsRow />);

    // assert
    await waitFor(() => {
      expect(screen.getAllByText('—')).toHaveLength(4);
    });
  });

  it('renders transaction count subtitle', async () => {
    // arrange
    const stats = createDashboardStats({ transaction_count_24h: 42 });
    server.use(http.get('/api/v1/dashboard/stats', () => HttpResponse.json(stats)));

    // act
    render(<StatsRow />);

    // assert
    expect(await screen.findByText('42 transactions')).toBeInTheDocument();
  });
});
