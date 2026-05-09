import 'server-only';
import { apiFetchNullable } from '~/lib/api-client/fetcher';
import type { CustomerSummaryDto } from '~/types/api';

export async function fetchCustomerSummary(customerId: string): Promise<CustomerSummaryDto | null> {
  return apiFetchNullable<CustomerSummaryDto>(
    `/api/v1/customers/${encodeURIComponent(customerId)}/summary`,
  );
}

export async function fetchAdminCustomerSummary(
  customerId: string,
): Promise<CustomerSummaryDto | null> {
  return apiFetchNullable<CustomerSummaryDto>(
    `/api/v1/admin/customers/${encodeURIComponent(customerId)}/summary`,
  );
}
