const TERMINAL = new Set([
  'COMPLETED',
  'FAILED',
  'CANCELLED',
  'EXPIRED',
  'REFUND_COMPLETED',
  'CONFISCATED',
  'REPLACED',
]);

export function isTerminal(status?: string): boolean {
  return status != null && TERMINAL.has(status);
}
