import { waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createDlqPage } from '~/test/fixtures/dlq';
import { server } from '~/test/msw-server';
import { renderHook } from '~/test/render';
import { useDlqList } from './use-dlq-list';

describe('useDlqList', () => {
  it('returns initial data immediately', () => {
    // arrange
    const page = createDlqPage();

    // act
    const { result } = renderHook(() => useDlqList(undefined, page));

    // assert
    expect(result.current.data).toEqual(page);
  });

  it('fetches DLQ list from the API', async () => {
    // arrange
    const page = createDlqPage({ has_more: true, next_cursor: 'cursor-1' });
    server.use(http.get('/api/v1/admin/dlq', () => HttpResponse.json(page)));

    // act
    const { result } = renderHook(() => useDlqList());

    // assert
    await waitFor(() => {
      expect(result.current.data?.has_more).toBe(true);
    });
  });

  it('passes cursor parameter to the API', async () => {
    // arrange
    let capturedUrl = '';
    server.use(
      http.get('/api/v1/admin/dlq', ({ request }) => {
        capturedUrl = request.url;
        return HttpResponse.json(createDlqPage());
      }),
    );

    // act
    const { result } = renderHook(() => useDlqList('page-2'));

    // assert
    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(capturedUrl).toContain('cursor=page-2');
  });

  it('throws on API error', async () => {
    // arrange
    server.use(
      http.get('/api/v1/admin/dlq', () =>
        HttpResponse.json({ error_code: 'STBLPAY-4030' }, { status: 403 }),
      ),
    );

    // act
    const { result } = renderHook(() => useDlqList());

    // assert
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
    });
  });
});
