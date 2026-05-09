'use client';

import { Card, CardContent, CardHeader, CardTitle } from '~/components/ui/card';
import { useTransactionsList } from '~/lib/hooks/use-transactions-list';

interface FlowCategory {
  label: string;
  color: string;
  match: (row: { type: string; direction: string }) => boolean;
}

const FLOW_CATEGORIES: FlowCategory[] = [
  {
    label: 'Fiat payin',
    color: '#3B82F6',
    match: (r) => r.type === 'FIAT' && r.direction === 'PAYIN',
  },
  {
    label: 'Fiat payout',
    color: '#8B5CF6',
    match: (r) => r.type === 'FIAT' && r.direction === 'PAYOUT',
  },
  {
    label: 'Crypto',
    color: '#22C55E',
    match: (r) => r.type === 'CRYPTO',
  },
  {
    label: 'Multi-leg',
    color: '#F59E0B',
    match: () => false,
  },
];

export function FlowBreakdownCard() {
  const { data } = useTransactionsList({ limit: 6 });

  const rows = data?.data ?? [];
  const total = rows.length || 1;

  const counts = FLOW_CATEGORIES.map((cat) => ({
    ...cat,
    count: rows.filter((r) => cat.match(r)).length,
  }));

  return (
    <Card data-testid="flow-breakdown-card">
      <CardHeader className="pb-3">
        <CardTitle className="text-sm font-semibold">Flow breakdown</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        {counts.map((cat) => {
          const pct = Math.round((cat.count / total) * 100);
          return (
            <div key={cat.label}>
              <div className="mb-1 flex items-center justify-between text-xs">
                <span className="text-fg-2">{cat.label}</span>
                <span className="font-mono text-fg-3">{cat.count}</span>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-white/5">
                <div
                  className="h-full rounded-full transition-all duration-300"
                  style={{ width: `${pct}%`, backgroundColor: cat.color }}
                  role="progressbar"
                  aria-valuenow={pct}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label={`${cat.label}: ${pct}%`}
                />
              </div>
            </div>
          );
        })}
      </CardContent>
    </Card>
  );
}
