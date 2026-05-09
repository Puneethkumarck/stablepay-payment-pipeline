'use client';

import { useState } from 'react';
import { toast } from 'sonner';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '~/components/ui/alert-dialog';
import { Button } from '~/components/ui/button';
import { formatMoney } from '~/lib/format/money';
import type { StuckPaymentDto } from '~/types/api';

interface RefundConfirmationDialogProps {
  stuckPayment: StuckPaymentDto;
}

export function RefundConfirmationDialog({ stuckPayment }: RefundConfirmationDialogProps) {
  const [open, setOpen] = useState(false);
  const formattedAmount = formatMoney(stuckPayment.amount.amount, stuckPayment.amount.currency);

  const handleConfirmRefund = () => {
    setOpen(false);
    toast.info('Refund initiation deferred to Phase 6', {
      description:
        'The Stuck refund endpoint is not in REQUIREMENTS v1; backend wiring will be added in Phase 6.',
      duration: 8000,
    });
  };

  return (
    <AlertDialog open={open} onOpenChange={setOpen}>
      <AlertDialogTrigger
        render={
          <Button data-testid="trigger-refund-button" variant="destructive" size="xs">
            Trigger refund
          </Button>
        }
      />
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Trigger refund?</AlertDialogTitle>
          <AlertDialogDescription>
            This creates an irreversible refund initiation event for {formattedAmount} to customer{' '}
            {stuckPayment.customer_id}. The refund will be processed by the partner provider.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Cancel</AlertDialogCancel>
          <AlertDialogAction variant="destructive" onClick={handleConfirmRefund}>
            Trigger refund
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
