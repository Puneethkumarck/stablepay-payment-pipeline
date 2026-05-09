import { describe, expect, it } from 'vitest';
import { newIdempotencyKey } from './idempotency';

const UUID_V4_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

describe('newIdempotencyKey', () => {
  it('returns a valid UUID v4 string', () => {
    // act
    const key = newIdempotencyKey();

    // assert
    expect(key).toMatch(UUID_V4_REGEX);
  });

  it('returns unique values on successive calls', () => {
    // act
    const key1 = newIdempotencyKey();
    const key2 = newIdempotencyKey();

    // assert
    expect(key1).not.toBe(key2);
  });
});
