import 'server-only';
import { apiFetchNullable } from '~/lib/api-client/fetcher';
import type { FlowDto } from '~/types/api';

export async function fetchFlow(id: string): Promise<FlowDto | null> {
  return apiFetchNullable<FlowDto>(`/api/v1/flows/${encodeURIComponent(id)}`);
}

export async function fetchAdminFlow(id: string): Promise<FlowDto | null> {
  return apiFetchNullable<FlowDto>(`/api/v1/admin/flows/${encodeURIComponent(id)}`);
}
