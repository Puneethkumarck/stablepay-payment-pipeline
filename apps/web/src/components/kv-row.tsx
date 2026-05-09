import { cn } from '~/lib/utils';

interface KVRowProps {
  label: string;
  value: React.ReactNode;
  mono?: boolean;
  last?: boolean;
}

export function KVRow({ label, value, mono = true, last = false }: KVRowProps) {
  return (
    <div
      data-testid="kv-row"
      data-last={last || undefined}
      data-mono={mono}
      className={cn(
        'flex items-center justify-between gap-4 py-[9px]',
        !last && 'border-b border-[rgba(255,255,255,0.05)]',
      )}
    >
      <span className="shrink-0 text-[12px] text-fg-3 whitespace-nowrap">{label}</span>
      <span
        className={cn(
          'text-right text-[12px] text-fg-1 break-all',
          mono ? 'font-mono' : 'font-sans',
        )}
      >
        {value}
      </span>
    </div>
  );
}
