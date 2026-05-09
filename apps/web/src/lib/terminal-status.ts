const TERMINAL = new Set([
  'COMPLETED',
  'FAILED',
  'CANCELLED',
  'EXPIRED',
  'REFUND_COMPLETED',
  'CONFISCATED',
  'REPLACED',
  'SCREENING_REJECTED',
  'SCREENING_SEIZED',
  'SUSPENDED',
]);

export function isTerminal(status?: string): boolean {
  return status != null && TERMINAL.has(status);
}
