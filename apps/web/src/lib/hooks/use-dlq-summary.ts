'use client';

import { useQuery } from '@tanstack/react-query';
import { clientFetch } from '~/lib/api-client/client-fetch';
import type { DlqSummaryDto } from '~/types/api';

export function useDlqSummary(initialData?: DlqSummaryDto) {
  return useQuery({
    queryKey: ['dlq', 'summary'],
    initialData,
    queryFn: () => clientFetch<DlqSummaryDto>('/api/v1/admin/dlq/summary'),
    staleTime: 10_000,
  });
}
