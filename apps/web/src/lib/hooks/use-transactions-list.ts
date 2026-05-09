'use client';

import { useQuery } from '@tanstack/react-query';
import type { CursorPage, TransactionDto, TransactionListCriteria } from '~/types/api';

async function fetchTransactions(
  criteria: TransactionListCriteria,
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
  const response = await fetch(`/api/v1/transactions${query ? `?${query}` : ''}`);
  if (!response.ok) {
    throw new Error('STBLPAY-1999');
  }
  return response.json() as Promise<CursorPage<TransactionDto>>;
}

export function useTransactionsList(
  criteria: TransactionListCriteria = {},
  initialData?: CursorPage<TransactionDto>,
) {
  return useQuery({
    queryKey: ['transactions', criteria],
    initialData,
    queryFn: () => fetchTransactions(criteria),
    staleTime: 5_000,
  });
}
