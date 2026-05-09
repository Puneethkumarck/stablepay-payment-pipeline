import 'server-only';
import { apiFetch, apiFetchNullable } from '~/lib/api-client/fetcher';
import type { CursorPage, DlqEntryDto, DlqSummaryDto } from '~/types/api';

export async function fetchDlqList(cursor?: string): Promise<CursorPage<DlqEntryDto>> {
  const params = cursor ? `?cursor=${encodeURIComponent(cursor)}` : '';
  return apiFetch<CursorPage<DlqEntryDto>>(`/api/v1/admin/dlq${params}`);
}

export async function fetchDlqEntry(id: string): Promise<DlqEntryDto | null> {
  return apiFetchNullable<DlqEntryDto>(`/api/v1/admin/dlq/${encodeURIComponent(id)}`);
}

export async function fetchDlqSummary(): Promise<DlqSummaryDto> {
  return apiFetch<DlqSummaryDto>('/api/v1/admin/dlq/summary');
}
