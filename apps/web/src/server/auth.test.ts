import type { JWT } from '@auth/core/jwt';
import type { Session, User } from 'next-auth';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import '~/types/auth';

function toBase64Url(input: string): string {
  return btoa(input).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function fakeJwt(payload: Record<string, unknown>): string {
  const header = toBase64Url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = toBase64Url(JSON.stringify(payload));
  return `${header}.${body}.fake-signature`;
}

const JWT_PAYLOAD = {
  sub: 'user-uuid-123',
  email: 'alice@stablepay.io',
  roles: ['ROLE_CUSTOMER'],
  customer_id: 'customer-uuid-456',
  iat: 1700000000,
  exp: 1700000900,
};

const LOGIN_RESPONSE = {
  access_token: fakeJwt(JWT_PAYLOAD),
  refresh_token: 'refresh-token-abc',
  expires_in: 900,
};

// We need to import the NextAuth config to test the callbacks.
// Since NextAuth() is called at module level, we mock the NextAuth default export
// to capture the config, then test callbacks directly.
let capturedConfig: Record<string, unknown>;

vi.mock('next-auth', () => ({
  default: (config: Record<string, unknown>) => {
    capturedConfig = config;
    return {
      handlers: { GET: vi.fn(), POST: vi.fn() },
      signIn: vi.fn(),
      signOut: vi.fn(),
      auth: vi.fn(),
    };
  },
}));

vi.mock('next-auth/providers/credentials', () => ({
  default: (config: Record<string, unknown>) => ({
    ...config,
    type: 'credentials',
    id: 'credentials',
  }),
}));

// Import after mocks are set up
await import('~/server/auth');

describe('authorize', () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-01-01T00:00:00Z'));
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    vi.useRealTimers();
  });

  function getAuthorize(): (
    credentials: Partial<Record<string, unknown>>,
    request: Request,
  ) => Promise<User | null> {
    const providers = capturedConfig.providers as Array<{
      authorize: (
        credentials: Partial<Record<string, unknown>>,
        request: Request,
      ) => Promise<User | null>;
    }>;
    const provider = providers[0];
    if (!provider) throw new Error('No credentials provider configured');
    return provider.authorize;
  }

  it('returns user with tokens on successful login', async () => {
    // arrange
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(LOGIN_RESPONSE),
    });
    const authorize = getAuthorize();

    // act
    const result = await authorize(
      { email: 'alice@stablepay.io', password: 'secret' },
      new Request('http://localhost'),
    );

    // assert
    expect(result).toEqual({
      id: 'user-uuid-123',
      email: 'alice@stablepay.io',
      roles: ['ROLE_CUSTOMER'],
      customerId: 'customer-uuid-456',
      accessToken: LOGIN_RESPONSE.access_token,
      refreshToken: 'refresh-token-abc',
      accessTokenExpiresAt: Date.now() + 900 * 1000,
    });
    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8081/api/v1/auth/login',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ email: 'alice@stablepay.io', password: 'secret' }),
      }),
    );
  });

  it('returns null on failed login', async () => {
    // arrange
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: () =>
        Promise.resolve({
          code: 'STBLPAY-1001',
          message: 'Invalid credentials',
        }),
    });
    const authorize = getAuthorize();

    // act
    const result = await authorize(
      { email: 'alice@stablepay.io', password: 'wrong' },
      new Request('http://localhost'),
    );

    // assert
    expect(result).toBeNull();
  });

  it('returns null when fetch throws', async () => {
    // arrange
    globalThis.fetch = vi.fn().mockRejectedValue(new Error('Network error'));
    const authorize = getAuthorize();

    // act
    const result = await authorize(
      { email: 'alice@stablepay.io', password: 'secret' },
      new Request('http://localhost'),
    );

    // assert
    expect(result).toBeNull();
  });
});

describe('jwt callback', () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2024-01-01T00:00:00Z'));
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
    vi.useRealTimers();
  });

  function getJwtCallback(): (params: {
    token: JWT;
    user: User;
    trigger?: 'signIn' | 'signUp' | 'update';
    account?: unknown;
    profile?: unknown;
    session?: unknown;
  }) => Promise<JWT> | JWT {
    const callbacks = capturedConfig.callbacks as {
      jwt: (params: {
        token: JWT;
        user: User;
        trigger?: 'signIn' | 'signUp' | 'update';
      }) => Promise<JWT> | JWT;
    };
    return callbacks.jwt;
  }

  it('copies user tokens to JWT on initial sign-in', () => {
    // arrange
    const jwtCallback = getJwtCallback();
    const token: JWT = { sub: undefined };
    const user: User = {
      id: 'user-uuid-123',
      email: 'alice@stablepay.io',
      roles: ['ROLE_CUSTOMER'],
      customerId: 'customer-uuid-456',
      accessToken: 'access-token-xyz',
      refreshToken: 'refresh-token-abc',
      accessTokenExpiresAt: Date.now() + 900_000,
    };

    // act
    const result = jwtCallback({ token, user, trigger: 'signIn' });

    // assert
    expect(result).toEqual({
      sub: 'user-uuid-123',
      accessToken: 'access-token-xyz',
      refreshToken: 'refresh-token-abc',
      accessTokenExpiresAt: Date.now() + 900_000,
      roles: ['ROLE_CUSTOMER'],
      email: 'alice@stablepay.io',
      customerId: 'customer-uuid-456',
    });
  });

  it('returns token unchanged when not near expiry', () => {
    // arrange
    const jwtCallback = getJwtCallback();
    const token: JWT = {
      sub: 'user-uuid-123',
      accessToken: 'access-token-xyz',
      refreshToken: 'refresh-token-abc',
      accessTokenExpiresAt: Date.now() + 300_000, // 5 minutes from now (> 60s buffer)
      roles: ['ROLE_CUSTOMER'],
      email: 'alice@stablepay.io',
      customerId: 'customer-uuid-456',
    };

    // act
    const result = jwtCallback({
      token,
      user: undefined as unknown as User,
      trigger: undefined,
    });

    // assert
    expect(result).toBe(token);
  });

  it('refreshes token when near expiry', async () => {
    // arrange
    const refreshedPayload = {
      ...JWT_PAYLOAD,
      iat: 1700001000,
      exp: 1700001900,
    };
    const refreshResponse = {
      access_token: fakeJwt(refreshedPayload),
      refresh_token: 'new-refresh-token',
      expires_in: 900,
    };
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(refreshResponse),
    });
    const jwtCallback = getJwtCallback();
    const token: JWT = {
      sub: 'user-uuid-123',
      accessToken: 'old-access-token',
      refreshToken: 'refresh-token-abc',
      accessTokenExpiresAt: Date.now() + 30_000, // 30 seconds — within 60s buffer
      roles: ['ROLE_CUSTOMER'],
      email: 'alice@stablepay.io',
      customerId: 'customer-uuid-456',
    };

    // act
    const result = await jwtCallback({
      token,
      user: undefined as unknown as User,
      trigger: undefined,
    });

    // assert
    expect(result).toEqual({
      sub: 'user-uuid-123',
      accessToken: refreshResponse.access_token,
      refreshToken: 'new-refresh-token',
      accessTokenExpiresAt: Date.now() + 900 * 1000,
      roles: ['ROLE_CUSTOMER'],
      email: 'alice@stablepay.io',
      customerId: 'customer-uuid-456',
      error: undefined,
    });
    expect(globalThis.fetch).toHaveBeenCalledWith(
      'http://localhost:8081/api/v1/auth/refresh',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ refresh_token: 'refresh-token-abc' }),
      }),
    );
  });

  it('sets error on refresh failure', async () => {
    // arrange
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
    });
    const jwtCallback = getJwtCallback();
    const token: JWT = {
      sub: 'user-uuid-123',
      accessToken: 'old-access-token',
      refreshToken: 'expired-refresh-token',
      accessTokenExpiresAt: Date.now() - 1000, // Already expired
      roles: ['ROLE_CUSTOMER'],
      email: 'alice@stablepay.io',
      customerId: 'customer-uuid-456',
    };

    // act
    const result = await jwtCallback({
      token,
      user: undefined as unknown as User,
      trigger: undefined,
    });

    // assert
    expect(result).toEqual(
      expect.objectContaining({
        error: 'RefreshAccessTokenError',
      }),
    );
  });
});

describe('session callback', () => {
  function getSessionCallback(): (params: { session: Session; token: JWT }) => Session {
    const callbacks = capturedConfig.callbacks as {
      session: (params: { session: Session; token: JWT }) => Session;
    };
    return callbacks.session;
  }

  it('exposes access token and roles on session', () => {
    // arrange
    const sessionCallback = getSessionCallback();
    const token: JWT = {
      sub: 'user-uuid-123',
      accessToken: 'access-token-xyz',
      refreshToken: 'refresh-token-abc',
      accessTokenExpiresAt: Date.now() + 900_000,
      roles: ['ROLE_CUSTOMER'],
      email: 'alice@stablepay.io',
      customerId: 'customer-uuid-456',
    };
    const session: Session = {
      user: { id: undefined, email: undefined, name: undefined, image: undefined },
      expires: new Date(Date.now() + 86400_000).toISOString(),
    };

    // act
    const result = sessionCallback({ session, token });

    // assert
    expect(result.accessToken).toBe('access-token-xyz');
    expect(result.user.roles).toEqual(['ROLE_CUSTOMER']);
    expect(result.user.email).toBe('alice@stablepay.io');
    expect(result.user.customerId).toBe('customer-uuid-456');
    expect(result.user.id).toBe('user-uuid-123');
    expect(result.error).toBeUndefined();
  });

  it('exposes error on session when present', () => {
    // arrange
    const sessionCallback = getSessionCallback();
    const token: JWT = {
      sub: 'user-uuid-123',
      accessToken: 'old-access-token',
      error: 'RefreshAccessTokenError',
      roles: ['ROLE_CUSTOMER'],
      email: 'alice@stablepay.io',
      customerId: 'customer-uuid-456',
    };
    const session: Session = {
      user: { id: undefined, email: undefined, name: undefined, image: undefined },
      expires: new Date(Date.now() + 86400_000).toISOString(),
    };

    // act
    const result = sessionCallback({ session, token });

    // assert
    expect(result.error).toBe('RefreshAccessTokenError');
  });
});
