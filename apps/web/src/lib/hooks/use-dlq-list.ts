'use client';

import { useQuery } from '@tanstack/react-query';
import { clientFetch } from '~/lib/api-client/client-fetch';
import type { CursorPage, DlqEntryDto } from '~/types/api';

export function useDlqList(cursor?: string, initialData?: CursorPage<DlqEntryDto>) {
  const params = cursor ? `?cursor=${encodeURIComponent(cursor)}` : '';
  return useQuery({
    queryKey: ['dlq', 'list', cursor],
    initialData,
    queryFn: () => clientFetch<CursorPage<DlqEntryDto>>(`/api/v1/admin/dlq${params}`),
    staleTime: 5_000,
  });
}
