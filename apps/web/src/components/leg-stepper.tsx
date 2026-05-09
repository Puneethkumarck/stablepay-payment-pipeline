import { ChevronRight } from 'lucide-react';
import { StatusBadge } from '~/components/status-badge';
import { cn } from '~/lib/utils';

export interface LegState {
  label: string;
  status: string;
  detail?: string;
}

type FlowStatus = 'COMPENSATION_INITIATED' | 'COMPENSATION_COMPLETED' | string;

const FAILED_STATUSES = new Set(['FAILED', 'CANCELLED', 'EXPIRED']);

interface LegStepperProps {
  legs: LegState[];
  flowStatus?: FlowStatus;
  className?: string;
}

export function LegStepper({ legs, flowStatus, className }: LegStepperProps) {
  const failedIndex = legs.findIndex((leg) => FAILED_STATUSES.has(leg.status));

  return (
    <div data-testid="leg-stepper" className={className}>
      {flowStatus === 'COMPENSATION_INITIATED' && (
        <div
          data-testid="compensation-banner"
          className="mb-4 flex items-center gap-2 rounded-md border border-[rgba(245,158,11,0.18)] bg-[rgba(245,158,11,0.06)] px-4 py-3 text-[13px] text-[#FCD34D]"
        >
          <span className="inline-block size-3 animate-spin rounded-full border-2 border-current border-t-transparent" />
          Compensation in progress — partner refund initiated
        </div>
      )}
      {flowStatus === 'COMPENSATION_COMPLETED' && (
        <div
          data-testid="compensation-banner"
          className="mb-4 rounded-md border border-[rgba(34,197,94,0.22)] bg-[rgba(34,197,94,0.06)] px-4 py-3 text-[13px] text-[#86EFAC]"
        >
          Compensation complete
        </div>
      )}
      <div className="flex items-stretch gap-2">
        {legs.map((leg, i) => {
          const isFailed = FAILED_STATUSES.has(leg.status);
          const isSkipped =
            failedIndex >= 0 && i > failedIndex && flowStatus !== 'COMPENSATION_COMPLETED';

          return (
            <div key={i} className="flex items-stretch">
              <div
                data-testid="leg-card"
                className={cn(
                  'flex min-w-[160px] flex-1 flex-col gap-2 rounded-card border bg-surface-2 p-4',
                  isFailed ? 'border-[rgba(239,68,68,0.32)]' : 'border-border-1',
                  isSkipped && 'opacity-50',
                )}
              >
                <div className="text-[13px] font-semibold text-fg-1">{leg.label}</div>
                <div className="flex items-center gap-2">
                  <StatusBadge status={isSkipped ? 'CANCELLED' : leg.status} />
                  {isSkipped && <span className="text-[11px] font-medium text-fg-3">Skipped</span>}
                </div>
                {leg.detail && <div className="font-mono text-[11px] text-fg-3">{leg.detail}</div>}
              </div>
              {i < legs.length - 1 && (
                <div className="flex items-center px-1 text-fg-3">
                  <ChevronRight size={16} />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
