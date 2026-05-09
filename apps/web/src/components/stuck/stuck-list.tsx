'use client';

import { useCallback, useMemo, useState } from 'react';
import Link from 'next/link';
import type { Route } from 'next';
import { AlertTriangle, ChevronDown, ChevronRight } from 'lucide-react';
import type { ColumnConfig } from '~/components/data-table';
import { DataTable } from '~/components/data-table';
import { Amount } from '~/components/amount';
import { StatusBadge } from '~/components/status-badge';
import { Badge } from '~/components/ui/badge';
import { Button } from '~/components/ui/button';
import { RefundConfirmationDialog } from '~/components/stuck/refund-confirmation-dialog';
import { useStuckList } from '~/lib/hooks/use-stuck-list';
import type { StuckPaymentDto } from '~/types/api';
import { cn } from '~/lib/utils';

const CRITICAL_THRESHOLD_MS = 24 * 60 * 60 * 1000;

function isCritical(stuckSince: string): boolean {
  return Date.now() - new Date(stuckSince).getTime() >= CRITICAL_THRESHOLD_MS;
}

type StuckRow = StuckPaymentDto & Record<string, unknown>;

export function StuckList() {
  const { data: stuckPayments, isLoading } = useStuckList();
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const items = (stuckPayments ?? []) as StuckRow[];

  const criticalCount = useMemo(
    () => items.filter((p) => isCritical(p.stuck_since)).length,
    [items],
  );

  const toggleExpanded = useCallback((ref: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(ref)) {
        next.delete(ref);
      } else {
        next.add(ref);
      }
      return next;
    });
  }, []);

  const tableColumns: ColumnConfig<StuckRow>[] = useMemo(
    () => [
      {
        key: 'transaction_ref',
        label: 'Transaction ref',
        width: '22%',
        tdClassName: 'font-mono text-[12px] text-fg-2',
      },
      {
        key: 'status',
        label: 'Status',
        width: '14%',
        render: (_: unknown, row: StuckRow) => <StatusBadge status={row.status} />,
      },
      {
        key: 'amount',
        label: 'Amount',
        width: '16%',
        render: (_: unknown, row: StuckRow) => (
          <Amount value={row.amount.amount} currency={row.amount.currency} size="sm" />
        ),
      },
      {
        key: 'stuck_since',
        label: 'Stuck since',
        width: '20%',
        render: (val: unknown) =>
          new Date(val as string).toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          }),
        tdClassName: 'text-[12px] text-fg-3',
      },
      {
        key: 'stuck_reason',
        label: 'Reason',
        width: '28%',
        tdClassName: 'text-[12px] text-fg-2 truncate',
      },
    ],
    [],
  );

  return (
    <div className="page" data-testid="stuck-list-page">
      <div className="sp-eyebrow mb-1 text-[10px]">Admin</div>
      <h1 className="mb-5 text-[22px] font-bold tracking-tight text-fg-1">Stuck Payments</h1>

      {/* Banner */}
      <div
        data-testid="stuck-banner"
        className={cn(
          'mb-5 flex items-center gap-3 rounded-card border px-4 py-3',
          criticalCount > 0
            ? 'border-red-500/24 bg-red-500/10'
            : 'border-amber-500/24 bg-amber-500/10',
        )}
      >
        <AlertTriangle
          size={16}
          className={criticalCount > 0 ? 'text-red-400' : 'text-amber-400'}
        />
        <span className="text-[13px] font-medium text-fg-1">
          {items.length} stuck payment{items.length !== 1 ? 's' : ''}
          {criticalCount > 0 && (
            <span className="text-red-400">
              {' '}— {criticalCount} exceed{criticalCount === 1 ? 's' : ''} 24h threshold
            </span>
          )}
        </span>
      </div>

      {/* Expandable cards */}
      <div className="mb-6 space-y-2" data-testid="stuck-cards">
        {items.map((payment) => {
          const isOpen = expanded.has(payment.transaction_ref);
          const critical = isCritical(payment.stuck_since);

          return (
            <div
              key={payment.transaction_ref}
              className="rounded-card border border-border-1 bg-surface-2"
            >
              <button
                type="button"
                data-testid={`stuck-card-${payment.transaction_ref}`}
                className="flex w-full items-center justify-between px-4 py-3 text-left"
                onClick={() => toggleExpanded(payment.transaction_ref)}
                aria-expanded={isOpen}
              >
                <div className="flex items-center gap-3">
                  <span className="font-mono text-[12px] text-fg-2">
                    {payment.transaction_ref}
                  </span>
                  <StatusBadge status={payment.status} />
                  <Amount
                    value={payment.amount.amount}
                    currency={payment.amount.currency}
                    size="sm"
                  />
                  {critical && (
                    <Badge data-testid="critical-badge" variant="destructive">
                      Critical
                    </Badge>
                  )}
                </div>
                {isOpen ? (
                  <ChevronDown size={16} className="text-fg-3" />
                ) : (
                  <ChevronRight size={16} className="text-fg-3" />
                )}
              </button>

              {isOpen && (
                <div
                  data-testid={`stuck-card-detail-${payment.transaction_ref}`}
                  className="border-t border-border-1 px-4 py-3"
                >
                  <p className="mb-3 text-[12px] text-fg-3">{payment.stuck_reason}</p>
                  <div className="flex items-center gap-2">
                    <Link
                      href={`/transactions/${encodeURIComponent(payment.transaction_ref)}` as Route}
                    >
                      <Button variant="outline" size="xs" data-testid="view-transaction-link">
                        View transaction
                      </Button>
                    </Link>
                    <RefundConfirmationDialog stuckPayment={payment} />
                  </div>
                </div>
              )}
            </div>
          );
        })}

        {items.length === 0 && !isLoading && (
          <div className="py-8 text-center text-[13px] text-fg-3">No stuck payments</div>
        )}
      </div>

      {/* Agg stuck withdrawals table */}
      <div className="sp-eyebrow mb-2 text-[10px]">Trino · iceberg catalog</div>
      <h2 className="mb-3 text-[16px] font-semibold text-fg-1">agg_stuck_withdrawals</h2>
      <div className="rounded-card border border-border-1 bg-surface-2">
        <DataTable
          columns={tableColumns}
          rows={items}
          emptyMessage="No stuck withdrawals"
          loading={isLoading}
        />
      </div>
    </div>
  );
}
