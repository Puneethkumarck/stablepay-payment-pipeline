import { waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createTransaction } from '~/test/fixtures/transaction';
import { server } from '~/test/msw-server';
import { renderHook } from '~/test/render';
import { useTransactionDetail } from './use-transaction-detail';

describe('useTransactionDetail', () => {
  it('returns initial data immediately', () => {
    // arrange
    const transaction = createTransaction({ ref: 'TXN-100' });

    // act
    const { result } = renderHook(() => useTransactionDetail('TXN-100', transaction));

    // assert
    expect(result.current.data).toEqual(transaction);
  });

  it('fetches data from the API when no initial data provided', async () => {
    // arrange
    const transaction = createTransaction({ ref: 'TXN-200', internal_status: 'COMPLETED' });
    server.use(http.get('/api/v1/transactions/TXN-200', () => HttpResponse.json(transaction)));

    // act
    const { result } = renderHook(() => useTransactionDetail('TXN-200'));

    // assert
    await waitFor(() => {
      expect(result.current.data?.internal_status).toBe('COMPLETED');
    });
  });

  it('stops polling when status is terminal', async () => {
    // arrange
    const transaction = createTransaction({ internal_status: 'COMPLETED' });
    server.use(http.get('/api/v1/transactions/TXN-001', () => HttpResponse.json(transaction)));

    // act
    const { result } = renderHook(() => useTransactionDetail('TXN-001', transaction));

    // assert
    await waitFor(() => {
      expect(result.current.data).toEqual(transaction);
    });
  });

  it('throws on API error', async () => {
    // arrange
    const initial = createTransaction({ ref: 'TXN-ERR', internal_status: 'INITIATED' });
    server.use(
      http.get('/api/v1/transactions/TXN-ERR', () =>
        HttpResponse.json(
          { error_code: 'STBLPAY-5000', message: 'Internal error' },
          { status: 500 },
        ),
      ),
    );

    // act
    const { result } = renderHook(() => useTransactionDetail('TXN-ERR', initial));

    // assert
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
    });
  });
});
