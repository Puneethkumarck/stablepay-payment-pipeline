import { describe, expect, it } from 'vitest';
import { formatAbsoluteTooltip, formatDuration, formatRelativeTime } from './time';

describe('formatRelativeTime', () => {
  const now = new Date('2026-05-09T12:00:00Z');

  it('returns "just now" for less than 60 seconds ago', () => {
    // arrange
    const iso = '2026-05-09T11:59:30Z';

    // act
    const result = formatRelativeTime(iso, now);

    // assert
    expect(result).toBe('just now');
  });

  it('returns minutes ago for less than 60 minutes', () => {
    // arrange
    const iso = '2026-05-09T11:45:00Z';

    // act
    const result = formatRelativeTime(iso, now);

    // assert
    expect(result).toBe('15 minutes ago');
  });

  it('returns hours ago for less than 24 hours', () => {
    // arrange
    const iso = '2026-05-09T09:00:00Z';

    // act
    const result = formatRelativeTime(iso, now);

    // assert
    expect(result).toBe('3 hours ago');
  });

  it('returns days ago for less than 7 days', () => {
    // arrange
    const iso = '2026-05-07T12:00:00Z';

    // act
    const result = formatRelativeTime(iso, now);

    // assert
    expect(result).toBe('2 days ago');
  });

  it('returns absolute date for 7 days or more', () => {
    // arrange
    const iso = '2026-05-01T09:11:00Z';

    // act
    const result = formatRelativeTime(iso, now);

    // assert
    expect(result).toBe('May 1, 2026');
  });

  it('returns "1 minute ago" at the 60 second boundary', () => {
    // arrange
    const iso = '2026-05-09T11:59:00Z';

    // act
    const result = formatRelativeTime(iso, now);

    // assert
    expect(result).toBe('1 minute ago');
  });
});

describe('formatDuration', () => {
  it('returns seconds for less than 60s', () => {
    // act
    const result = formatDuration(45_000);

    // assert
    expect(result).toBe('45s');
  });

  it('returns minutes for less than 60m', () => {
    // act
    const result = formatDuration(5 * 60_000);

    // assert
    expect(result).toBe('5m');
  });

  it('returns hours for 60m or more', () => {
    // act
    const result = formatDuration(90 * 60_000);

    // assert
    expect(result).toBe('1h');
  });

  it('returns 0s for zero milliseconds', () => {
    // act
    const result = formatDuration(0);

    // assert
    expect(result).toBe('0s');
  });
});

describe('formatAbsoluteTooltip', () => {
  it('returns UTC time with local time in parentheses', () => {
    // arrange
    const iso = '2026-05-01T09:11:00Z';

    // act
    const result = formatAbsoluteTooltip(iso);

    // assert
    expect(result).toContain('2026-05-01 09:11:00 UTC');
    expect(result).toMatch(/\(.+\)$/);
  });
});
