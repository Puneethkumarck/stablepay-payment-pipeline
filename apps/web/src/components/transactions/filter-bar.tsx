'use client';

import { useCallback } from 'react';
import { SearchBar } from '~/components/search-bar';
import { StatusFilterDropdown } from '~/components/transactions/status-filter-dropdown';
import { cn } from '~/lib/utils';

const QUICK_CHIPS = ['SENT_TO_PARTNER', 'COMPLETED', 'FAILED', 'SCREENING_HOLD', 'STUCK'] as const;

const CHIP_LABELS: Record<string, string> = {
  SENT_TO_PARTNER: 'Sent to partner',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
  SCREENING_HOLD: 'Screening hold',
  STUCK: 'Stuck',
};

interface FilterBarProps {
  search: string;
  onSearchChange: (value: string) => void;
  selectedStatuses: string[];
  onStatusesChange: (statuses: string[]) => void;
}

export function FilterBar({
  search,
  onSearchChange,
  selectedStatuses,
  onStatusesChange,
}: FilterBarProps) {
  const toggleChip = useCallback(
    (status: string) => {
      const next = selectedStatuses.includes(status)
        ? selectedStatuses.filter((s) => s !== status)
        : [...selectedStatuses, status];
      onStatusesChange(next);
    },
    [selectedStatuses, onStatusesChange],
  );

  return (
    <div data-testid="filter-bar" className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2">
        <SearchBar
          placeholder="Search transactions..."
          value={search}
          onChange={onSearchChange}
          className="w-full sm:w-[260px]"
        />
        <StatusFilterDropdown selected={selectedStatuses} onChange={onStatusesChange} />
      </div>
      <div className="flex flex-wrap gap-[6px]">
        {QUICK_CHIPS.map((status) => {
          const active = selectedStatuses.includes(status);
          return (
            <button
              key={status}
              type="button"
              onClick={() => toggleChip(status)}
              data-testid={`chip-${status}`}
              className={cn(
                'rounded-full border px-3 py-[5px] text-[12px] font-medium transition-colors',
                active
                  ? 'border-[rgba(153,69,255,0.36)] bg-[rgba(153,69,255,0.14)] text-[#C4B5FD]'
                  : 'border-[rgba(255,255,255,0.10)] bg-[rgba(255,255,255,0.05)] text-[rgba(255,255,255,0.40)] hover:border-[rgba(255,255,255,0.18)] hover:text-[rgba(255,255,255,0.60)]',
              )}
            >
              {CHIP_LABELS[status]}
            </button>
          );
        })}
      </div>
    </div>
  );
}
