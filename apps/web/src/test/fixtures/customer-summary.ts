import type { CustomerSummaryDto } from '~/types/api';

const defaults: CustomerSummaryDto = {
  customer_id: 'CUST-001',
  total_transactions: 128,
  total_volume: { amount: 5_000_000, currency: 'USD' },
  active_flows: 3,
  last_activity_at: '2026-01-15T10:30:00Z',
};

export function createCustomerSummary(overrides?: Partial<CustomerSummaryDto>): CustomerSummaryDto {
  return { ...defaults, ...overrides };
}
