'use client';

import { useQuery } from '@tanstack/react-query';
import type { StuckPaymentDto } from '~/types/api';

async function fetchStuckList(): Promise<StuckPaymentDto[]> {
  const response = await fetch('/api/v1/admin/stuck');
  if (!response.ok) {
    throw new Error('STBLPAY-1999');
  }
  return response.json() as Promise<StuckPaymentDto[]>;
}

export function useStuckList(initialData?: StuckPaymentDto[]) {
  return useQuery({
    queryKey: ['stuck'],
    initialData,
    queryFn: fetchStuckList,
    staleTime: 10_000,
  });
}
