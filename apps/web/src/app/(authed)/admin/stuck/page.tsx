import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { StuckList } from '~/components/stuck/stuck-list';
import { fetchStuckList } from '~/lib/data';

export default async function StuckPage() {
  const queryClient = new QueryClient();

  await queryClient.prefetchQuery({
    queryKey: ['stuck'],
    queryFn: () => fetchStuckList(),
  });

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <StuckList />
    </HydrationBoundary>
  );
}
