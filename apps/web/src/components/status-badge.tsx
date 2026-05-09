'use client';

import { getStatusStyle, type StatusColor } from '~/lib/status-config';
import { cn } from '~/lib/utils';

const colorClasses: Record<StatusColor, { bg: string; border: string; text: string; dot: string }> =
  {
    info: {
      bg: 'bg-[rgba(56,189,248,0.10)]',
      border: 'border-[rgba(56,189,248,0.22)]',
      text: 'text-[#7DD3FC]',
      dot: 'bg-[#38BDF8]',
    },
    purple: {
      bg: 'bg-[rgba(153,69,255,0.12)]',
      border: 'border-[rgba(153,69,255,0.30)]',
      text: 'text-[#C4B5FD]',
      dot: 'bg-[#9945FF]',
    },
    success: {
      bg: 'bg-[rgba(34,197,94,0.10)]',
      border: 'border-[rgba(34,197,94,0.22)]',
      text: 'text-[#86EFAC]',
      dot: 'bg-[#22C55E]',
    },
    warning: {
      bg: 'bg-[rgba(245,158,11,0.10)]',
      border: 'border-[rgba(245,158,11,0.24)]',
      text: 'text-[#FCD34D]',
      dot: 'bg-[#F59E0B]',
    },
    danger: {
      bg: 'bg-[rgba(239,68,68,0.10)]',
      border: 'border-[rgba(239,68,68,0.24)]',
      text: 'text-[#FCA5A5]',
      dot: 'bg-[#EF4444]',
    },
    neutral: {
      bg: 'bg-[rgba(255,255,255,0.05)]',
      border: 'border-[rgba(255,255,255,0.10)]',
      text: 'text-[rgba(255,255,255,0.40)]',
      dot: 'bg-[rgba(255,255,255,0.24)]',
    },
  };

interface StatusBadgeProps {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const style = getStatusStyle(status);
  const colors = colorClasses[style.color];

  return (
    <span
      data-testid="status-badge"
      className={cn(
        'inline-flex items-center gap-[5px] rounded-full border px-[9px] py-[3px] text-[11px] font-medium whitespace-nowrap tracking-[0.01em]',
        colors.bg,
        colors.border,
        colors.text,
        className,
      )}
    >
      <span
        className={cn(
          'size-[5px] shrink-0 rounded-full',
          colors.dot,
          style.pulse && 'animate-[badge-pulse_2s_ease-in-out_infinite]',
        )}
      />
      {style.label}
    </span>
  );
}
