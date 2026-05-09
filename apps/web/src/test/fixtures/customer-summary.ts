import type { CustomerSummaryDto } from '~/types/api';
import { createTransaction } from './transaction';

const defaults: CustomerSummaryDto = {
  customer_id: 'c3a1f5e2-7b4d-4e8a-9f2c-1d6e3a8b5c7f',
  name: 'Alice Johnson',
  email: 'alice.johnson@example.com',
  kyc_status: 'VERIFIED',
  risk_tier: 'LOW',
  balance: { amount: 2_500_000, currency: 'USDC' },
  total_sent: { amount: 12_340_000, currency: 'USDC' },
  total_transactions: 128,
  total_volume: { amount: 5_000_000, currency: 'USD' },
  active_flows: 3,
  member_since: '2025-03-10T00:00:00Z',
  last_activity_at: '2026-01-15T10:30:00Z',
  recent_transactions: [
    createTransaction({ ref: 'TXN-001', created_at: '2026-01-15T10:30:00Z' }),
    createTransaction({ ref: 'TXN-002', direction: 'PAYOUT', created_at: '2026-01-14T08:15:00Z' }),
    createTransaction({ ref: 'TXN-003', type: 'CRYPTO', created_at: '2026-01-13T14:45:00Z' }),
    createTransaction({ ref: 'TXN-004', created_at: '2026-01-12T11:20:00Z' }),
    createTransaction({
      ref: 'TXN-005',
      direction: 'PAYOUT',
      type: 'CRYPTO',
      created_at: '2026-01-11T09:00:00Z',
    }),
  ],
};

export function createCustomerSummary(overrides?: Partial<CustomerSummaryDto>): CustomerSummaryDto {
  return { ...defaults, ...overrides };
}
