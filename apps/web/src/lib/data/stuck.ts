import 'server-only';
import { apiFetch } from '~/lib/api-client/fetcher';
import type { StuckPaymentDto } from '~/types/api';

export async function fetchStuckList(): Promise<StuckPaymentDto[]> {
  return apiFetch<StuckPaymentDto[]>('/api/v1/admin/stuck');
}
