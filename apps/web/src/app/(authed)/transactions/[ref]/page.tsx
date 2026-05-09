import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { notFound } from 'next/navigation';
import { TransactionDetail } from '~/components/transactions/transaction-detail';
import { fetchTransaction } from '~/lib/data';

export default async function TransactionDetailPage({
  params,
}: {
  params: Promise<{ ref: string }>;
}) {
  const { ref } = await params;
  const txn = await fetchTransaction(ref);
  if (!txn) notFound();

  const queryClient = new QueryClient();
  queryClient.setQueryData(['transaction', ref], txn);

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <TransactionDetail txnRef={ref} initialData={txn} />
    </HydrationBoundary>
  );
}
