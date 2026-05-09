'use client';

import { useQuery } from '@tanstack/react-query';
import { clientFetch } from '~/lib/api-client/client-fetch';
import type { CursorPage, TransactionDto, TransactionListCriteria } from '~/types/api';

function buildTransactionQuery(criteria: TransactionListCriteria): string {
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
  return `/api/v1/transactions${query ? `?${query}` : ''}`;
}

export function useTransactionsList(
  criteria: TransactionListCriteria = {},
  initialData?: CursorPage<TransactionDto>,
) {
  return useQuery({
    queryKey: ['transactions', criteria],
    initialData,
    queryFn: () => clientFetch<CursorPage<TransactionDto>>(buildTransactionQuery(criteria)),
    staleTime: 5_000,
  });
}
