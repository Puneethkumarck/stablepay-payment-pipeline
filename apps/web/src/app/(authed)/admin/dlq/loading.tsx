import { Skeleton } from '~/components/ui/skeleton';

export default function DlqListLoading() {
  return (
    <div className="page" data-testid="dlq-list-loading">
      <Skeleton className="mb-1 h-[11px] w-[50px]" />
      <Skeleton className="mb-5 h-[28px] w-[180px]" />

      {/* Breakdown cards */}
      <div className="mb-5 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="rounded-card border border-border-1 bg-surface-2 px-4 py-3">
            <Skeleton className="mb-2 h-[11px] w-[80px]" />
            <Skeleton className="h-[22px] w-[40px]" />
          </div>
        ))}
      </div>

      {/* Search bar */}
      <Skeleton className="mb-4 h-[34px] w-[340px] rounded-[9px]" />

      {/* Table */}
      <div className="rounded-card border border-border-1 bg-surface-2">
        <div className="border-b border-border-1 px-[14px] py-[9px]">
          <div className="flex gap-4">
            <Skeleton className="h-[11px] w-[80px]" />
            <Skeleton className="h-[11px] w-[70px]" />
            <Skeleton className="h-[11px] w-[90px]" />
            <Skeleton className="h-[11px] w-[50px]" />
            <Skeleton className="h-[11px] w-[60px]" />
            <Skeleton className="h-[11px] w-[60px]" />
          </div>
        </div>
        {Array.from({ length: 6 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-4 border-b border-border-1 px-[14px] py-[11px] last:border-b-0"
          >
            <Skeleton className="h-[11px] w-[100px]" />
            <Skeleton className="h-[16px] w-[90px] rounded-full" />
            <Skeleton className="h-[11px] w-[120px]" />
            <Skeleton className="h-[11px] w-[30px]" />
            <Skeleton className="h-[11px] w-[80px]" />
            <Skeleton className="h-[24px] w-[60px] rounded-md" />
          </div>
        ))}
      </div>
    </div>
  );
}
