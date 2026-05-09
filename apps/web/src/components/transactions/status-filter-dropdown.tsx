'use client';

import { ChevronDown, X } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { getStatusStyle } from '~/lib/status-config';
import { cn } from '~/lib/utils';

interface StatusGroup {
  label: string;
  statuses: string[];
}

const STATUS_GROUPS: StatusGroup[] = [
  {
    label: 'Approval',
    statuses: ['PENDING_APPROVAL', 'FIRST_APPROVAL', 'PENDING_SECOND_APPROVAL', 'APPROVED'],
  },
  {
    label: 'Screening',
    statuses: [
      'PENDING_SCREENING',
      'SCREENING_IN_PROGRESS',
      'SCREENING_HOLD',
      'SCREENING_RFI',
      'SCREENING_RELEASED',
      'SCREENING_CLEARED',
      'SCREENING_REJECTED',
      'SCREENING_SEIZED',
      'SCREENING_FLAGGED',
      'SCREENING_HELD',
      'MANUAL_REVIEW',
    ],
  },
  {
    label: 'Partner routing',
    statuses: [
      'PENDING_PARTNER_ROUTING',
      'PARTNER_ASSIGNED',
      'PENDING_EXECUTION',
      'EXECUTING',
      'SENT_TO_PARTNER',
      'PARTNER_ACKNOWLEDGED',
      'PENDING_CONFIRMATION',
    ],
  },
  {
    label: 'Refund / exception',
    statuses: [
      'RETURNED',
      'REFUND_INITIATED',
      'REFUND_PENDING',
      'REFUND_COMPLETED',
      'LEDGER_SUSPENSE',
    ],
  },
  {
    label: 'Terminal',
    statuses: [
      'INITIATED',
      'COMPLETED',
      'FAILED',
      'CANCELLED',
      'CONFIRMED',
      'SUSPENDED',
      'CONFISCATED',
      'EXPIRED',
      'STUCK',
    ],
  },
  {
    label: 'Payin',
    statuses: ['DETECTED', 'MATCHED', 'PENDING_ALLOCATION', 'ALLOCATED'],
  },
  {
    label: 'Crypto-specific',
    statuses: [
      'PENDING_SIGNING',
      'SIGNING_IN_PROGRESS',
      'SIGNED',
      'BROADCASTING',
      'BROADCAST',
      'CONFIRMING',
      'RBF_INITIATED',
      'RBF_BROADCAST',
      'REPLACED',
    ],
  },
  {
    label: 'Customer-facing',
    statuses: ['PENDING', 'PROCESSING', 'REFUNDED'],
  },
];

interface StatusFilterDropdownProps {
  selected: string[];
  onChange: (statuses: string[]) => void;
  className?: string;
}

export { STATUS_GROUPS };

export function StatusFilterDropdown({ selected, onChange, className }: StatusFilterDropdownProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const toggle = useCallback(
    (status: string) => {
      const next = selected.includes(status)
        ? selected.filter((s) => s !== status)
        : [...selected, status];
      onChange(next);
    },
    [selected, onChange],
  );

  useEffect(() => {
    if (!open) return;
    const handler = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [open]);

  const triggerLabel = selected.length === 0 ? 'All statuses' : `${selected.length} selected`;

  return (
    <div ref={containerRef} className={cn('relative', className)} data-testid="status-filter">
      <button
        type="button"
        onClick={() => setOpen((p) => !p)}
        aria-expanded={open}
        aria-haspopup="listbox"
        className="flex items-center gap-1.5 rounded-[9px] border border-border-1 bg-[rgba(255,255,255,0.05)] px-3 py-[7px] text-[13px] text-fg-2 transition-colors hover:border-border-2"
      >
        {triggerLabel}
        <ChevronDown size={13} className={cn('transition-transform', open && 'rotate-180')} />
      </button>

      {selected.length > 0 && (
        <button
          type="button"
          onClick={() => onChange([])}
          aria-label="Clear status filter"
          className="ml-1 inline-flex size-[22px] items-center justify-center rounded-full text-fg-3 transition-colors hover:bg-[rgba(255,255,255,0.08)] hover:text-fg-2"
        >
          <X size={12} />
        </button>
      )}

      {open && (
        <div
          role="listbox"
          aria-multiselectable="true"
          className="absolute top-full left-0 z-50 mt-1 max-h-[360px] w-[260px] overflow-y-auto rounded-lg border border-border-2 bg-surface-1 p-1 shadow-lg"
        >
          {STATUS_GROUPS.map((group) => (
            <div key={group.label} className="py-1">
              <div className="px-2 py-1 text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-4">
                {group.label}
              </div>
              {group.statuses.map((status) => {
                const style = getStatusStyle(status);
                const checked = selected.includes(status);
                return (
                  <button
                    key={status}
                    type="button"
                    role="option"
                    aria-selected={checked}
                    onClick={() => toggle(status)}
                    className="flex w-full cursor-pointer items-center gap-2 rounded-md px-2 py-[5px] text-left text-[12px] transition-colors hover:bg-[rgba(255,255,255,0.05)]"
                  >
                    <span
                      className={cn(
                        'flex size-[14px] shrink-0 items-center justify-center rounded-[3px] border transition-colors',
                        checked
                          ? 'border-solana-purple bg-solana-purple text-white'
                          : 'border-border-2 bg-transparent',
                      )}
                    >
                      {checked && (
                        <svg
                          width="10"
                          height="10"
                          viewBox="0 0 10 10"
                          fill="none"
                          role="img"
                          aria-label="Selected"
                        >
                          <path
                            d="M2 5L4.5 7.5L8 3"
                            stroke="currentColor"
                            strokeWidth="1.5"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                      )}
                    </span>
                    <span className="text-fg-2">{style.label}</span>
                  </button>
                );
              })}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
