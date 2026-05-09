'use client';

import { ArrowLeft } from 'lucide-react';
import Link from 'next/link';
import type { Route } from 'next';
import { DlqErrorBlock } from '~/components/dlq-error-block';
import { ReplayButton } from '~/components/dlq/replay-button';
import { KVRow } from '~/components/kv-row';
import { StatusBadge } from '~/components/status-badge';
import { buttonVariants } from '~/components/ui/button';
import { useDlqDetail } from '~/lib/hooks/use-dlq-detail';
import type { DlqEntryDto } from '~/types/api';

interface DlqDetailProps {
  dlqId: string;
  initialData: DlqEntryDto;
}

export function DlqDetail({ dlqId, initialData }: DlqDetailProps) {
  const { data: entry } = useDlqDetail(dlqId, initialData);

  if (!entry) return null;

  return (
    <div className="page" data-testid="dlq-detail-page">
      {/* Back link */}
      <Link
        href={'/admin/dlq' as Route}
        className={buttonVariants({ variant: 'ghost', size: 'sm' })}
        data-testid="dlq-back-link"
      >
        <ArrowLeft size={14} />
        Back to DLQ
      </Link>

      {/* Header */}
      <div className="mt-4 mb-5">
        <div className="sp-eyebrow mb-1 text-[10px]">DLQ Entry</div>
        <h1 className="text-[22px] font-bold tracking-tight text-fg-1">{entry.id}</h1>
      </div>

      {/* KV grid */}
      <div
        data-testid="dlq-kv-grid"
        className="rounded-card border border-border-1 bg-surface-2 px-5 py-2"
      >
        <KVRow label="DLQ ID" value={entry.id} />
        <KVRow
          label="Error class"
          value={<StatusBadge status={entry.error_class} />}
          mono={false}
        />
        <KVRow label="Source topic" value={entry.topic} />
        <KVRow label="Event key" value={entry.event_key} />
        <KVRow label="Retry count" value={String(entry.retry_count)} mono={false} />
        <KVRow
          label="Failed at"
          value={new Date(entry.created_at).toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
          })}
          mono={false}
        />
        <KVRow
          label="Updated at"
          value={new Date(entry.updated_at).toLocaleString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
          })}
          mono={false}
          last
        />
      </div>

      {/* Error message block */}
      <div className="mt-5">
        <h2 className="sp-eyebrow mb-3 text-[10px]">Error message</h2>
        <DlqErrorBlock message={entry.error_message} />
      </div>

      {/* Event payload */}
      <div className="mt-5">
        <h2 className="sp-eyebrow mb-3 text-[10px]">Event payload</h2>
        <DlqErrorBlock message={entry.event_payload} />
      </div>

      {/* Replay action */}
      <div
        data-testid="dlq-replay-section"
        className="mt-5 rounded-card border border-border-1 bg-surface-2 px-5 py-4"
      >
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-[13px] font-semibold text-fg-1">Replay</h2>
            <p className="mt-0.5 text-[11px] text-fg-3">
              Re-publish this event to the source topic for reprocessing.
            </p>
          </div>
          <ReplayButton
            dlqId={entry.id}
            errorClass={entry.error_class}
            retryCount={entry.retry_count}
          />
        </div>
      </div>
    </div>
  );
}
