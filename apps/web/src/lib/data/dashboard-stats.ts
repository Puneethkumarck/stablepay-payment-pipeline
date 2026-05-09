import 'server-only';
import { apiFetch } from '~/lib/api-client/fetcher';
import type { DashboardStatsDto } from '~/types/api';

export async function fetchDashboardStats(): Promise<DashboardStatsDto> {
  return apiFetch<DashboardStatsDto>('/api/v1/dashboard/stats');
}

export async function fetchAdminDashboardStats(): Promise<DashboardStatsDto> {
  return apiFetch<DashboardStatsDto>('/api/v1/admin/dashboard/stats');
}
