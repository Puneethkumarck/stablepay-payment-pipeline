'use client';

import type { Route } from 'next';
import Link from 'next/link';
import { ArrowRight } from 'lucide-react';
import { useDlqSummary } from '~/lib/hooks/use-dlq-summary';
import { buttonVariants } from '~/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '~/components/ui/card';
import { cn } from '~/lib/utils';

const ERROR_CLASSES = [
  { key: 'SCHEMA_INVALID', label: 'Schema invalid', color: 'text-red-400' },
  { key: 'PROCESSING_FAILED', label: 'Proc. failed', color: 'text-amber-400' },
  { key: 'SINK_FAILURE', label: 'Sink failure', color: 'text-orange-400' },
  { key: 'LATE_EVENT', label: 'Late event', color: 'text-sky-400' },
] as const;

export function DlqSummaryCard() {
  const { data } = useDlqSummary();

  const byClass = data?.by_error_class ?? {};

  return (
    <Card data-testid="dlq-summary-card">
      <CardHeader className="flex-row items-center justify-between pb-3">
        <CardTitle className="text-sm font-semibold">DLQ summary</CardTitle>
        <Link
          href={'/admin/dlq' as Route}
          className={buttonVariants({ variant: 'ghost', size: 'sm' })}
        >
          Open DLQ inspector <ArrowRight size={14} />
        </Link>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-3">
          {ERROR_CLASSES.map(({ key, label, color }) => (
            <div
              key={key}
              className="rounded-md border border-border-1 bg-surface-2 px-3 py-2.5"
            >
              <div className="text-[10px] font-medium uppercase tracking-wider text-fg-3">
                {label}
              </div>
              <div className={cn('mt-1 text-lg font-bold', color)}>
                {byClass[key] ?? 0}
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
