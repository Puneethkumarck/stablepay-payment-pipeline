'use client';

import { useQuery } from '@tanstack/react-query';
import { clientFetch } from '~/lib/api-client/client-fetch';
import { isTerminal } from '~/lib/terminal-status';
import type { TransactionDto } from '~/types/api';

export function useTransactionDetail(ref: string, initialData?: TransactionDto) {
  return useQuery({
    queryKey: ['transaction', ref],
    initialData,
    queryFn: () =>
      clientFetch<TransactionDto>(`/api/v1/transactions/${encodeURIComponent(ref)}`),
    refetchInterval: (query) => (isTerminal(query.state.data?.internal_status) ? false : 3_000),
    staleTime: 1_000,
  });
}
