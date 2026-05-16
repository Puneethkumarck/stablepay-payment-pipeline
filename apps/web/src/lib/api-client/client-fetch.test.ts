import { HttpResponse, http } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';
import { server } from '~/test/msw-server';
import { clientFetch, resetTokenCache } from './client-fetch';

describe('clientFetch', () => {
  afterEach(() => {
    resetTokenCache();
  });

  it('fetches data with authorization header from session', async () => {
    // arrange
    let capturedAuth = '';
    server.use(
      http.get('/api/v1/test', ({ request }) => {
        capturedAuth = request.headers.get('Authorization') ?? '';
        return HttpResponse.json({ result: 'ok' });
      }),
    );

    // act
    const data = await clientFetch<{ result: string }>('/api/v1/test');

    // assert
    expect(data).toEqual({ result: 'ok' });
    expect(capturedAuth).toBe('Bearer test-access-token');
  });

  it('throws with error_code from API error response', async () => {
    // arrange
    server.use(
      http.get('/api/v1/failing', () =>
        HttpResponse.json({ error_code: 'STBLPAY-4040', message: 'Not found' }, { status: 404 }),
      ),
    );

    // act / assert
    await expect(clientFetch('/api/v1/failing')).rejects.toThrow('STBLPAY-4040');
  });

  it('throws STBLPAY-1999 when error response has no body', async () => {
    // arrange
    server.use(http.get('/api/v1/empty-error', () => new HttpResponse(null, { status: 500 })));

    // act / assert
    await expect(clientFetch('/api/v1/empty-error')).rejects.toThrow('STBLPAY-1999');
  });

  it('works without access token when session endpoint fails', async () => {
    // arrange
    server.use(
      http.get('/api/auth/session', () => new HttpResponse(null, { status: 500 })),
      http.get('/api/v1/public', ({ request }) => {
        const auth = request.headers.get('Authorization');
        return HttpResponse.json({ hasAuth: !!auth });
      }),
    );

    // act
    const data = await clientFetch<{ hasAuth: boolean }>('/api/v1/public');

    // assert
    expect(data).toEqual({ hasAuth: false });
  });
});
