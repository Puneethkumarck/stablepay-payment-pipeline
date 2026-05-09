import { waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createDlqEntry } from '~/test/fixtures/dlq';
import { server } from '~/test/msw-server';
import { renderHook } from '~/test/render';
import { useDlqDetail } from './use-dlq-detail';

describe('useDlqDetail', () => {
  it('returns initial data immediately', () => {
    // arrange
    const entry = createDlqEntry({ id: 'DLQ-100' });

    // act
    const { result } = renderHook(() => useDlqDetail('DLQ-100', entry));

    // assert
    expect(result.current.data).toEqual(entry);
  });

  it('fetches DLQ entry from the API when no initial data provided', async () => {
    // arrange
    const entry = createDlqEntry({ id: 'DLQ-200', retry_count: 1 });
    server.use(http.get('/api/v1/admin/dlq/DLQ-200', () => HttpResponse.json(entry)));

    // act
    const { result } = renderHook(() => useDlqDetail('DLQ-200'));

    // assert
    await waitFor(() => {
      expect(result.current.data?.retry_count).toBe(1);
    });
  });

  it('throws on API error', async () => {
    // arrange
    const initial = createDlqEntry({ id: 'DLQ-ERR' });
    server.use(
      http.get('/api/v1/admin/dlq/DLQ-ERR', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000' }, { status: 500 }),
      ),
    );

    // act
    const { result } = renderHook(() => useDlqDetail('DLQ-ERR', initial));

    // assert
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
    });
  });
});
