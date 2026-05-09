import Link from 'next/link';
import { cn } from '~/lib/utils';

interface SessionExpiredBannerProps {
  visible: boolean;
  className?: string;
}

export function SessionExpiredBanner({ visible, className }: SessionExpiredBannerProps) {
  if (!visible) return null;

  return (
    <div
      data-testid="session-expired-banner"
      role="alert"
      className={cn(
        'fixed inset-x-0 top-0 z-[60] flex items-center justify-center gap-3 border-b border-[rgba(245,158,11,0.24)] bg-[rgba(245,158,11,0.10)] px-4 py-3 text-[13px] font-medium text-[#FCD34D] backdrop-blur-sm',
        className,
      )}
    >
      <span>Your session has expired.</span>
      <Link
        href={{ pathname: '/login', query: { reason: 'session-expired' } }}
        data-testid="session-expired-cta"
        className="rounded-md border border-[rgba(245,158,11,0.28)] bg-[rgba(245,158,11,0.12)] px-3 py-1 text-[12px] font-semibold text-[#FCD34D] transition-colors duration-[120ms] hover:bg-[rgba(245,158,11,0.20)]"
      >
        Sign in again
      </Link>
    </div>
  );
}
