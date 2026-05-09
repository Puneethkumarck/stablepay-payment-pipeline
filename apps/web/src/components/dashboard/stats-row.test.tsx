import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createDashboardStats } from '~/test/fixtures/dashboard';
import { render } from '~/test/render';
import { StatsRow } from './stats-row';

vi.mock('~/lib/hooks/use-dashboard-stats', () => ({
  useDashboardStats: vi.fn(),
}));

import { useDashboardStats } from '~/lib/hooks/use-dashboard-stats';

const mockUseDashboardStats = vi.mocked(useDashboardStats);

describe('StatsRow', () => {
  it('renders all 4 stat cards with data', () => {
    // arrange
    const stats = createDashboardStats({
      volume_24h: { amount: 1_250_000, currency: 'USD' },
      success_rate_24h: 0.95,
      dlq_count: 3,
      stuck_count: 1,
      transaction_count_24h: 42,
    });
    mockUseDashboardStats.mockReturnValue({ data: stats } as ReturnType<
      typeof useDashboardStats
    >);

    // act
    render(<StatsRow />);

    // assert
    expect(screen.getByText('Volume (24h)')).toBeInTheDocument();
    expect(screen.getByText('Success rate')).toBeInTheDocument();
    expect(screen.getByText('DLQ events')).toBeInTheDocument();
    expect(screen.getByText('Stuck payments')).toBeInTheDocument();
    expect(screen.getByText('$1.25')).toBeInTheDocument();
    expect(screen.getByText('95.0%')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('renders dash values when data is undefined', () => {
    // arrange
    mockUseDashboardStats.mockReturnValue({ data: undefined } as ReturnType<
      typeof useDashboardStats
    >);

    // act
    render(<StatsRow />);

    // assert
    const dashes = screen.getAllByText('—');
    expect(dashes).toHaveLength(4);
  });

  it('renders transaction count subtitle', () => {
    // arrange
    const stats = createDashboardStats({ transaction_count_24h: 42 });
    mockUseDashboardStats.mockReturnValue({ data: stats } as ReturnType<
      typeof useDashboardStats
    >);

    // act
    render(<StatsRow />);

    // assert
    expect(screen.getByText('42 transactions')).toBeInTheDocument();
  });
});
