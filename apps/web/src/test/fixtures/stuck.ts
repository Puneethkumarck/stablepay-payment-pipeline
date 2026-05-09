import type { StuckPaymentDto } from '~/types/api';

const defaults: StuckPaymentDto = {
  transaction_ref: 'TXN-STUCK-001',
  flow_id: 'FLOW-STUCK-001',
  customer_id: 'CUST-001',
  status: 'STUCK',
  amount: { amount: 50_000, currency: 'USD' },
  stuck_since: '2026-01-14T08:00:00Z',
  stuck_reason: 'Partner timeout after 24h',
};

export function createStuckPayment(overrides?: Partial<StuckPaymentDto>): StuckPaymentDto {
  return { ...defaults, ...overrides };
}
