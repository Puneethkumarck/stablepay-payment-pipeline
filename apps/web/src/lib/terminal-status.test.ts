import { describe, expect, it } from 'vitest';
import { isTerminal } from './terminal-status';

describe('isTerminal', () => {
  it.each([
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
  ])('returns true for terminal status %s', (status) => {
    // arrange / act / assert
    expect(isTerminal(status)).toBe(true);
  });

  it.each([
    'INITIATED',
    'PENDING_SCREENING',
    'SCREENING_IN_PROGRESS',
    'PENDING_APPROVAL',
    'EXECUTING',
    'BROADCASTING',
    'CONFIRMING',
  ])('returns false for non-terminal status %s', (status) => {
    // arrange / act / assert
    expect(isTerminal(status)).toBe(false);
  });

  it('returns false for undefined', () => {
    // arrange / act / assert
    expect(isTerminal(undefined)).toBe(false);
  });

  it('returns false for empty string', () => {
    // arrange / act / assert
    expect(isTerminal('')).toBe(false);
  });

  it('covers exactly 10 terminal states', () => {
    // arrange
    const allTerminal = [
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
    ];

    // act / assert
    expect(allTerminal.filter((s) => isTerminal(s))).toHaveLength(10);
  });
});
