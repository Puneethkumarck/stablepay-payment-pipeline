'use client';

import { useState } from 'react';
import { toast } from 'sonner';
import { Button } from '~/components/ui/button';
import { clientFetchWithResponse } from '~/lib/api-client/client-fetch';
import { newIdempotencyKey } from '~/lib/idempotency';

const NON_REPLAYABLE = ['SCHEMA_INVALID', 'LATE_EVENT'] as const;

interface ReplayButtonProps {
  dlqId: string;
  errorClass: string;
  retryCount: number;
}

export function ReplayButton({ dlqId, errorClass, retryCount }: ReplayButtonProps) {
  const [state, setState] = useState<'idle' | 'loading' | 'done'>('idle');

  if (retryCount >= 2) {
    return (
      <span data-testid="replay-exhausted" className="text-xs text-fg-3">
        Retry budget exhausted
      </span>
    );
  }

  if (NON_REPLAYABLE.includes(errorClass as (typeof NON_REPLAYABLE)[number])) {
    return (
      <span data-testid="replay-no-retry" className="text-xs text-fg-3">
        No retry
      </span>
    );
  }

  const handleReplay = async () => {
    setState('loading');
    const key = newIdempotencyKey();
    try {
      const { response } = await clientFetchWithResponse(
        `/api/v1/admin/dlq/${encodeURIComponent(dlqId)}/replay`,
        {
          method: 'POST',
          headers: { 'X-Idempotency-Key': key },
        },
      );
      const cached = response.headers.get('Idempotency-Replayed') === 'true';
      setState('done');
      toast.success(cached ? 'Replay re-queued' : 'Replay queued', {
        description:
          'Command published. Replay execution is wired in Phase 6.',
        duration: 8000,
      });
    } catch (err) {
      setState('idle');
      const message = err instanceof Error ? err.message : 'See logs for details.';
      toast.error('Replay failed', { description: message });
    }
  };

  return (
    <Button
      data-testid="replay-button"
      variant="outline"
      size="xs"
      className="border-amber-500/30 text-amber-400 hover:bg-amber-500/10 hover:text-amber-300"
      onClick={handleReplay}
      disabled={state !== 'idle'}
    >
      {state === 'idle' ? 'Replay' : state === 'loading' ? 'Replaying…' : 'Replayed'}
    </Button>
  );
}
