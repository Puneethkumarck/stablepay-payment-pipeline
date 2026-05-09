import type { CursorPage, TransactionDto } from '~/types/api';

const defaults: TransactionDto = {
  ref: 'TXN-001',
  flow_id: 'FLOW-001',
  customer_id: 'CUST-001',
  direction: 'PAYIN',
  type: 'FIAT',
  internal_status: 'COMPLETED',
  customer_status: 'COMPLETED',
  amount: { amount: 150_000, currency: 'USD' },
  created_at: '2026-01-15T10:30:00Z',
  updated_at: '2026-01-15T10:35:00Z',
};

export function createTransaction(overrides?: Partial<TransactionDto>): TransactionDto {
  return { ...defaults, ...overrides };
}

export function createTransactionPage(
  overrides?: Partial<CursorPage<TransactionDto>>,
): CursorPage<TransactionDto> {
  return {
    data: [createTransaction()],
    next_cursor: null,
    has_more: false,
    ...overrides,
  };
}
