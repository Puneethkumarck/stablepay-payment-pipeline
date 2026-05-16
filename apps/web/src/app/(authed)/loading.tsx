import { Skeleton } from '~/components/ui/skeleton';

export default function AuthedLoading() {
  return (
    <div className="page" data-testid="authed-loading">
      <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="rounded-card border border-border-1 bg-surface-2 px-5 py-[18px]">
            <Skeleton className="mb-[14px] h-[11px] w-[110px]" />
            <Skeleton className="mb-[6px] h-[28px] w-[80px]" />
            <Skeleton className="h-[11px] w-[100px]" />
          </div>
        ))}
      </div>

      <div className="rounded-card border border-border-1 bg-surface-2">
        <div className="border-b border-border-1 px-[14px] py-[9px]">
          <Skeleton className="h-[11px] w-[200px]" />
        </div>
        {Array.from({ length: 5 }).map((_, i) => (
          <div
            key={i}
            className="flex items-center gap-4 border-b border-border-1 px-[14px] py-[11px] last:border-b-0"
          >
            <Skeleton className="h-[11px] w-[100px]" />
            <Skeleton className="h-[11px] w-[60px]" />
            <Skeleton className="h-[11px] w-[70px]" />
            <Skeleton className="h-[11px] w-[50px]" />
            <Skeleton className="h-[16px] w-[90px] rounded-full" />
            <Skeleton className="ml-auto h-[11px] w-[50px]" />
          </div>
        ))}
      </div>
    </div>
  );
}
