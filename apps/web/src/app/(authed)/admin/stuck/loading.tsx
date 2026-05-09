import { Skeleton } from '~/components/ui/skeleton';

export default function StuckLoading() {
  return (
    <div className="page" data-testid="stuck-loading">
      <Skeleton className="mb-1 h-[11px] w-[50px]" />
      <Skeleton className="mb-5 h-[28px] w-[200px]" />

      {/* Banner skeleton */}
      <Skeleton className="mb-5 h-[48px] w-full rounded-card" />

      {/* Expandable cards skeleton */}
      <div className="mb-6 space-y-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="rounded-card border border-border-1 bg-surface-2 px-4 py-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <Skeleton className="h-[14px] w-[120px]" />
                <Skeleton className="h-[20px] w-[60px] rounded-full" />
                <Skeleton className="h-[14px] w-[80px]" />
              </div>
              <Skeleton className="size-4" />
            </div>
          </div>
        ))}
      </div>

      {/* Agg table skeleton */}
      <Skeleton className="mb-2 h-[11px] w-[160px]" />
      <div className="rounded-card border border-border-1 bg-surface-2">
        <div className="border-b border-border-1 px-[14px] py-[9px]">
          <div className="flex gap-4">
            <Skeleton className="h-[11px] w-[100px]" />
            <Skeleton className="h-[11px] w-[60px]" />
            <Skeleton className="h-[11px] w-[80px]" />
            <Skeleton className="h-[11px] w-[90px]" />
            <Skeleton className="h-[11px] w-[70px]" />
          </div>
        </div>
        {Array.from({ length: 4 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-4 border-b border-border-1 px-[14px] py-[11px] last:border-b-0"
          >
            <Skeleton className="h-[11px] w-[100px]" />
            <Skeleton className="h-[16px] w-[70px] rounded-full" />
            <Skeleton className="h-[11px] w-[80px]" />
            <Skeleton className="h-[11px] w-[90px]" />
            <Skeleton className="h-[11px] w-[80px]" />
          </div>
        ))}
      </div>
    </div>
  );
}
