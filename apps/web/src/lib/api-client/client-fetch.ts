let cachedToken: string | null = null;
let tokenExpiresAt = 0;

async function getAccessToken(): Promise<string | undefined> {
  if (cachedToken && Date.now() < tokenExpiresAt) {
    return cachedToken;
  }

  const response = await fetch('/api/auth/session');
  if (!response.ok) return undefined;

  const session = (await response.json()) as { accessToken?: string };
  if (session.accessToken) {
    cachedToken = session.accessToken;
    tokenExpiresAt = Date.now() + 30_000;
    return cachedToken;
  }

  return undefined;
}

export function resetTokenCache() {
  cachedToken = null;
  tokenExpiresAt = 0;
}

interface ClientFetchOptions {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
}

export interface ClientFetchResult<T> {
  data: T;
  response: Response;
}

export async function clientFetchWithResponse<T>(
  path: string,
  options: ClientFetchOptions = {},
): Promise<ClientFetchResult<T>> {
  const token = await getAccessToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...options.headers,
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(path, {
    method: options.method ?? 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as {
      error_code?: string;
      message?: string;
    } | null;
    throw new Error(error?.error_code ?? 'STBLPAY-1999');
  }

  const data = (await response.json()) as T;
  return { data, response };
}

export async function clientFetch<T>(path: string): Promise<T> {
  const token = await getAccessToken();
  const headers: Record<string, string> = {
    Accept: 'application/json',
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(path, { headers });

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as {
      error_code?: string;
      message?: string;
    } | null;
    throw new Error(error?.error_code ?? 'STBLPAY-1999');
  }

  return (await response.json()) as T;
}
