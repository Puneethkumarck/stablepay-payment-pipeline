import { Search } from 'lucide-react';
import Link from 'next/link';
import { cn } from '~/lib/utils';

interface NotFoundCardProps {
  title?: string;
  body?: string;
  primaryCta?: { label: string; href: string };
  className?: string;
}

export function NotFoundCard({
  title = "We can't find that transaction",
  body = 'It may have been removed, or you may not have access. Check the reference and try again.',
  primaryCta = { label: 'Back to dashboard', href: '/' },
  className,
}: NotFoundCardProps) {
  return (
    <div
      data-testid="not-found-card"
      className={cn(
        'mx-auto flex max-w-md flex-col items-center gap-4 rounded-card border border-border-1 bg-surface-2 p-10 text-center',
        className,
      )}
    >
      <Search size={32} className="text-fg-4" />
      <h2 className="text-[20px] font-bold tracking-tight text-fg-1">{title}</h2>
      <p className="text-[13px] leading-relaxed text-fg-3">{body}</p>
      <Link
        href={primaryCta.href}
        data-testid="not-found-cta"
        className="mt-2 inline-flex items-center gap-2 rounded-md bg-gradient-to-r from-solana-purple to-solana-magenta px-4 py-2 text-[13px] font-semibold text-white shadow-[var(--glow-solana-soft)] transition-opacity hover:opacity-90"
      >
        {primaryCta.label}
      </Link>
    </div>
  );
}
