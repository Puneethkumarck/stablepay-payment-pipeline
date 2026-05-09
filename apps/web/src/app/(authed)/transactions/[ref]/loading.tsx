import { Skeleton } from '~/components/ui/skeleton';

export default function TransactionDetailLoading() {
  return (
    <div className="page">
      {/* Page header skeleton */}
      <div className="mb-6 flex items-center gap-[10px]">
        <Skeleton className="size-[30px] rounded-md" />
        <div>
          <Skeleton className="mb-[3px] h-3 w-24" />
          <Skeleton className="h-6 w-48" />
        </div>
      </div>

      {/* Hero row skeleton */}
      <div className="mb-4 grid grid-cols-[300px_1fr] gap-4">
        {/* Amount hero skeleton */}
        <div className="rounded-card border border-border-1 bg-surface-2 p-5">
          <Skeleton className="mb-3 h-3 w-20" />
          <Skeleton className="mb-1 h-9 w-36" />
          <Skeleton className="mb-4 h-3 w-24" />
          <Skeleton className="mt-[18px] h-6 w-28 rounded-full" />
        </div>
        {/* Metadata grid skeleton */}
        <div className="rounded-card border border-border-1 bg-surface-2 p-0">
          <div className="grid grid-cols-2">
            {Array.from({ length: 8 }).map((_, i) => (
              <div
                key={i}
                className="border-b border-r border-[rgba(255,255,255,0.05)] p-[10px_16px] last:border-r-0 [&:nth-child(even)]:border-r-0 [&:nth-last-child(-n+2)]:border-b-0"
              >
                <Skeleton className="mb-1 h-3 w-20" />
                <Skeleton className="h-4 w-32" />
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom row skeleton */}
      <div className="grid grid-cols-2 gap-4">
        <div className="rounded-card border border-border-1 bg-surface-2 p-5">
          <Skeleton className="mb-[18px] h-4 w-40" />
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="mb-4 flex gap-3">
              <Skeleton className="size-[18px] shrink-0 rounded-full" />
              <Skeleton className="h-4 w-32" />
            </div>
          ))}
        </div>
        <div className="rounded-card border border-border-1 bg-surface-2 p-5">
          <Skeleton className="mb-[14px] h-4 w-36" />
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="mb-3 flex justify-between">
              <Skeleton className="h-3 w-24" />
              <Skeleton className="h-3 w-40" />
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
