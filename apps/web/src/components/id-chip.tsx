'use client';

import { Check, Copy } from 'lucide-react';
import { useState } from 'react';
import { truncateId } from '~/lib/format/id';
import { cn } from '~/lib/utils';

interface IdChipProps {
  value: string;
  full?: boolean;
  className?: string;
}

export function IdChip({ value, full = false, className }: IdChipProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard?.writeText(value).catch(() => {});
    setCopied(true);
    setTimeout(() => setCopied(false), 1000);
  };

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
          copied ? 'text-[#86EFAC]' : 'text-fg-3 hover:text-fg-2',
        )}
      >
        {copied ? <Check size={10} /> : <Copy size={10} />}
      </button>
    </span>
  );
}
