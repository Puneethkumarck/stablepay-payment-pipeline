'use server';

import { AuthError } from 'next-auth';
import { NEXT_URL_ALLOWLIST } from '~/middleware';
import { signIn } from '~/server/auth';

export type LoginError = 'invalid_credentials' | 'account_locked' | 'rate_limited' | 'server_error';

export interface LoginResult {
  error?: LoginError;
}

const AUTH_API_URL = process.env.AUTH_API_URL ?? 'http://localhost:8081';
const AUTH_API_TIMEOUT_MS = 5_000;

export async function login(
  email: string,
  password: string,
  redirectTo: string,
): Promise<LoginResult> {
  const safeRedirect = NEXT_URL_ALLOWLIST.test(redirectTo) ? redirectTo : '/';

  try {
    let probe: Response;
    try {
      probe = await fetch(`${AUTH_API_URL}/api/v1/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        signal: AbortSignal.timeout(AUTH_API_TIMEOUT_MS),
        body: JSON.stringify({ email, password }),
      });
    } catch {
      return { error: 'server_error' };
    }

    if (!probe.ok) {
      if (probe.status === 401) return { error: 'invalid_credentials' };
      if (probe.status === 423) return { error: 'account_locked' };
      if (probe.status === 429) return { error: 'rate_limited' };
      return { error: 'server_error' };
    }

    await signIn('credentials', { email, password, redirectTo: safeRedirect });
    return {};
  } catch (error) {
    if (error instanceof AuthError && error.type === 'CredentialsSignin') {
      return { error: 'invalid_credentials' };
    }
    throw error;
  }
}
