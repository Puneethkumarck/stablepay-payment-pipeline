'use client';

import { useQuery } from '@tanstack/react-query';
import { clientFetch } from '~/lib/api-client/client-fetch';
import type { DashboardStatsDto } from '~/types/api';

export function useDashboardStats(initialData?: DashboardStatsDto) {
  return useQuery({
    queryKey: ['dashboard-stats'],
    initialData,
    queryFn: () => clientFetch<DashboardStatsDto>('/api/v1/dashboard/stats'),
    refetchInterval: 10_000,
    staleTime: 5_000,
  });
}
