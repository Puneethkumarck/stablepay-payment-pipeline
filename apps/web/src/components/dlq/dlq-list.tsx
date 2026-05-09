'use client';

import { useCallback, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import type { Route } from 'next';
import { AlertTriangle, Clock, Cpu, FileWarning } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { ColumnConfig } from '~/components/data-table';
import { DataTable } from '~/components/data-table';
import { SearchBar } from '~/components/search-bar';
import { StatusBadge } from '~/components/status-badge';
import { ReplayButton } from '~/components/dlq/replay-button';
import { useDlqList } from '~/lib/hooks/use-dlq-list';
import { useDlqSummary } from '~/lib/hooks/use-dlq-summary';
import type { DlqEntryDto } from '~/types/api';
import { cn } from '~/lib/utils';

const ERROR_CLASSES: { key: string; label: string; color: string; icon: LucideIcon }[] = [
  { key: 'SCHEMA_INVALID', label: 'Schema invalid', color: 'text-red-400', icon: FileWarning },
  { key: 'PROCESSING_FAILED', label: 'Processing failed', color: 'text-amber-400', icon: Cpu },
  { key: 'SINK_FAILURE', label: 'Sink failure', color: 'text-orange-400', icon: AlertTriangle },
  { key: 'LATE_EVENT', label: 'Late event', color: 'text-sky-400', icon: Clock },
];

type DlqRow = DlqEntryDto & Record<string, unknown>;

export function DlqList() {
  const router = useRouter();
  const [search, setSearch] = useState('');
  const { data: listData, isLoading } = useDlqList();
  const { data: summaryData } = useDlqSummary();

  const byClass = summaryData?.by_error_class ?? {};

  const filteredRows = useMemo(() => {
    const rows = (listData?.data ?? []) as DlqRow[];
    if (!search) return rows;
    const q = search.toLowerCase();
    return rows.filter(
      (row) =>
        row.id.toLowerCase().includes(q) ||
        row.error_class.toLowerCase().includes(q) ||
        row.topic.toLowerCase().includes(q),
    );
  }, [listData?.data, search]);

  const handleRowClick = useCallback(
    (row: DlqRow) => {
      router.push(`/admin/dlq/${encodeURIComponent(row.id)}` as Route);
    },
    [router],
  );

  const columns: ColumnConfig<DlqRow>[] = useMemo(
    () => [
      {
        key: 'id',
        label: 'DLQ ID',
        width: '18%',
        tdClassName: 'font-mono text-[12px] text-fg-2',
      },
      {
        key: 'error_class',
        label: 'Error class',
        width: '16%',
        render: (_: unknown, row: DlqRow) => <StatusBadge status={row.error_class} />,
      },
      {
        key: 'topic',
        label: 'Source topic',
        width: '22%',
        tdClassName: 'font-mono text-[12px] text-fg-2',
      },
      {
        key: 'retry_count',
        label: 'Retries',
        width: '8%',
        tdClassName: 'text-[12px] text-fg-2 text-center',
        render: (val: unknown) => String(val),
      },
      {
        key: 'created_at',
        label: 'Failed at',
        width: '18%',
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
        key: 'actions',
        label: 'Actions',
        width: '18%',
        render: (_: unknown, row: DlqRow) => (
          <div onClick={(e) => e.stopPropagation()} onKeyDown={(e) => e.stopPropagation()}>
            <ReplayButton
              dlqId={row.id}
              errorClass={row.error_class}
              retryCount={row.retry_count}
            />
          </div>
        ),
      },
    ],
    [],
  );

  return (
    <div className="page" data-testid="dlq-list-page">
      <div className="sp-eyebrow mb-1 text-[10px]">Admin</div>
      <h1 className="mb-5 text-[22px] font-bold tracking-tight text-fg-1">DLQ Inspector</h1>

      {/* 4-class breakdown row */}
      <div
        data-testid="dlq-breakdown"
        className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4"
      >
        {ERROR_CLASSES.map(({ key, label, color, icon: Icon }) => (
          <div
            key={key}
            className="rounded-card border border-border-1 bg-surface-2 px-4 py-3"
          >
            <div className="mb-2 flex items-center justify-between">
              <span className="text-[10px] font-medium uppercase tracking-wider text-fg-3">
                {label}
              </span>
              <Icon size={13} className="text-fg-3 opacity-60" />
            </div>
            <div className={cn('text-[22px] font-bold leading-none', color)}>
              {byClass[key] ?? 0}
            </div>
          </div>
        ))}
      </div>

      {/* Search bar */}
      <div className="mb-4">
        <SearchBar
          placeholder="Search by DLQ ID, error class, or topic…"
          onChange={setSearch}
          className="max-w-[340px]"
        />
      </div>

      {/* Data table */}
      <div className="rounded-card border border-border-1 bg-surface-2">
        <DataTable
          columns={columns}
          rows={filteredRows}
          onRowClick={handleRowClick}
          emptyMessage="No DLQ entries"
          loading={isLoading}
          hasMore={listData?.has_more ?? false}
        />
      </div>
    </div>
  );
}
