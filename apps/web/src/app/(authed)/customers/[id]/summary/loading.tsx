import { Skeleton } from '~/components/ui/skeleton';

export default function CustomerSummaryLoading() {
  return (
    <div className="page">
      {/* Profile card skeleton */}
      <div className="relative overflow-hidden rounded-card border border-border-1 bg-surface-2 p-6">
        <div className="flex items-center gap-4">
          <Skeleton className="size-10 rounded-full" />
          <div>
            <Skeleton className="mb-1 h-5 w-36" />
            <Skeleton className="h-3 w-48" />
          </div>
        </div>
      </div>

      {/* Stats grid skeleton */}
      <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="rounded-card border border-border-1 bg-surface-2 px-5 py-[18px]">
            <Skeleton className="h-3 w-16" />
            <Skeleton className="mt-[14px] h-8 w-28" />
          </div>
        ))}
      </div>

      {/* Compliance card skeleton */}
      <div className="mt-5 rounded-card border border-border-1 bg-surface-2 p-5">
        <Skeleton className="mb-4 h-3 w-20" />
        <div className="grid grid-cols-2 gap-x-8 gap-y-4 sm:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i}>
              <Skeleton className="mb-1 h-3 w-16" />
              <Skeleton className="h-4 w-24" />
            </div>
          ))}
        </div>
      </div>

      {/* Recent transactions table skeleton */}
      <div className="mt-5 overflow-hidden rounded-card border border-border-1 bg-surface-2">
        <div className="px-5 pt-4 pb-2">
          <Skeleton className="h-3 w-32" />
        </div>
        <div className="border-t border-border-1">
          {Array.from({ length: 5 }).map((_, i) => (
            <div
              key={i}
              className="flex items-center gap-4 border-b border-[rgba(255,255,255,0.04)] px-5 py-[11px]"
            >
              <Skeleton className="h-3 w-20" />
              <Skeleton className="h-3 w-16" />
              <Skeleton className="h-3 w-14" />
              <Skeleton className="h-3 w-20" />
              <Skeleton className="h-5 w-20 rounded-full" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
