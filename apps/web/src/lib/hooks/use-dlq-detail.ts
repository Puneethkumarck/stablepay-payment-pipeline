'use client';

import { useQuery } from '@tanstack/react-query';
import { clientFetch } from '~/lib/api-client/client-fetch';
import type { DlqEntryDto } from '~/types/api';

export function useDlqDetail(id: string, initialData?: DlqEntryDto) {
  return useQuery({
    queryKey: ['dlq', 'detail', id],
    initialData,
    queryFn: () => clientFetch<DlqEntryDto>(`/api/v1/admin/dlq/${encodeURIComponent(id)}`),
    staleTime: 5_000,
  });
}
