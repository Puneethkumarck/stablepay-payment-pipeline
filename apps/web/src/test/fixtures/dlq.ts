import type { CursorPage, DlqEntryDto, DlqSummaryDto } from '~/types/api';

const defaults: DlqEntryDto = {
  id: 'DLQ-001',
  topic: 'fiat.payin.events.v1',
  error_class: 'SCHEMA_INVALID',
  error_message: 'Missing required field: amount',
  event_key: 'TXN-FAIL-001',
  event_payload: '{"ref":"TXN-FAIL-001"}',
  retry_count: 0,
  created_at: '2026-01-15T11:00:00Z',
  updated_at: '2026-01-15T11:00:00Z',
};

export function createDlqEntry(overrides?: Partial<DlqEntryDto>): DlqEntryDto {
  return { ...defaults, ...overrides };
}

export function createDlqPage(
  overrides?: Partial<CursorPage<DlqEntryDto>>,
): CursorPage<DlqEntryDto> {
  return {
    data: [createDlqEntry()],
    next_cursor: null,
    has_more: false,
    ...overrides,
  };
}

export function createDlqSummary(overrides?: Partial<DlqSummaryDto>): DlqSummaryDto {
  return {
    total: 3,
    by_error_class: {
      SCHEMA_INVALID: 1,
      PROCESSING_FAILED: 1,
      LATE_EVENT: 1,
    },
    ...overrides,
  };
}
