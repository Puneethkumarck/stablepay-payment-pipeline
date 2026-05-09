import { waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createDlqSummary } from '~/test/fixtures/dlq';
import { server } from '~/test/msw-server';
import { renderHook } from '~/test/render';
import { useDlqSummary } from './use-dlq-summary';

describe('useDlqSummary', () => {
  it('returns initial data immediately', () => {
    // arrange
    const summary = createDlqSummary({ total: 5 });

    // act
    const { result } = renderHook(() => useDlqSummary(summary));

    // assert
    expect(result.current.data).toEqual(summary);
  });

  it('fetches DLQ summary from the API', async () => {
    // arrange
    const summary = createDlqSummary({ total: 8 });
    server.use(http.get('/api/v1/admin/dlq/summary', () => HttpResponse.json(summary)));

    // act
    const { result } = renderHook(() => useDlqSummary());

    // assert
    await waitFor(() => {
      expect(result.current.data?.total).toBe(8);
    });
  });

  it('throws on API error', async () => {
    // arrange
    server.use(
      http.get('/api/v1/admin/dlq/summary', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000' }, { status: 500 }),
      ),
    );

    // act
    const { result } = renderHook(() => useDlqSummary());

    // assert
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
    });
  });
});
