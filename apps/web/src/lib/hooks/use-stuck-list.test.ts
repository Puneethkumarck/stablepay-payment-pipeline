import { waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createStuckPayment } from '~/test/fixtures/stuck';
import { server } from '~/test/msw-server';
import { renderHook } from '~/test/render';
import { useStuckList } from './use-stuck-list';

describe('useStuckList', () => {
  it('returns initial data immediately', () => {
    // arrange
    const stuck = [createStuckPayment()];

    // act
    const { result } = renderHook(() => useStuckList(stuck));

    // assert
    expect(result.current.data).toEqual(stuck);
  });

  it('fetches stuck list from the API', async () => {
    // arrange
    const stuck = [
      createStuckPayment({ transaction_ref: 'TXN-S1' }),
      createStuckPayment({ transaction_ref: 'TXN-S2' }),
    ];
    server.use(http.get('/api/v1/admin/stuck', () => HttpResponse.json(stuck)));

    // act
    const { result } = renderHook(() => useStuckList());

    // assert
    await waitFor(() => {
      expect(result.current.data).toHaveLength(2);
    });
  });

  it('returns empty array when no stuck payments', async () => {
    // arrange
    server.use(http.get('/api/v1/admin/stuck', () => HttpResponse.json([])));

    // act
    const { result } = renderHook(() => useStuckList());

    // assert
    await waitFor(() => {
      expect(result.current.data).toEqual([]);
    });
  });

  it('throws on API error', async () => {
    // arrange
    server.use(
      http.get('/api/v1/admin/stuck', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000' }, { status: 500 }),
      ),
    );

    // act
    const { result } = renderHook(() => useStuckList());

    // assert
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
    });
  });
});
