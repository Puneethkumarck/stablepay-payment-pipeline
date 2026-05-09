import type { LucideIcon } from 'lucide-react';
import { cn } from '~/lib/utils';

interface StatCardProps {
  label: string;
  value: string | number;
  sub?: string;
  trend?: { up: boolean; label: string };
  icon?: LucideIcon;
  accentColor?: string;
  className?: string;
}

export function StatCard({
  label,
  value,
  sub,
  trend,
  icon: Icon,
  accentColor,
  className,
}: StatCardProps) {
  return (
    <div
      data-testid="stat-card"
      className={cn(
        'relative overflow-hidden rounded-card border border-border-1 bg-surface-2 px-5 py-[18px]',
        className,
      )}
    >
      {accentColor && (
        <div
          className="pointer-events-none absolute -top-[50px] -right-[30px] size-[110px] rounded-full"
          style={{
            background: `radial-gradient(circle, ${accentColor}22, transparent 70%)`,
          }}
        />
      )}
      <div className="mb-[14px] flex items-start justify-between">
        <span className="sp-eyebrow text-[10px]">{label}</span>
        {Icon && (
          <div className="opacity-70" style={{ color: accentColor ?? 'var(--color-fg-3)' }}>
            <Icon size={14} />
          </div>
        )}
      </div>
      <div className="sp-amount mb-[6px] text-[28px] font-bold leading-none tracking-[-0.025em] text-fg-1">
        {value}
      </div>
      {sub && <div className="text-[11px] text-fg-3">{sub}</div>}
      {trend && (
        <div
          className={cn(
            'mt-[10px] text-[11px] font-semibold',
            trend.up ? 'text-[#86EFAC]' : 'text-[#FCA5A5]',
          )}
        >
          {trend.up ? '↑' : '↓'} {trend.label}
        </div>
      )}
    </div>
  );
}
