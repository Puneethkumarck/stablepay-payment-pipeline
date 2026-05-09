'use client';

import type { Route } from 'next';
import { useRouter } from 'next/navigation';
import { useCallback, useMemo, useState } from 'react';
import { Amount } from '~/components/amount';
import { type ColumnConfig, DataTable } from '~/components/data-table';
import { PageHeader } from '~/components/layout/page-header';
import { StatusBadge } from '~/components/status-badge';
import { FilterBar } from '~/components/transactions/filter-bar';
import { formatRelativeTime } from '~/lib/format/time';
import { useTransactionsList } from '~/lib/hooks/use-transactions-list';
import type { TransactionDto } from '~/types/api';

type TxnRow = TransactionDto & Record<string, unknown>;

const PAGE_SIZE = 50;

const columns: ColumnConfig<TxnRow>[] = [
  {
    key: 'ref',
    label: 'Reference',
    width: '20%',
    render: (_, row) => <span className="font-mono text-xs text-fg-2">{(row as TxnRow).ref}</span>,
  },
  {
    key: 'type',
    label: 'Type',
    width: '12%',
    render: (_, row) => {
      const txn = row as TxnRow;
      return (
        <span className="text-xs text-fg-3">
          {txn.type} {txn.direction}
        </span>
      );
    },
  },
  {
    key: 'amount',
    label: 'Amount',
    width: '15%',
    render: (_, row) => {
      const txn = row as TxnRow;
      return <Amount value={txn.amount.amount} currency={txn.amount.currency} size="sm" />;
    },
  },
  {
    key: 'internal_status',
    label: 'Status',
    width: '18%',
    render: (_, row) => <StatusBadge status={(row as TxnRow).internal_status} />,
  },
  {
    key: 'counterparty',
    label: 'Counterparty',
    width: '18%',
    render: (_, row) => {
      const txn = row as TxnRow;
      return <span className="text-xs text-fg-3">{txn.counterparty ?? '—'}</span>;
    },
  },
  {
    key: 'created_at',
    label: 'Time',
    width: '17%',
    render: (_, row) => (
      <span className="text-xs text-fg-3">{formatRelativeTime((row as TxnRow).created_at)}</span>
    ),
  },
];

export function TransactionsList() {
  const router = useRouter();
  const [search, setSearch] = useState('');
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>([]);
  const [pages, setPages] = useState<TxnRow[][]>([]);
  const [cursor, setCursor] = useState<string | null>(null);

  const criteria = useMemo(
    () => ({
      limit: PAGE_SIZE,
      search: search || undefined,
      status: selectedStatuses.length > 0 ? selectedStatuses.join(',') : undefined,
    }),
    [search, selectedStatuses],
  );

  const { data, isLoading } = useTransactionsList(criteria);

  const firstPageRows = useMemo(() => (data?.data ?? []) as TxnRow[], [data]);
  const allRows = useMemo(() => [...firstPageRows, ...pages.flat()], [firstPageRows, pages]);
  const hasMore = pages.length > 0 ? cursor !== null : (data?.has_more ?? false);
  const totalHint = data?.data?.length ?? 0;

  const handleLoadMore = useCallback(() => {
    const nextCursor = pages.length > 0 ? cursor : data?.next_cursor;
    if (!nextCursor) return;

    const params = new URLSearchParams();
    params.set('cursor', nextCursor);
    params.set('limit', String(PAGE_SIZE));
    if (criteria.status) params.set('status', criteria.status);
    if (criteria.search) params.set('search', criteria.search);

    fetch(`/api/v1/transactions?${params.toString()}`, {
      credentials: 'include',
    })
      .then((r) => r.json())
      .then((page: { data: TxnRow[]; next_cursor: string | null; has_more: boolean }) => {
        setPages((prev) => [...prev, page.data]);
        setCursor(page.has_more ? page.next_cursor : null);
      });
  }, [pages, cursor, data?.next_cursor, criteria]);

  const handleSearchChange = useCallback((value: string) => {
    setSearch(value);
    setPages([]);
    setCursor(null);
  }, []);

  const handleStatusesChange = useCallback((statuses: string[]) => {
    setSelectedStatuses(statuses);
    setPages([]);
    setCursor(null);
  }, []);

  return (
    <div className="page">
      <PageHeader title="Transactions" eyebrow="All transactions" />
      <FilterBar
        search={search}
        onSearchChange={handleSearchChange}
        selectedStatuses={selectedStatuses}
        onStatusesChange={handleStatusesChange}
      />
      <div className="mt-4">
        <DataTable
          columns={columns}
          rows={allRows}
          loading={isLoading}
          hasMore={hasMore}
          onLoadMore={handleLoadMore}
          onRowClick={(row) => router.push(`/transactions/${row.ref}` as Route)}
          emptyMessage="No transactions match your filters"
        />
      </div>
      <div data-testid="transactions-footer" className="mt-3 text-[11px] tracking-wide text-fg-4">
        3s polling active &middot; {allRows.length} of {totalHint} shown
      </div>
    </div>
  );
}
