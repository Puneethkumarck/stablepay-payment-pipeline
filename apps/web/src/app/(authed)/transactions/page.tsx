import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { TransactionsList } from '~/components/transactions/transactions-list';
import { fetchTransactionsList } from '~/lib/data';

export default async function TransactionsPage() {
  const queryClient = new QueryClient();

  await queryClient.prefetchQuery({
    queryKey: ['transactions', { limit: 50 }],
    queryFn: () => fetchTransactionsList({ limit: 50 }),
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <TransactionsList />
    </HydrationBoundary>
  );
}
