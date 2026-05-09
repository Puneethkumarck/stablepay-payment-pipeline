import { dehydrate, HydrationBoundary, QueryClient } from '@tanstack/react-query';
import { DlqSummaryCard } from '~/components/dashboard/dlq-summary-card';
import { FlowBreakdownCard } from '~/components/dashboard/flow-breakdown-card';
import { RecentTxnsCard } from '~/components/dashboard/recent-txns-card';
import { StatsRow } from '~/components/dashboard/stats-row';
import { StuckAlert } from '~/components/dashboard/stuck-alert';
import { PageHeader } from '~/components/layout/page-header';
import {
  fetchDashboardStats,
  fetchDlqSummary,
  fetchStuckList,
  fetchTransactionsList,
} from '~/lib/data';

export default async function Dashboard() {
  const queryClient = new QueryClient();

  await Promise.all([
    queryClient.prefetchQuery({
      queryKey: ['dashboard-stats'],
      queryFn: fetchDashboardStats,
    }),
    queryClient.prefetchQuery({
      queryKey: ['transactions', { limit: 6 }],
      queryFn: () => fetchTransactionsList({ limit: 6 }),
    }),
    queryClient.prefetchQuery({
      queryKey: ['dlq', 'summary'],
      queryFn: fetchDlqSummary,
    }),
    queryClient.prefetchQuery({
      queryKey: ['stuck'],
      queryFn: fetchStuckList,
    }),
  ]);

  return (
    <HydrationBoundary state={dehydrate(queryClient)}>
      <div className="page">
        <PageHeader title="Dashboard" eyebrow="Overview" />
        <StatsRow />
        <div className="mt-5 grid grid-cols-1 gap-4 lg:grid-cols-[1fr_280px]">
          <div className="flex min-w-0 flex-col gap-4">
            <RecentTxnsCard />
            <StuckAlert />
          </div>
          <div className="flex min-w-0 flex-col gap-3.5">
            <FlowBreakdownCard />
            <DlqSummaryCard />
          </div>
        </div>
      </div>
    </HydrationBoundary>
  );
}
