'use client';

import { Check, Copy } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { truncateId } from '~/lib/format/id';
import { cn } from '~/lib/utils';

interface IdChipProps {
  value: string;
  full?: boolean;
  className?: string;
}

export function IdChip({ value, full = false, className }: IdChipProps) {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(null);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  const handleCopy = useCallback(
    (e: React.MouseEvent) => {
      e.stopPropagation();
      if (!navigator.clipboard?.writeText) return;
      navigator.clipboard.writeText(value).then(
        () => {
          setCopied(true);
          timerRef.current = setTimeout(() => setCopied(false), 1000);
        },
        () => {},
      );
    },
    [value],
  );

  return (
    <span
      data-testid="id-chip"
      className={cn(
        'inline-flex items-center gap-[5px] font-mono text-[11px] text-fg-3',
        className,
      )}
    >
      {full ? value : truncateId(value)}
      <button
        type="button"
        onClick={handleCopy}
        data-testid="id-chip-copy"
        className={cn(
          'cursor-pointer transition-colors duration-[120ms]',
          copied ? 'text-green-400' : 'text-fg-3 hover:text-fg-2',
        )}
      >
        {copied ? <Check size={10} /> : <Copy size={10} />}
      </button>
    </span>
  );
}
