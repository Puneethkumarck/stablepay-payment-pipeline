'use client';

import { useQuery } from '@tanstack/react-query';
import { isTerminal } from '~/lib/terminal-status';
import type { TransactionDto } from '~/types/api';

async function fetchTransaction(ref: string): Promise<TransactionDto> {
  const response = await fetch(`/api/v1/transactions/${encodeURIComponent(ref)}`);
  if (!response.ok) {
    throw new Error(`STBLPAY-1999`);
  }
  return response.json() as Promise<TransactionDto>;
}

export function useTransactionDetail(ref: string, initialData?: TransactionDto) {
  return useQuery({
    queryKey: ['transaction', ref],
    initialData,
    queryFn: () => fetchTransaction(ref),
    refetchInterval: (query) => (isTerminal(query.state.data?.internal_status) ? false : 3_000),
    staleTime: 1_000,
  });
}
