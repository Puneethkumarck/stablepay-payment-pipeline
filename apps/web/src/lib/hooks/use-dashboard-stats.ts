'use client';

import { useQuery } from '@tanstack/react-query';
import type { DashboardStatsDto } from '~/types/api';

async function fetchDashboardStats(): Promise<DashboardStatsDto> {
  const response = await fetch('/api/v1/dashboard/stats');
  if (!response.ok) {
    throw new Error('STBLPAY-1999');
  }
  return response.json() as Promise<DashboardStatsDto>;
}

export function useDashboardStats(initialData?: DashboardStatsDto) {
  return useQuery({
    queryKey: ['dashboard-stats'],
    initialData,
    queryFn: fetchDashboardStats,
    refetchInterval: 10_000,
    staleTime: 5_000,
  });
}
