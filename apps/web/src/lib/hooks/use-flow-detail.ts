'use client';

import { useQuery } from '@tanstack/react-query';
import { clientFetch } from '~/lib/api-client/client-fetch';
import { isTerminal } from '~/lib/terminal-status';
import type { FlowDto } from '~/types/api';

export function useFlowDetail(id: string, initialData?: FlowDto) {
  return useQuery({
    queryKey: ['flow', id],
    initialData,
    queryFn: () => clientFetch<FlowDto>(`/api/v1/flows/${encodeURIComponent(id)}`),
    refetchInterval: (query) => (isTerminal(query.state.data?.status) ? false : 3_000),
    staleTime: 1_000,
  });
}
