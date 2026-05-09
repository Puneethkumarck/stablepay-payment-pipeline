import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { DlqList } from '~/components/dlq/dlq-list';
import { fetchDlqList, fetchDlqSummary } from '~/lib/data';

export default async function DlqListPage() {
  const queryClient = new QueryClient();

  await Promise.all([
    queryClient.prefetchQuery({
      queryKey: ['dlq', 'list', undefined],
      queryFn: () => fetchDlqList(),
    }),
    queryClient.prefetchQuery({
      queryKey: ['dlq', 'summary'],
      queryFn: () => fetchDlqSummary(),
    }),
  ]);

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <DlqList />
    </HydrationBoundary>
  );
}
