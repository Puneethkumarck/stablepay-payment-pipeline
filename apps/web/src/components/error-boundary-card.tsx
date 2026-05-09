'use client';

import { AlertTriangle } from 'lucide-react';
import { cn } from '~/lib/utils';

interface ErrorBoundaryCardProps {
  onReset?: () => void;
  className?: string;
}

export function ErrorBoundaryCard({ onReset, className }: ErrorBoundaryCardProps) {
  return (
    <div
      data-testid="error-boundary-card"
      className={cn(
        'mx-auto flex max-w-md flex-col items-center gap-4 rounded-card border border-border-1 bg-surface-2 p-10 text-center',
        className,
      )}
    >
      <AlertTriangle size={32} className="text-danger" />
      <h2 className="text-[20px] font-bold tracking-tight text-fg-1">Something went wrong</h2>
      <p className="text-[13px] leading-relaxed text-fg-3">
        We&apos;ve logged the error. Please try again, or contact support if the issue persists.
      </p>
      <button
        type="button"
        onClick={onReset ?? (() => window.location.reload())}
        data-testid="error-boundary-reload"
        className="mt-2 inline-flex items-center gap-2 rounded-md bg-gradient-to-r from-solana-purple to-solana-magenta px-4 py-2 text-[13px] font-semibold text-white shadow-[var(--glow-solana-soft)] transition-opacity hover:opacity-90"
      >
        Reload
      </button>
    </div>
  );
}
