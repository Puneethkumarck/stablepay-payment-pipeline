import { Database, type LucideIcon } from 'lucide-react';
import { cn } from '~/lib/utils';

interface EmptyProps {
  icon?: LucideIcon;
  title: string;
  sub?: string;
  className?: string;
}

export function Empty({ icon: Icon = Database, title, sub, className }: EmptyProps) {
  return (
    <div
      data-testid="empty"
      className={cn(
        'flex flex-col items-center justify-center gap-[10px] px-6 py-12 text-center',
        className,
      )}
    >
      <Icon size={28} className="text-fg-4" strokeWidth={1.6} />
      <div className="mt-1 text-[14px] font-semibold text-fg-3">{title}</div>
      {sub && <div className="max-w-[240px] text-[12px] leading-[1.5] text-fg-4">{sub}</div>}
    </div>
  );
}
