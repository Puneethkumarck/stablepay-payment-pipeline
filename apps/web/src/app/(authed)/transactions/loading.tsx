import { Skeleton } from '~/components/ui/skeleton';

export default function TransactionsLoading() {
  return (
    <div className="page" data-testid="transactions-loading">
      <Skeleton className="mb-1 h-[11px] w-[110px]" />
      <Skeleton className="mb-6 h-[28px] w-[180px]" />

      <div className="mb-4 flex flex-wrap items-center gap-2">
        <Skeleton className="h-[34px] w-[260px] rounded-[9px]" />
        <Skeleton className="h-[34px] w-[130px] rounded-[9px]" />
      </div>
      <div className="mb-4 flex flex-wrap gap-[6px]">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-[28px] w-[100px] rounded-full" />
        ))}
      </div>

      <div className="rounded-card border border-border-1 bg-surface-2">
        <div className="border-b border-border-1 px-[14px] py-[9px]">
          <div className="flex gap-4">
            <Skeleton className="h-[11px] w-[80px]" />
            <Skeleton className="h-[11px] w-[50px]" />
            <Skeleton className="h-[11px] w-[60px]" />
            <Skeleton className="h-[11px] w-[50px]" />
            <Skeleton className="h-[11px] w-[80px]" />
            <Skeleton className="h-[11px] w-[40px]" />
          </div>
        </div>
        {Array.from({ length: 8 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-4 border-b border-border-1 px-[14px] py-[11px] last:border-b-0"
          >
            <Skeleton className="h-[11px] w-[100px]" />
            <Skeleton className="h-[11px] w-[60px]" />
            <Skeleton className="h-[11px] w-[70px]" />
            <Skeleton className="h-[16px] w-[90px] rounded-full" />
            <Skeleton className="h-[11px] w-[80px]" />
            <Skeleton className="ml-auto h-[11px] w-[50px]" />
          </div>
        ))}
      </div>
    </div>
  );
}
