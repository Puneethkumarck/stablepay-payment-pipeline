import { waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createTransactionPage } from '~/test/fixtures/transaction';
import { server } from '~/test/msw-server';
import { renderHook } from '~/test/render';
import { useTransactionsList } from './use-transactions-list';

describe('useTransactionsList', () => {
  it('returns initial data immediately', () => {
    // arrange
    const page = createTransactionPage();

    // act
    const { result } = renderHook(() => useTransactionsList({}, page));

    // assert
    expect(result.current.data).toEqual(page);
  });

  it('fetches transaction list from the API', async () => {
    // arrange
    const page = createTransactionPage({ has_more: true, next_cursor: 'abc' });
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(page)));

    // act
    const { result } = renderHook(() => useTransactionsList());

    // assert
    await waitFor(() => {
      expect(result.current.data?.has_more).toBe(true);
    });
  });

  it('passes query parameters to the API', async () => {
    // arrange
    let capturedUrl = '';
    server.use(
      http.get('/api/v1/transactions', ({ request }) => {
        capturedUrl = request.url;
        return HttpResponse.json(createTransactionPage());
      }),
    );

    // act
    const { result } = renderHook(() => useTransactionsList({ status: 'COMPLETED', limit: 10 }));

    // assert
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(capturedUrl).toContain('status=COMPLETED');
    expect(capturedUrl).toContain('limit=10');
  });

  it('throws on API error', async () => {
    // arrange
    server.use(
      http.get('/api/v1/transactions', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000' }, { status: 500 }),
      ),
    );

    // act
    const { result } = renderHook(() => useTransactionsList());

    // assert
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
    });
  });
});
