'use client';

import type { Route } from 'next';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowRight } from 'lucide-react';
import { useTransactionsList } from '~/lib/hooks/use-transactions-list';
import { Amount } from '~/components/amount';
import { StatusBadge } from '~/components/status-badge';
import { DataTable, type ColumnConfig } from '~/components/data-table';
import { buttonVariants } from '~/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '~/components/ui/card';
import { formatRelativeTime } from '~/lib/format/time';
import type { TransactionDto } from '~/types/api';

type TxnRow = TransactionDto & Record<string, unknown>;

const columns: ColumnConfig<TxnRow>[] = [
  {
    key: 'ref',
    label: 'Ref',
    width: '25%',
    render: (_, row) => (
      <span className="font-mono text-xs text-fg-2">{(row as TxnRow).ref}</span>
    ),
  },
  {
    key: 'type',
    label: 'Type',
    width: '15%',
    render: (_, row) => (
      <span className="text-xs text-fg-3">
        {(row as TxnRow).type} {(row as TxnRow).direction}
      </span>
    ),
  },
  {
    key: 'amount',
    label: 'Amount',
    width: '20%',
    render: (_, row) => {
      const txn = row as TxnRow;
      return <Amount value={txn.amount.amount} currency={txn.amount.currency} size="sm" />;
    },
  },
  {
    key: 'internal_status',
    label: 'Status',
    width: '20%',
    render: (_, row) => <StatusBadge status={(row as TxnRow).internal_status} />,
  },
  {
    key: 'created_at',
    label: 'Time',
    width: '20%',
    render: (_, row) => (
      <span className="text-xs text-fg-3">{formatRelativeTime((row as TxnRow).created_at)}</span>
    ),
  },
];

export function RecentTxnsCard() {
  const router = useRouter();
  const { data, isLoading } = useTransactionsList({ limit: 6 });

  const rows = (data?.data ?? []) as TxnRow[];

  return (
    <Card data-testid="recent-txns-card">
      <CardHeader className="flex-row items-center justify-between pb-3">
        <CardTitle className="text-sm font-semibold">Recent transactions</CardTitle>
        <Link
          href={'/transactions' as Route}
          className={buttonVariants({ variant: 'ghost', size: 'sm' })}
        >
          View all <ArrowRight size={14} />
        </Link>
      </CardHeader>
      <CardContent className="px-0 pb-0">
        <DataTable
          columns={columns}
          rows={rows}
          loading={isLoading}
          onRowClick={(row) => router.push(`/transactions/${row.ref}` as Route)}
          emptyMessage="No transactions yet"
        />
      </CardContent>
    </Card>
  );
}
