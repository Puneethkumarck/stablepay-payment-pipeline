import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { notFound } from 'next/navigation';
import { FlowDetail } from '~/components/flows/flow-detail';
import { fetchFlow } from '~/lib/data';

export default async function FlowDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const flow = await fetchFlow(id);
  if (!flow) notFound();

  const queryClient = new QueryClient();
  queryClient.setQueryData(['flow', id], flow);

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <FlowDetail flowId={id} initialData={flow} />
    </HydrationBoundary>
  );
}
