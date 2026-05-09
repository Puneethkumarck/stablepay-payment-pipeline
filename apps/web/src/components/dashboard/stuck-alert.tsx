'use client';

import { AlertTriangle } from 'lucide-react';
import type { Route } from 'next';
import Link from 'next/link';
import { useMemo } from 'react';
import { Badge } from '~/components/ui/badge';
import { buttonVariants } from '~/components/ui/button';
import { useStuckList } from '~/lib/hooks/use-stuck-list';

const TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000;

function countCritical(list: { stuck_since: string }[]): number {
  const now = Date.now();
  return list.filter((s) => now - new Date(s.stuck_since).getTime() > TWENTY_FOUR_HOURS_MS).length;
}

export function StuckAlert() {
  const { data: stuckList } = useStuckList();

  const count = stuckList?.length ?? 0;
  const criticalCount = useMemo(() => (stuckList ? countCritical(stuckList) : 0), [stuckList]);

  if (count === 0) return null;

  return (
    <div
      data-testid="stuck-alert"
      className="flex items-center justify-between rounded-lg border-2 border-red-500/40 bg-red-500/5 px-5 py-4"
    >
      <div className="flex items-center gap-3">
        <AlertTriangle size={18} className="shrink-0 text-red-400" />
        <div>
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-fg-1">
              {count} stuck payment{count !== 1 ? 's' : ''}
            </span>
            {criticalCount > 0 && (
              <Badge variant="destructive" className="text-[10px]">
                {criticalCount} critical
              </Badge>
            )}
          </div>
        </div>
      </div>
      <Link
        href={'/admin/stuck' as Route}
        className={buttonVariants({ variant: 'destructive', size: 'sm' })}
      >
        Review
      </Link>
    </div>
  );
}
