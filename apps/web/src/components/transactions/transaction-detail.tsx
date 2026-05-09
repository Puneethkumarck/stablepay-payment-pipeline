'use client';

import type { Route } from 'next';
import { useRouter } from 'next/navigation';
import { Amount } from '~/components/amount';
import { IdChip } from '~/components/id-chip';
import { KVRow } from '~/components/kv-row';
import { PageHeader } from '~/components/layout/page-header';
import { StatusBadge } from '~/components/status-badge';
import { Timeline } from '~/components/timeline';
import { Card, CardContent } from '~/components/ui/card';
import { formatAbsoluteTooltip, formatRelativeTime } from '~/lib/format/time';
import { useTransactionDetail } from '~/lib/hooks/use-transaction-detail';
import { isTerminal } from '~/lib/terminal-status';
import { deriveTimeline } from '~/lib/timeline-derivation';
import type { TransactionDto } from '~/types/api';

interface TransactionDetailProps {
  txnRef: string;
  initialData: TransactionDto;
}

function MetadataCell({
  label,
  value,
  mono = false,
  borderRight = false,
  borderBottom = true,
}: {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
  borderRight?: boolean;
  borderBottom?: boolean;
}) {
  return (
    <div
      className={[
        'p-[10px_16px]',
        borderBottom ? 'border-b border-[rgba(255,255,255,0.05)]' : '',
        borderRight ? 'border-r border-[rgba(255,255,255,0.05)]' : '',
      ].join(' ')}
    >
      <div className="mb-1 text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-3">
        {label}
      </div>
      <div
        className={[
          'break-all text-[12px] font-medium leading-[1.3] text-fg-1',
          mono ? 'font-mono' : 'font-sans',
        ].join(' ')}
      >
        {value}
      </div>
    </div>
  );
}

export function TransactionDetail({ txnRef, initialData }: TransactionDetailProps) {
  const router = useRouter();
  const { data: txn } = useTransactionDetail(txnRef, initialData);
  const transaction = txn ?? initialData;

  const timelineSteps = deriveTimeline(
    transaction.internal_status,
    transaction.type,
    transaction.direction,
  );

  const flowLabel = transaction.source_topic
    ? transaction.source_topic.replace('payment.', '').replace('.v1', '')
    : `${transaction.type} ${transaction.direction}`.toLowerCase();

  return (
    <div className="page">
      <PageHeader
        title={`Transaction ${transaction.ref}`}
        eyebrow="Transaction detail"
        onBack={() => router.push('/transactions' as Route)}
      />

      {/* Hero row */}
      <div className="mb-4 grid grid-cols-[300px_1fr] gap-4">
        {/* Amount hero card */}
        <Card
          data-testid="hero-card"
          className="relative overflow-hidden border-[rgba(153,69,255,0.22)]"
          style={{
            background:
              'linear-gradient(140deg, rgba(153,69,255,0.12) 0%, rgba(0,255,163,0.05) 100%)',
          }}
        >
          <div
            className="pointer-events-none absolute -right-[50px] -top-[50px] size-[160px] rounded-full"
            style={{
              background: 'radial-gradient(circle, rgba(153,69,255,0.22), transparent 70%)',
            }}
          />
          <CardContent className="relative">
            <div className="mb-3 text-[10px] font-semibold uppercase tracking-[0.11em] text-[rgba(153,69,255,0.8)]">
              Amount sent
            </div>
            <Amount
              value={transaction.amount.amount}
              currency={transaction.amount.currency}
              size="xl"
              className="mb-1 block font-bold tracking-[-0.025em]"
            />
            <div className="mb-4 font-mono text-[11px] text-fg-3">
              {transaction.amount.currency} &middot; {flowLabel}
            </div>
            <div className="mt-[18px]">
              <StatusBadge status={transaction.internal_status} />
            </div>
          </CardContent>
        </Card>

        {/* Metadata grid */}
        <Card data-testid="metadata-grid" className="p-0">
          <div className="grid grid-cols-2">
            <MetadataCell
              label="customer_id"
              value={<IdChip value={transaction.customer_id} />}
              mono
              borderRight
            />
            <MetadataCell
              label="account_id"
              value={transaction.account_id ?? '—'}
              mono
            />
            <MetadataCell
              label="counterparty"
              value={transaction.counterparty ?? '—'}
              mono
              borderRight
            />
            <MetadataCell
              label="flow_id"
              value={<IdChip value={transaction.flow_id} />}
              mono
            />
            <MetadataCell
              label="flow_type (topic)"
              value={transaction.source_topic ?? `${transaction.type}_${transaction.direction}`}
              borderRight
            />
            <MetadataCell
              label="currency_code"
              value={transaction.amount.currency}
            />
            <MetadataCell
              label="provider / chain"
              value={transaction.provider ?? transaction.blockchain ?? '—'}
              borderBottom={false}
              borderRight
            />
            <MetadataCell
              label="created"
              value={
                <span title={formatAbsoluteTooltip(transaction.created_at)}>
                  {formatRelativeTime(transaction.created_at)}
                </span>
              }
              borderBottom={false}
            />
          </div>
        </Card>
      </div>

      {/* Bottom: timeline + EventEnvelope */}
      <div className="grid grid-cols-2 gap-4">
        {/* State-machine timeline */}
        <Card data-testid="timeline-card">
          <CardContent>
            <div className="mb-[18px] text-[13px] font-bold">State-machine timeline</div>
            <Timeline steps={timelineSteps} />
          </CardContent>
        </Card>

        {/* EventEnvelope fields */}
        <Card data-testid="envelope-card">
          <CardContent>
            <div className="mb-[14px] text-[13px] font-bold">EventEnvelope fields</div>
            <KVRow label="event_id" value={transaction.event_id ?? '—'} />
            <KVRow label="flow_id" value={transaction.flow_id} />
            <KVRow label="correlation_id" value={transaction.correlation_id ?? '—'} />
            <KVRow label="trace_id" value={transaction.trace_id ?? '—'} />
            <KVRow label="schema_version" value={transaction.schema_version ?? '—'} />
            <KVRow label="source_topic" value={transaction.source_topic ?? '—'} last />
          </CardContent>
        </Card>
      </div>

      {/* Polling footer */}
      <div data-testid="polling-footer" className="mt-3 text-[11px] tracking-wide text-fg-4">
        {isTerminal(transaction.internal_status) ? 'Terminal state reached' : '3s polling active'}
        {' '}&middot;{' '}
        <span title={formatAbsoluteTooltip(transaction.updated_at)}>
          updated {formatRelativeTime(transaction.updated_at)}
        </span>
      </div>
    </div>
  );
}
