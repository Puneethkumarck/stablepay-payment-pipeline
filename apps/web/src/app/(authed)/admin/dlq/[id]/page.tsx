import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { notFound } from 'next/navigation';
import { DlqDetail } from '~/components/dlq/dlq-detail';
import { fetchDlqEntry } from '~/lib/data';

export default async function DlqDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const entry = await fetchDlqEntry(id);
  if (!entry) notFound();

  const queryClient = new QueryClient();
  queryClient.setQueryData(['dlq', 'detail', id], entry);

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <DlqDetail dlqId={id} initialData={entry} />
    </HydrationBoundary>
  );
}
