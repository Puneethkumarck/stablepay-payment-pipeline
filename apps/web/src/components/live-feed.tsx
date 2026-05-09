'use client';

import { X } from 'lucide-react';
import { getColorTokens, getStatusStyle } from '~/lib/status-config';
import {
  type ConnectionStatus,
  type TransactionEvent,
  useTransactionFeed,
} from '~/lib/stores/sse-client-store';
import { cn } from '~/lib/utils';

interface LiveFeedEvent {
  id: string;
  type: string;
  status: string;
  amount: string;
  ts: string;
  customer_id?: string;
}

function parseEvent(event: TransactionEvent): LiveFeedEvent | null {
  const data = event.data as Record<string, unknown> | null;
  if (!data) return null;
  return {
    id: String(data.event_id ?? event.id ?? ''),
    type: String(data.flow_type ?? ''),
    status: String(data.status ?? ''),
    amount: String(data.amount_micros ?? ''),
    ts: String(data.event_time ?? ''),
    customer_id: data.customer_id != null ? String(data.customer_id) : undefined,
  };
}

const statusLabel: Record<ConnectionStatus, string> = {
  connecting: 'Reconnecting…',
  connected: 'SSE feed',
  disconnected: 'Disconnected',
  'fallback-polling': 'Polling',
};

interface LiveFeedProps {
  visible: boolean;
  onHide: () => void;
  isAdmin?: boolean;
  userEmail?: string;
  className?: string;
}

export function LiveFeed({
  visible,
  onHide,
  isAdmin = false,
  userEmail,
  className,
}: LiveFeedProps) {
  const events = useTransactionFeed((s) => s.events);
  const status = useTransactionFeed((s) => s.status);

  if (!visible) return null;

  const displayEvents = events.slice(-10).reverse();
  const isConnected = status === 'connected';

  return (
    <aside
      data-testid="live-feed"
      aria-live="polite"
      aria-label="Live transaction feed"
      className={cn(
        'sticky top-0 flex h-screen w-[232px] shrink-0 flex-col border-l border-border-1 bg-sidebar',
        className,
      )}
    >
      <div className="flex shrink-0 items-center justify-between border-b border-[rgba(255,255,255,0.05)] px-[14px] py-[14px] pb-[10px]">
        <div className="flex items-center gap-[6px]">
          <span
            className={cn(
              'inline-block size-[6px] rounded-full shadow-[0_0_6px_rgba(34,197,94,0.4)]',
              isConnected ? 'bg-success' : 'bg-warning',
            )}
          />
          <span className="sp-eyebrow text-[10px]">{statusLabel[status]}</span>
        </div>
        <button
          type="button"
          onClick={onHide}
          aria-label="Close live feed"
          data-testid="live-feed-close"
          className="grid cursor-pointer place-items-center p-[2px] text-fg-3 hover:text-fg-2"
        >
          <X size={13} />
        </button>
      </div>

      {/* Eyebrow: admin vs customer */}
      <div className="sp-eyebrow border-b border-[rgba(255,255,255,0.03)] px-[13px] py-[6px] text-[10px]">
        {isAdmin ? 'All customers' : (userEmail ?? 'My events')}
      </div>

      <div className="flex-1 overflow-y-auto py-1">
        {displayEvents.length === 0 && (
          <div className="flex flex-col items-center gap-2 px-4 py-8 text-center">
            <span className="inline-block size-[6px] rounded-full bg-fg-3 opacity-40" />
            <span className="text-[11px] text-fg-3">Listening for events…</span>
          </div>
        )}
        {displayEvents.map((event, i) => {
          const parsed = parseEvent(event);
          if (!parsed) return null;

          const style = getStatusStyle(parsed.status);
          const tokens = getColorTokens(style.color);

          return (
            <div
              key={`${parsed.id}-${i}`}
              className="border-b border-[rgba(255,255,255,0.03)] px-[13px] py-2"
              style={{ opacity: Math.max(0.3, 1 - i * 0.08) }}
            >
              <div className="mb-1 flex justify-between">
                <span className="font-mono text-[10px] font-semibold text-fg-3">
                  {parsed.id.slice(0, 8)}
                </span>
                <span className="font-mono text-[10px] text-fg-3">{parsed.ts ? 'now' : ''}</span>
              </div>
              <div className="mb-[5px] flex items-center justify-between">
                <span className="text-[11px] font-medium text-fg-2">
                  {parsed.type.replace(/_/g, ' ')}
                </span>
                <span className="font-mono text-[11px] font-semibold">{parsed.amount}</span>
              </div>
              <div className="flex items-center gap-1">
                <span
                  className="inline-flex items-center gap-1 rounded-full border px-[7px] py-[1px] text-[9.5px] font-medium tracking-[0.01em]"
                  style={{
                    background: tokens.bg,
                    borderColor: tokens.border,
                    color: tokens.text,
                  }}
                >
                  <span
                    className="inline-block size-1 shrink-0 rounded-full"
                    style={{ background: tokens.dot }}
                  />
                  {parsed.status}
                </span>
                {isAdmin && parsed.customer_id && (
                  <span className="rounded-full bg-[var(--accent-soft)] px-[6px] py-[1px] font-mono text-[9px] text-[#C4B5FD]">
                    {parsed.customer_id.slice(0, 6)}
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </aside>
  );
}
