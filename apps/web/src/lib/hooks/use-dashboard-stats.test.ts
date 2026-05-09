import { waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createDashboardStats } from '~/test/fixtures/dashboard';
import { server } from '~/test/msw-server';
import { renderHook } from '~/test/render';
import { useDashboardStats } from './use-dashboard-stats';

describe('useDashboardStats', () => {
  it('returns initial data immediately', () => {
    // arrange
    const stats = createDashboardStats();

    // act
    const { result } = renderHook(() => useDashboardStats(stats));

    // assert
    expect(result.current.data).toEqual(stats);
  });

  it('fetches dashboard stats from the API', async () => {
    // arrange
    const stats = createDashboardStats({ transaction_count_24h: 99 });
    server.use(http.get('/api/v1/dashboard/stats', () => HttpResponse.json(stats)));

    // act
    const { result } = renderHook(() => useDashboardStats());

    // assert
    await waitFor(() => {
      expect(result.current.data?.transaction_count_24h).toBe(99);
    });
  });

  it('throws on API error', async () => {
    // arrange
    server.use(
      http.get('/api/v1/dashboard/stats', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000' }, { status: 500 }),
      ),
    );

    // act
    const { result } = renderHook(() => useDashboardStats());

    // assert
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
    });
  });
});
