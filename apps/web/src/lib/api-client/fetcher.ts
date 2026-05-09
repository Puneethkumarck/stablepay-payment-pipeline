import 'server-only';
import { auth } from '~/server/auth';
import type { ApiError } from '~/types/api';

const BASE_URL = process.env.STABLEPAY_API_INTERNAL_URL ?? 'http://apps-api:8080';

export class ApiResponseError extends Error {
  constructor(
    public readonly errorCode: string,
    message: string,
    public readonly status: number,
  ) {
    super(message);
    this.name = 'ApiResponseError';
  }
}

interface FetchOptions {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
}

async function getAuthHeader(): Promise<string | undefined> {
  const session = await auth();
  return session?.accessToken ? `Bearer ${session.accessToken}` : undefined;
}

export async function apiFetch<T>(path: string, options: FetchOptions = {}): Promise<T> {
  const authorization = await getAuthHeader();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...options.headers,
  };
  if (authorization) {
    headers.Authorization = authorization;
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ApiError | null;
    throw new ApiResponseError(
      error?.error_code ?? 'STBLPAY-1999',
      error?.message ?? `API request failed with status ${response.status}`,
      response.status,
    );
  }

  return (await response.json()) as T;
}

export async function apiFetchNullable<T>(
  path: string,
  options: FetchOptions = {},
): Promise<T | null> {
  const authorization = await getAuthHeader();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...options.headers,
  };
  if (authorization) {
    headers.Authorization = authorization;
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 404) {
    return null;
  }

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ApiError | null;
    throw new ApiResponseError(
      error?.error_code ?? 'STBLPAY-1999',
      error?.message ?? `API request failed with status ${response.status}`,
      response.status,
    );
  }

  return (await response.json()) as T;
}
