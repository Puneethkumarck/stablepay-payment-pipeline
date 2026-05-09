'use client';

import type { LucideIcon } from 'lucide-react';
import { Database } from 'lucide-react';
import { useCallback, useEffect, useRef } from 'react';
import { Empty } from '~/components/empty';
import { Skeleton } from '~/components/ui/skeleton';
import { cn } from '~/lib/utils';

export interface ColumnConfig<T> {
  key: string;
  label: string;
  width?: string;
  render?: (value: unknown, row: T) => React.ReactNode;
  tdClassName?: string;
}

interface DataTableProps<T extends Record<string, unknown>> {
  columns: ColumnConfig<T>[];
  rows: T[];
  onRowClick?: (row: T) => void;
  emptyMessage?: string;
  emptyIcon?: LucideIcon;
  loading?: boolean;
  cursor?: string | null;
  hasMore?: boolean;
  onLoadMore?: () => void;
  className?: string;
}

export function DataTable<T extends Record<string, unknown>>({
  columns,
  rows,
  onRowClick,
  emptyMessage = 'No results',
  emptyIcon,
  loading = false,
  hasMore = false,
  onLoadMore,
  className,
}: DataTableProps<T>) {
  const sentinelRef = useRef<HTMLTableRowElement>(null);
  const loadTriggeredRef = useRef(false);

  useEffect(() => {
    if (!loading) loadTriggeredRef.current = false;
  }, [loading]);

  const handleIntersection = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const entry = entries[0];
      if (entry?.isIntersecting && hasMore && onLoadMore && !loading && !loadTriggeredRef.current) {
        loadTriggeredRef.current = true;
        onLoadMore();
      }
    },
    [hasMore, onLoadMore, loading],
  );

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel || !hasMore || !onLoadMore) return;

    const observer = new IntersectionObserver(handleIntersection, {
      threshold: 0.1,
    });
    observer.observe(sentinel);

    return () => observer.disconnect();
  }, [handleIntersection, hasMore, onLoadMore]);

  return (
    <div data-testid="data-table" className={cn('overflow-x-auto', className)}>
      <table className="w-full border-collapse" style={{ tableLayout: 'fixed' }}>
        <colgroup>
          {columns.map((col) => (
            <col key={`${col.key}-col`} style={{ width: col.width ?? 'auto' }} />
          ))}
        </colgroup>
        <thead>
          <tr className="border-b border-border-1">
            {columns.map((col) => (
              <th
                key={col.key}
                className="px-[14px] py-[9px] text-left text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-3 whitespace-nowrap"
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && !loading && (
            <tr>
              <td colSpan={columns.length}>
                <Empty icon={emptyIcon ?? Database} title={emptyMessage} />
              </td>
            </tr>
          )}
          {rows.map((row, i) => (
            <tr
              key={i}
              onClick={() => onRowClick?.(row)}
              onKeyDown={(e) => {
                if ((e.key === 'Enter' || e.key === ' ') && onRowClick) {
                  e.preventDefault();
                  onRowClick(row);
                }
              }}
              role={onRowClick ? 'button' : undefined}
              tabIndex={onRowClick ? 0 : undefined}
              className={cn(
                'border-b border-[rgba(255,255,255,0.04)] transition-colors duration-100',
                onRowClick &&
                  'cursor-pointer hover:bg-[rgba(255,255,255,0.035)] focus-visible:bg-[rgba(255,255,255,0.035)]',
              )}
            >
              {columns.map((col) => (
                <td
                  key={col.key}
                  className={cn(
                    'overflow-hidden px-[14px] py-[11px] text-ellipsis align-middle whitespace-nowrap',
                    col.tdClassName,
                  )}
                >
                  {col.render ? col.render(row[col.key], row) : String(row[col.key] ?? '')}
                </td>
              ))}
            </tr>
          ))}
          {loading &&
            Array.from({ length: 3 }).map((_, i) => (
              <tr key={`skeleton-${i}`} className="border-b border-[rgba(255,255,255,0.04)]">
                {columns.map((col) => (
                  <td key={col.key} className="px-[14px] py-[11px]">
                    <Skeleton className="h-3 w-full" />
                  </td>
                ))}
              </tr>
            ))}
          {hasMore && !loading && (
            <tr ref={sentinelRef} data-testid="data-table-sentinel">
              <td colSpan={columns.length} className="h-1" />
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
