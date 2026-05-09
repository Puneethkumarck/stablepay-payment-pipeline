'use client';

import { getStatusStyle, type StatusColor } from '~/lib/status-config';
import { cn } from '~/lib/utils';

const colorClasses: Record<StatusColor, { bg: string; border: string; text: string; dot: string }> =
  {
    info: {
      bg: 'bg-sky-400/10',
      border: 'border-sky-400/22',
      text: 'text-sky-300',
      dot: 'bg-sky-400',
    },
    purple: {
      bg: 'bg-violet-500/12',
      border: 'border-violet-500/30',
      text: 'text-violet-300',
      dot: 'bg-violet-500',
    },
    success: {
      bg: 'bg-green-500/10',
      border: 'border-green-500/22',
      text: 'text-green-300',
      dot: 'bg-green-500',
    },
    warning: {
      bg: 'bg-amber-500/10',
      border: 'border-amber-500/24',
      text: 'text-amber-300',
      dot: 'bg-amber-500',
    },
    danger: {
      bg: 'bg-red-500/10',
      border: 'border-red-500/24',
      text: 'text-red-300',
      dot: 'bg-red-500',
    },
    neutral: {
      bg: 'bg-white/5',
      border: 'border-white/10',
      text: 'text-white/40',
      dot: 'bg-white/24',
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
      data-color={style.color}
      data-pulse={style.pulse || undefined}
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
