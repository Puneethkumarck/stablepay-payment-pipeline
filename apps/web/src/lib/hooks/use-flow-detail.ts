'use client';

import { useQuery } from '@tanstack/react-query';
import { isTerminal } from '~/lib/terminal-status';
import type { FlowDto } from '~/types/api';

async function fetchFlow(id: string): Promise<FlowDto> {
  const response = await fetch(`/api/v1/flows/${encodeURIComponent(id)}`);
  if (!response.ok) {
    throw new Error('STBLPAY-1999');
  }
  return response.json() as Promise<FlowDto>;
}

export function useFlowDetail(id: string, initialData?: FlowDto) {
  return useQuery({
    queryKey: ['flow', id],
    initialData,
    queryFn: () => fetchFlow(id),
    refetchInterval: (query) => (isTerminal(query.state.data?.status) ? false : 3_000),
    staleTime: 1_000,
  });
}
