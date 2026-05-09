import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { DlqList } from '~/components/dlq/dlq-list';
import { fetchDlqList, fetchDlqSummary } from '~/lib/data';

export default async function DlqListPage() {
  const queryClient = new QueryClient();

  const [listData, summaryData] = await Promise.all([
    queryClient.fetchQuery({
      queryKey: ['dlq', 'list', undefined],
      queryFn: () => fetchDlqList(),
    }),
    queryClient.fetchQuery({
      queryKey: ['dlq', 'summary'],
      queryFn: () => fetchDlqSummary(),
    }),
  ]);

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <DlqList initialData={listData} initialSummary={summaryData} />
    </HydrationBoundary>
  );
}
