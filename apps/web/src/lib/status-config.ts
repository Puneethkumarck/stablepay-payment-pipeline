export interface StatusStyle {
  label: string;
  color: StatusColor;
  pulse?: boolean;
}

export type StatusColor = 'info' | 'purple' | 'success' | 'warning' | 'danger' | 'neutral';

export interface ColorTokens {
  bg: string;
  border: string;
  text: string;
  dot: string;
}

export const statusConfig: Record<string, StatusStyle> = {
  // Shared / terminal
  INITIATED: { label: 'Initiated', color: 'info' },
  COMPLETED: { label: 'Completed', color: 'success' },
  FAILED: { label: 'Failed', color: 'danger' },
  CANCELLED: { label: 'Cancelled', color: 'neutral' },
  CONFIRMED: { label: 'Confirmed', color: 'success' },
  SUSPENDED: { label: 'Suspended', color: 'danger' },
  CONFISCATED: { label: 'Confiscated', color: 'danger' },
  EXPIRED: { label: 'Expired', color: 'neutral' },
  STUCK: { label: 'Stuck', color: 'danger', pulse: true },

  // Approval (fiat payout)
  PENDING_APPROVAL: { label: 'Pending approval', color: 'warning' },
  FIRST_APPROVAL: { label: 'First approval', color: 'warning' },
  PENDING_SECOND_APPROVAL: { label: 'Pending 2nd approval', color: 'warning' },
  APPROVED: { label: 'Approved', color: 'info' },

  // Screening (shared across flows)
  PENDING_SCREENING: { label: 'Pending screening', color: 'info' },
  SCREENING_IN_PROGRESS: { label: 'Screening in progress', color: 'info', pulse: true },
  SCREENING_HOLD: { label: 'Screening hold', color: 'warning', pulse: true },
  SCREENING_RFI: { label: 'Screening RFI', color: 'warning', pulse: true },
  SCREENING_RELEASED: { label: 'Screening released', color: 'success' },
  SCREENING_CLEARED: { label: 'Screening cleared', color: 'success' },
  SCREENING_REJECTED: { label: 'Screening rejected', color: 'danger' },
  SCREENING_SEIZED: { label: 'Screening seized', color: 'danger' },
  SCREENING_FLAGGED: { label: 'Screening flagged', color: 'danger' },
  SCREENING_HELD: { label: 'Screening held', color: 'warning', pulse: true },
  MANUAL_REVIEW: { label: 'Manual review', color: 'warning', pulse: true },

  // Fiat payout — partner routing
  PENDING_PARTNER_ROUTING: { label: 'Pending routing', color: 'info' },
  PARTNER_ASSIGNED: { label: 'Partner assigned', color: 'info' },
  PENDING_EXECUTION: { label: 'Pending execution', color: 'info' },
  EXECUTING: { label: 'Executing', color: 'info', pulse: true },
  SENT_TO_PARTNER: { label: 'Sent to partner', color: 'info', pulse: true },
  PARTNER_ACKNOWLEDGED: { label: 'Partner acknowledged', color: 'info' },
  PENDING_CONFIRMATION: { label: 'Pending confirmation', color: 'info', pulse: true },

  // Fiat payout — refund / exception
  RETURNED: { label: 'Returned', color: 'warning' },
  REFUND_INITIATED: { label: 'Refund initiated', color: 'warning', pulse: true },
  REFUND_PENDING: { label: 'Refund pending', color: 'warning', pulse: true },
  REFUND_COMPLETED: { label: 'Refund completed', color: 'success' },
  LEDGER_SUSPENSE: { label: 'Ledger suspense', color: 'warning', pulse: true },

  // Crypto payout — signing / chain
  PENDING_SIGNING: { label: 'Pending signing', color: 'info' },
  SIGNING_IN_PROGRESS: { label: 'Signing in progress', color: 'info', pulse: true },
  SIGNED: { label: 'Signed', color: 'info' },
  BROADCASTING: { label: 'Broadcasting', color: 'info', pulse: true },
  BROADCAST: { label: 'Broadcast', color: 'info' },
  CONFIRMING: { label: 'Confirming', color: 'info', pulse: true },
  RBF_INITIATED: { label: 'RBF initiated', color: 'warning' },
  RBF_BROADCAST: { label: 'RBF broadcast', color: 'warning', pulse: true },
  REPLACED: { label: 'Replaced', color: 'info' },

  // Fiat / crypto payin
  DETECTED: { label: 'Detected', color: 'info' },
  MATCHED: { label: 'Matched', color: 'info' },
  PENDING_ALLOCATION: { label: 'Pending allocation', color: 'info' },
  ALLOCATED: { label: 'Allocated', color: 'info' },

  // Customer-facing statuses (customer_status field)
  PENDING: { label: 'Pending', color: 'info' },
  PROCESSING: { label: 'Processing', color: 'info', pulse: true },
  REFUNDED: { label: 'Refunded', color: 'warning' },

  // DLQ error classes
  SCHEMA_INVALID: { label: 'Schema invalid', color: 'danger' },
  LATE_EVENT: { label: 'Late event', color: 'warning' },
  PROCESSING_FAILED: { label: 'Processing failed', color: 'danger' },
  SINK_FAILED: { label: 'Sink failed', color: 'danger' },
};

export const colorMap: Record<StatusColor, ColorTokens> = {
  info: {
    bg: 'rgba(56,189,248,0.10)',
    border: 'rgba(56,189,248,0.22)',
    text: '#7DD3FC',
    dot: '#38BDF8',
  },
  purple: {
    bg: 'rgba(153,69,255,0.12)',
    border: 'rgba(153,69,255,0.30)',
    text: '#C4B5FD',
    dot: '#9945FF',
  },
  success: {
    bg: 'rgba(34,197,94,0.10)',
    border: 'rgba(34,197,94,0.22)',
    text: '#86EFAC',
    dot: '#22C55E',
  },
  warning: {
    bg: 'rgba(245,158,11,0.10)',
    border: 'rgba(245,158,11,0.24)',
    text: '#FCD34D',
    dot: '#F59E0B',
  },
  danger: {
    bg: 'rgba(239,68,68,0.10)',
    border: 'rgba(239,68,68,0.24)',
    text: '#FCA5A5',
    dot: '#EF4444',
  },
  neutral: {
    bg: 'rgba(255,255,255,0.05)',
    border: 'rgba(255,255,255,0.10)',
    text: 'rgba(255,255,255,0.40)',
    dot: 'rgba(255,255,255,0.24)',
  },
};

const FALLBACK_STYLE: StatusStyle = { label: '', color: 'neutral' };

export function getStatusStyle(status: string): StatusStyle {
  const style = statusConfig[status];
  if (style) return style;

  if (process.env.NODE_ENV === 'development') {
    console.warn(`[status-config] Unknown status: "${status}"`);
  }

  return { ...FALLBACK_STYLE, label: status };
}

export function getColorTokens(color: StatusColor): ColorTokens {
  return colorMap[color];
}
