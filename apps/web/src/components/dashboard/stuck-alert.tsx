'use client';

import type { Route } from 'next';
import Link from 'next/link';
import { AlertTriangle } from 'lucide-react';
import { useStuckList } from '~/lib/hooks/use-stuck-list';
import { Badge } from '~/components/ui/badge';
import { buttonVariants } from '~/components/ui/button';
import { cn } from '~/lib/utils';

export function StuckAlert() {
  const { data: stuckList } = useStuckList();

  const count = stuckList?.length ?? 0;

  if (count === 0) return null;

  const criticalCount = stuckList?.filter((s) => {
    const stuckMs = Date.now() - new Date(s.stuck_since).getTime();
    return stuckMs > 24 * 60 * 60 * 1000;
  }).length ?? 0;

  return (
    <div
      data-testid="stuck-alert"
      className={cn(
        'flex items-center justify-between rounded-lg border-2 border-red-500/40 bg-red-500/5 px-5 py-4',
      )}
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
