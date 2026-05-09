import { HttpResponse, http } from 'msw';
import { setupServer } from 'msw/node';

export const server = setupServer(
  http.get('/api/auth/session', () => HttpResponse.json({ accessToken: 'test-access-token' })),
);
