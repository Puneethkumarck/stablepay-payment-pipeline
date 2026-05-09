import type { TimelineStep } from '~/components/timeline';

interface LifecycleDefinition {
  statuses: string[];
  labels: Record<string, string>;
}

const FIAT_PAYIN: LifecycleDefinition = {
  statuses: [
    'INITIATED',
    'DETECTED',
    'MATCHED',
    'PENDING_SCREENING',
    'SCREENING_IN_PROGRESS',
    'SCREENING_CLEARED',
    'PENDING_ALLOCATION',
    'ALLOCATED',
    'COMPLETED',
  ],
  labels: {
    INITIATED: 'Initiated',
    DETECTED: 'Detected',
    MATCHED: 'Matched',
    PENDING_SCREENING: 'Pending screening',
    SCREENING_IN_PROGRESS: 'Screening',
    SCREENING_CLEARED: 'Screening cleared',
    PENDING_ALLOCATION: 'Pending allocation',
    ALLOCATED: 'Allocated',
    COMPLETED: 'Completed',
  },
};

const FIAT_PAYOUT: LifecycleDefinition = {
  statuses: [
    'INITIATED',
    'PENDING_APPROVAL',
    'FIRST_APPROVAL',
    'PENDING_SECOND_APPROVAL',
    'APPROVED',
    'PENDING_SCREENING',
    'SCREENING_IN_PROGRESS',
    'SCREENING_CLEARED',
    'PENDING_PARTNER_ROUTING',
    'PARTNER_ASSIGNED',
    'SENT_TO_PARTNER',
    'PARTNER_ACKNOWLEDGED',
    'PENDING_CONFIRMATION',
    'CONFIRMED',
    'COMPLETED',
  ],
  labels: {
    INITIATED: 'Initiated',
    PENDING_APPROVAL: 'Pending approval',
    FIRST_APPROVAL: 'First approval',
    PENDING_SECOND_APPROVAL: 'Pending 2nd approval',
    APPROVED: 'Approved',
    PENDING_SCREENING: 'Pending screening',
    SCREENING_IN_PROGRESS: 'Screening',
    SCREENING_CLEARED: 'Screening cleared',
    PENDING_PARTNER_ROUTING: 'Pending routing',
    PARTNER_ASSIGNED: 'Partner assigned',
    SENT_TO_PARTNER: 'Sent to partner',
    PARTNER_ACKNOWLEDGED: 'Partner acknowledged',
    PENDING_CONFIRMATION: 'Pending confirmation',
    CONFIRMED: 'Confirmed',
    COMPLETED: 'Completed',
  },
};

const CRYPTO_PAYIN: LifecycleDefinition = {
  statuses: [
    'INITIATED',
    'DETECTED',
    'CONFIRMING',
    'CONFIRMED',
    'PENDING_SCREENING',
    'SCREENING_IN_PROGRESS',
    'SCREENING_CLEARED',
    'COMPLETED',
  ],
  labels: {
    INITIATED: 'Initiated',
    DETECTED: 'Detected',
    CONFIRMING: 'Confirming on-chain',
    CONFIRMED: 'Confirmed',
    PENDING_SCREENING: 'Pending screening',
    SCREENING_IN_PROGRESS: 'Screening',
    SCREENING_CLEARED: 'Screening cleared',
    COMPLETED: 'Completed',
  },
};

const CRYPTO_PAYOUT: LifecycleDefinition = {
  statuses: [
    'INITIATED',
    'PENDING_APPROVAL',
    'APPROVED',
    'PENDING_SCREENING',
    'SCREENING_IN_PROGRESS',
    'SCREENING_CLEARED',
    'PENDING_SIGNING',
    'SIGNING_IN_PROGRESS',
    'SIGNED',
    'BROADCASTING',
    'BROADCAST',
    'CONFIRMING',
    'CONFIRMED',
    'COMPLETED',
  ],
  labels: {
    INITIATED: 'Initiated',
    PENDING_APPROVAL: 'Pending approval',
    APPROVED: 'Approved',
    PENDING_SCREENING: 'Pending screening',
    SCREENING_IN_PROGRESS: 'Screening',
    SCREENING_CLEARED: 'Screening cleared',
    PENDING_SIGNING: 'Pending signing',
    SIGNING_IN_PROGRESS: 'Signing',
    SIGNED: 'Signed',
    BROADCASTING: 'Broadcasting',
    BROADCAST: 'Broadcast',
    CONFIRMING: 'Confirming on-chain',
    CONFIRMED: 'Confirmed',
    COMPLETED: 'Completed',
  },
};

const TERMINAL_FAILURE = new Set([
  'FAILED',
  'CANCELLED',
  'EXPIRED',
  'SCREENING_REJECTED',
  'SCREENING_SEIZED',
  'CONFISCATED',
  'SUSPENDED',
]);

function resolveLifecycle(
  type: 'FIAT' | 'CRYPTO',
  direction: 'PAYIN' | 'PAYOUT',
): LifecycleDefinition {
  if (type === 'FIAT' && direction === 'PAYIN') return FIAT_PAYIN;
  if (type === 'FIAT' && direction === 'PAYOUT') return FIAT_PAYOUT;
  if (type === 'CRYPTO' && direction === 'PAYIN') return CRYPTO_PAYIN;
  return CRYPTO_PAYOUT;
}

export function deriveTimeline(
  internalStatus: string,
  type: 'FIAT' | 'CRYPTO',
  direction: 'PAYIN' | 'PAYOUT',
): TimelineStep[] {
  const lifecycle = resolveLifecycle(type, direction);

  if (TERMINAL_FAILURE.has(internalStatus)) {
    const idx = lifecycle.statuses.indexOf(internalStatus);
    const stepsBeforeFailure =
      idx >= 0 ? lifecycle.statuses.slice(0, idx) : lifecycle.statuses;

    const steps: TimelineStep[] = stepsBeforeFailure.map((s) => ({
      label: lifecycle.labels[s] ?? s,
      state: 'done' as const,
    }));

    steps.push({ label: internalStatus.replace(/_/g, ' ').toLowerCase(), state: 'live' as const });
    return steps;
  }

  const currentIndex = lifecycle.statuses.indexOf(internalStatus);

  if (currentIndex === -1) {
    return [{ label: internalStatus.replace(/_/g, ' ').toLowerCase(), state: 'live' as const }];
  }

  return lifecycle.statuses.map((s, i) => ({
    label: lifecycle.labels[s] ?? s,
    state: i < currentIndex ? ('done' as const) : i === currentIndex ? ('live' as const) : ('pending' as const),
  }));
}
