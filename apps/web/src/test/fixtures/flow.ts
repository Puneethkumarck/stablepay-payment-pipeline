import type { FlowDto } from '~/types/api';

const defaults: FlowDto = {
  id: 'FLOW-001',
  customer_id: 'CUST-001',
  flow_type: 'FIAT_PAYOUT',
  status: 'COMPLETED',
  source_amount: { amount: 100_000, currency: 'USD' },
  destination_amount: { amount: 92_000, currency: 'EUR' },
  legs: [
    {
      leg_index: 0,
      transaction_ref: 'TXN-001',
      direction: 'PAYIN',
      type: 'FIAT',
      status: 'COMPLETED',
      amount: { amount: 100_000, currency: 'USD' },
    },
    {
      leg_index: 1,
      transaction_ref: 'TXN-002',
      direction: 'PAYOUT',
      type: 'FIAT',
      status: 'COMPLETED',
      amount: { amount: 92_000, currency: 'EUR' },
    },
  ],
  created_at: '2026-01-15T10:30:00Z',
  updated_at: '2026-01-15T10:40:00Z',
};

export function createFlow(overrides?: Partial<FlowDto>): FlowDto {
  return { ...defaults, ...overrides };
}
