import { ArrowLeft } from 'lucide-react';
import { cn } from '~/lib/utils';

interface PageHeaderProps {
  title: string;
  eyebrow?: string;
  actions?: React.ReactNode;
  onBack?: () => void;
  className?: string;
}

export function PageHeader({ title, eyebrow, actions, onBack, className }: PageHeaderProps) {
  return (
    <div
      data-testid="page-header"
      className={cn('mb-6 flex items-center justify-between gap-4', className)}
    >
      <div className="flex items-center gap-[10px]">
        {onBack && (
          <button
            type="button"
            onClick={onBack}
            aria-label="Back"
            data-testid="page-header-back"
            className="grid size-[30px] shrink-0 cursor-pointer place-items-center rounded-md border border-border-1 bg-[rgba(255,255,255,0.05)] text-fg-2 transition-colors duration-[120ms] hover:border-border-2 hover:text-fg-1"
          >
            <ArrowLeft size={14} />
          </button>
        )}
        <div>
          {eyebrow && <div className="sp-eyebrow mb-[3px]">{eyebrow}</div>}
          <h1 className="text-[20px] font-bold leading-[1.2] tracking-tight">{title}</h1>
        </div>
      </div>
      {actions && <div className="flex shrink-0 gap-2">{actions}</div>}
    </div>
  );
}
