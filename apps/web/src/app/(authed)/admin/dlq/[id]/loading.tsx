import { Skeleton } from '~/components/ui/skeleton';

export default function DlqDetailLoading() {
  return (
    <div className="page" data-testid="dlq-detail-loading">
      {/* Back link */}
      <Skeleton className="h-[28px] w-[110px] rounded-md" />

      {/* Header */}
      <div className="mt-4 mb-5">
        <Skeleton className="mb-1 h-[11px] w-[70px]" />
        <Skeleton className="h-[28px] w-[160px]" />
      </div>

      {/* KV grid */}
      <div className="rounded-card border border-border-1 bg-surface-2 px-5 py-2">
        {Array.from({ length: 7 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center justify-between border-b border-[rgba(255,255,255,0.05)] py-[9px] last:border-b-0"
          >
            <Skeleton className="h-[11px] w-[80px]" />
            <Skeleton className="h-[11px] w-[140px]" />
          </div>
        ))}
      </div>

      {/* Error message */}
      <div className="mt-5">
        <Skeleton className="mb-3 h-[11px] w-[100px]" />
        <Skeleton className="h-[120px] w-full rounded-md" />
      </div>

      {/* Event payload */}
      <div className="mt-5">
        <Skeleton className="mb-3 h-[11px] w-[100px]" />
        <Skeleton className="h-[80px] w-full rounded-md" />
      </div>

      {/* Replay section */}
      <div className="mt-5 rounded-card border border-border-1 bg-surface-2 px-5 py-4">
        <div className="flex items-center justify-between">
          <div>
            <Skeleton className="h-[13px] w-[50px]" />
            <Skeleton className="mt-1 h-[11px] w-[240px]" />
          </div>
          <Skeleton className="h-[24px] w-[60px] rounded-md" />
        </div>
      </div>
    </div>
  );
}
