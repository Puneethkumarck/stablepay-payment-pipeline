import { Skeleton } from '~/components/ui/skeleton';

export default function FlowDetailLoading() {
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

      {/* LegStepper skeleton */}
      <div className="mb-4 flex items-stretch gap-2">
        {Array.from({ length: 2 }).map((_, i) => (
          <div key={i} className="flex items-stretch">
            <div className="flex min-w-[160px] flex-1 flex-col gap-2 rounded-card border border-border-1 bg-surface-2 p-4">
              <Skeleton className="h-4 w-24" />
              <Skeleton className="h-6 w-28 rounded-full" />
            </div>
            {i < 1 && (
              <div className="flex items-center px-1">
                <Skeleton className="size-4" />
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Metadata card skeleton */}
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
  );
}
