import type { DashboardStatsDto } from '~/types/api';

const defaults: DashboardStatsDto = {
  volume_24h: { amount: 1_250_000, currency: 'USD' },
  transaction_count_24h: 42,
  success_rate_24h: 0.95,
  dlq_count: 3,
  stuck_count: 1,
};

export function createDashboardStats(overrides?: Partial<DashboardStatsDto>): DashboardStatsDto {
  return { ...defaults, ...overrides };
}
