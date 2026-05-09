import { Check } from 'lucide-react';
import { cn } from '~/lib/utils';

export interface TimelineStep {
  label: string;
  sub?: string;
  meta?: React.ReactNode;
  state: 'done' | 'live' | 'pending';
}

interface TimelineProps {
  steps: TimelineStep[];
  className?: string;
}

export function Timeline({ steps, className }: TimelineProps) {
  return (
    <div data-testid="timeline" className={className}>
      {steps.map((step, i) => {
        const isLast = i === steps.length - 1;
        return (
          <div key={i} data-state={step.state} className={cn('relative flex gap-3', !isLast && 'pb-[18px]')}>
            {!isLast && <div className="absolute bottom-0 left-[9px] top-5 w-px bg-border-1" />}
            <div
              className={cn(
                'z-[1] mt-[1px] grid size-[18px] shrink-0 place-items-center rounded-full',
                step.state === 'done' && 'bg-success',
                step.state === 'live' &&
                  'bg-gradient-to-br from-solana-purple to-solana-magenta shadow-[0_0_10px_rgba(153,69,255,0.45)] animate-[badge-pulse_2s_ease-in-out_infinite]',
                step.state === 'pending' && 'border border-border-2 bg-[rgba(255,255,255,0.06)]',
              )}
            >
              {step.state === 'done' && (
                <Check size={9} className="text-surface-0" strokeWidth={2.5} />
              )}
              {step.state === 'live' && <span className="size-[5px] rounded-full bg-white" />}
            </div>
            <div>
              <div
                className={cn(
                  'text-[13px] font-semibold leading-snug',
                  step.state === 'pending' ? 'text-fg-3' : 'text-fg-1',
                )}
              >
                {step.label}
              </div>
              {step.sub && (
                <div className="mt-[2px] font-mono text-[11px] text-fg-3">{step.sub}</div>
              )}
              {step.meta && <div className="mt-[6px]">{step.meta}</div>}
            </div>
          </div>
        );
      })}
    </div>
  );
}
