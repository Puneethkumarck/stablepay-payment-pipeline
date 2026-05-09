export interface MoneyDto {
  amount: number;
  currency: string;
}

export interface TransactionDto {
  ref: string;
  flow_id: string;
  customer_id: string;
  account_id?: string;
  direction: 'PAYIN' | 'PAYOUT';
  type: 'FIAT' | 'CRYPTO';
  internal_status: string;
  customer_status: string;
  amount: MoneyDto;
  fee?: MoneyDto;
  counterparty?: string;
  provider?: string;
  blockchain?: string;
  tx_hash?: string;
  event_id?: string;
  correlation_id?: string;
  trace_id?: string;
  schema_version?: string;
  source_topic?: string;
  created_at: string;
  updated_at: string;
}

export interface CursorPage<T> {
  data: T[];
  next_cursor: string | null;
  has_more: boolean;
}

export interface TransactionListCriteria {
  cursor?: string;
  limit?: number;
  status?: string;
  direction?: string;
  type?: string;
  search?: string;
  from?: string;
  to?: string;
}

export interface FlowDto {
  id: string;
  customer_id: string;
  flow_type: string;
  status: string;
  source_amount: MoneyDto;
  destination_amount?: MoneyDto;
  legs: FlowLegDto[];
  created_at: string;
  updated_at: string;
}

export interface FlowLegDto {
  leg_index: number;
  transaction_ref: string;
  direction: 'PAYIN' | 'PAYOUT';
  type: 'FIAT' | 'CRYPTO';
  status: string;
  amount: MoneyDto;
}

export interface DlqEntryDto {
  id: string;
  topic: string;
  error_class: string;
  error_message: string;
  event_key: string;
  event_payload: string;
  retry_count: number;
  created_at: string;
  updated_at: string;
}

export interface DlqSummaryDto {
  total: number;
  by_error_class: Record<string, number>;
}

export interface StuckPaymentDto {
  transaction_ref: string;
  flow_id: string;
  customer_id: string;
  status: string;
  amount: MoneyDto;
  stuck_since: string;
  stuck_reason: string;
}

export interface CustomerSummaryDto {
  customer_id: string;
  total_transactions: number;
  total_volume: MoneyDto;
  active_flows: number;
  last_activity_at: string;
}

export interface DashboardStatsDto {
  volume_24h: MoneyDto;
  transaction_count_24h: number;
  success_rate_24h: number;
  dlq_count: number;
  stuck_count: number;
}

export interface ApiError {
  error_code: string;
  message: string;
}
