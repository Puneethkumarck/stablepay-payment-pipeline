'use client';

import type { Route } from 'next';
import { useRouter } from 'next/navigation';
import { IdChip } from '~/components/id-chip';
import { PageHeader } from '~/components/layout/page-header';
import { LegStepper, type LegState } from '~/components/leg-stepper';
import { StatusBadge } from '~/components/status-badge';
import { Card } from '~/components/ui/card';
import { formatAbsoluteTooltip, formatRelativeTime } from '~/lib/format/time';
import { formatMoney } from '~/lib/format/money';
import { useFlowDetail } from '~/lib/hooks/use-flow-detail';
import { isTerminal } from '~/lib/terminal-status';
import { cn } from '~/lib/utils';
import type { FlowDto, FlowLegDto } from '~/types/api';

interface FlowDetailProps {
  flowId: string;
  initialData: FlowDto;
}

function legLabel(leg: FlowLegDto): string {
  return `${leg.type.charAt(0)}${leg.type.slice(1).toLowerCase()} ${leg.direction.toLowerCase()}`;
}

function mapLegs(legs: FlowLegDto[]): LegState[] {
  return legs
    .slice()
    .sort((a, b) => a.leg_index - b.leg_index)
    .map((leg) => ({
      label: legLabel(leg),
      status: leg.status,
      detail: leg.transaction_ref,
    }));
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
      className={cn(
        'p-[10px_16px]',
        borderBottom && 'border-b border-[rgba(255,255,255,0.05)]',
        borderRight && 'border-r border-[rgba(255,255,255,0.05)]',
      )}
    >
      <div className="mb-1 text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-3">
        {label}
      </div>
      <div
        className={cn(
          'break-all text-[12px] font-medium leading-[1.3] text-fg-1',
          mono ? 'font-mono' : 'font-sans',
        )}
      >
        {value}
      </div>
    </div>
  );
}

export function FlowDetail({ flowId, initialData }: FlowDetailProps) {
  const router = useRouter();
  const { data } = useFlowDetail(flowId, initialData);
  const flow = data ?? initialData;

  const legStates = mapLegs(flow.legs);

  return (
    <div className="page">
      <PageHeader
        title={`Flow ${flow.id}`}
        eyebrow={flow.flow_type.replaceAll('_', ' ')}
        onBack={() => router.push('/flows' as Route)}
      />

      {/* LegStepper */}
      <div className="mb-4">
        <LegStepper legs={legStates} flowStatus={flow.status} />
      </div>

      {/* Flow metadata */}
      <Card data-testid="metadata-grid" className="p-0">
        <div className="grid grid-cols-2">
          <MetadataCell
            label="flow_id"
            value={<IdChip value={flow.id} />}
            mono
            borderRight
          />
          <MetadataCell
            label="customer_id"
            value={<IdChip value={flow.customer_id} />}
            mono
          />
          <MetadataCell
            label="flow_type"
            value={flow.flow_type}
            borderRight
          />
          <MetadataCell
            label="status"
            value={<StatusBadge status={flow.status} />}
          />
          <MetadataCell
            label="source_amount"
            value={formatMoney(flow.source_amount.amount, flow.source_amount.currency)}
            mono
            borderRight
          />
          <MetadataCell
            label="destination_amount"
            value={
              flow.destination_amount
                ? formatMoney(flow.destination_amount.amount, flow.destination_amount.currency)
                : '—'
            }
            mono
          />
          <MetadataCell
            label="created"
            value={
              <span title={formatAbsoluteTooltip(flow.created_at)}>
                {formatRelativeTime(flow.created_at)}
              </span>
            }
            borderBottom={false}
            borderRight
          />
          <MetadataCell
            label="updated"
            value={
              <span title={formatAbsoluteTooltip(flow.updated_at)}>
                {formatRelativeTime(flow.updated_at)}
              </span>
            }
            borderBottom={false}
          />
        </div>
      </Card>

      {/* Polling footer */}
      <div data-testid="polling-footer" className="mt-3 text-[11px] tracking-wide text-fg-4">
        {isTerminal(flow.status) ? 'Terminal state reached' : '3s polling active'}
        {' '}&middot;{' '}
        <span title={formatAbsoluteTooltip(flow.updated_at)}>
          updated {formatRelativeTime(flow.updated_at)}
        </span>
      </div>
    </div>
  );
}
