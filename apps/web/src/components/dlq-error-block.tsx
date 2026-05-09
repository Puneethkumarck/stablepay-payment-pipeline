'use client';

import { Check, Copy } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { cn } from '~/lib/utils';

interface DlqErrorBlockProps {
  message: string;
  className?: string;
}

export function DlqErrorBlock({ message, className }: DlqErrorBlockProps) {
  const [copied, setCopied] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(null);

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  const handleCopy = useCallback(() => {
    if (!navigator.clipboard?.writeText) return;
    navigator.clipboard.writeText(message).then(
      () => {
        setCopied(true);
        timerRef.current = setTimeout(() => setCopied(false), 1500);
      },
      () => {},
    );
  }, [message]);

  return (
    <div data-testid="dlq-error-block" className={cn('relative group', className)}>
      <pre className="max-h-[240px] overflow-y-auto rounded-md border border-border-1 bg-surface-0 p-3 font-mono text-[12px] text-fg-2 whitespace-pre-wrap break-words">
        {message}
      </pre>
      <button
        type="button"
        onClick={handleCopy}
        data-testid="dlq-error-copy"
        className={cn(
          'absolute top-2 right-2 flex items-center gap-1 rounded-md border border-border-1 bg-surface-2 px-2 py-1 text-[11px] font-medium opacity-0 transition-opacity duration-[120ms] group-hover:opacity-100',
          copied ? 'text-green-400' : 'text-fg-3 hover:text-fg-2',
        )}
      >
        {copied ? <Check size={12} /> : <Copy size={12} />}
        {copied ? 'Copied' : 'Copy'}
      </button>
    </div>
  );
}
