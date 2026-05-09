'use client';

import { Activity, AlertTriangle, CheckCircle, DollarSign } from 'lucide-react';
import { useDashboardStats } from '~/lib/hooks/use-dashboard-stats';
import { formatMoney } from '~/lib/format/money';
import { StatCard } from '~/components/stat-card';

export function StatsRow() {
  const { data } = useDashboardStats();

  const volume = data ? formatMoney(data.volume_24h.amount, data.volume_24h.currency) : '—';
  const successRate = data ? `${(data.success_rate_24h * 100).toFixed(1)}%` : '—';
  const dlqCount = data ? String(data.dlq_count) : '—';
  const stuckCount = data ? String(data.stuck_count) : '—';

  return (
    <div data-testid="stats-row" className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      <StatCard
        label="Volume (24h)"
        value={volume}
        sub={data ? `${data.transaction_count_24h} transactions` : undefined}
        icon={DollarSign}
        accentColor="#3B82F6"
      />
      <StatCard
        label="Success rate"
        value={successRate}
        icon={CheckCircle}
        accentColor="#22C55E"
      />
      <StatCard
        label="DLQ events"
        value={dlqCount}
        icon={Activity}
        accentColor="#F59E0B"
      />
      <StatCard
        label="Stuck payments"
        value={stuckCount}
        icon={AlertTriangle}
        accentColor="#EF4444"
      />
    </div>
  );
}
