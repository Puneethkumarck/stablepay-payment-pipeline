'use client';

import { useQuery } from '@tanstack/react-query';
import type { CursorPage, DlqEntryDto } from '~/types/api';

async function fetchDlqList(cursor?: string): Promise<CursorPage<DlqEntryDto>> {
  const params = cursor ? `?cursor=${encodeURIComponent(cursor)}` : '';
  const response = await fetch(`/api/v1/admin/dlq${params}`);
  if (!response.ok) {
    throw new Error('STBLPAY-1999');
  }
  return response.json() as Promise<CursorPage<DlqEntryDto>>;
}

export function useDlqList(cursor?: string, initialData?: CursorPage<DlqEntryDto>) {
  return useQuery({
    queryKey: ['dlq', cursor],
    initialData,
    queryFn: () => fetchDlqList(cursor),
    staleTime: 5_000,
  });
}
