'use client';

import { Sheet, SheetContent, SheetHeader, SheetTitle } from '~/components/ui/sheet';
import { cn } from '~/lib/utils';

interface DrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  width?: number;
  children: React.ReactNode;
  className?: string;
}

export function Drawer({
  open,
  onOpenChange,
  title,
  width = 480,
  children,
  className,
}: DrawerProps) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        data-testid="drawer"
        side="right"
        className={cn('flex flex-col', className)}
        style={{ width, maxWidth: '100vw' }}
      >
        <SheetHeader>
          <SheetTitle>{title}</SheetTitle>
        </SheetHeader>
        <div className="flex-1 overflow-y-auto p-[22px]">{children}</div>
      </SheetContent>
    </Sheet>
  );
}
