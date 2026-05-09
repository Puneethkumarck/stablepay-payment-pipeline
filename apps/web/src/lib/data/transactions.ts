import 'server-only';
import { apiFetch, apiFetchNullable } from '~/lib/api-client/fetcher';
import type { CursorPage, TransactionDto, TransactionListCriteria } from '~/types/api';

export async function fetchTransactionsList(
  criteria: TransactionListCriteria = {},
): Promise<CursorPage<TransactionDto>> {
  const params = new URLSearchParams();
  if (criteria.cursor) params.set('cursor', criteria.cursor);
  if (criteria.limit) params.set('limit', String(criteria.limit));
  if (criteria.status) params.set('status', criteria.status);
  if (criteria.direction) params.set('direction', criteria.direction);
  if (criteria.type) params.set('type', criteria.type);
  if (criteria.search) params.set('search', criteria.search);
  if (criteria.from) params.set('from', criteria.from);
  if (criteria.to) params.set('to', criteria.to);

  const query = params.toString();
  return apiFetch<CursorPage<TransactionDto>>(`/api/v1/transactions${query ? `?${query}` : ''}`);
}

export async function fetchTransaction(ref: string): Promise<TransactionDto | null> {
  return apiFetchNullable<TransactionDto>(`/api/v1/transactions/${encodeURIComponent(ref)}`);
}

export async function fetchAdminTransaction(ref: string): Promise<TransactionDto | null> {
  return apiFetchNullable<TransactionDto>(`/api/v1/admin/transactions/${encodeURIComponent(ref)}`);
}
