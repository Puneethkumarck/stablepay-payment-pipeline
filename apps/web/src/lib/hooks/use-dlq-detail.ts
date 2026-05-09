'use client';

import { useQuery } from '@tanstack/react-query';
import type { DlqEntryDto } from '~/types/api';

async function fetchDlqEntry(id: string): Promise<DlqEntryDto> {
  const response = await fetch(`/api/v1/admin/dlq/${encodeURIComponent(id)}`);
  if (!response.ok) {
    throw new Error('STBLPAY-1999');
  }
  return response.json() as Promise<DlqEntryDto>;
}

export function useDlqDetail(id: string, initialData?: DlqEntryDto) {
  return useQuery({
    queryKey: ['dlq', id],
    initialData,
    queryFn: () => fetchDlqEntry(id),
    staleTime: 5_000,
  });
}
