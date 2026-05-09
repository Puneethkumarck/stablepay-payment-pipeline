'use client';

import { useQuery } from '@tanstack/react-query';
import { clientFetch } from '~/lib/api-client/client-fetch';
import type { StuckPaymentDto } from '~/types/api';

export function useStuckList(initialData?: StuckPaymentDto[]) {
  return useQuery({
    queryKey: ['stuck'],
    initialData,
    queryFn: () => clientFetch<StuckPaymentDto[]>('/api/v1/admin/stuck'),
    staleTime: 10_000,
  });
}
