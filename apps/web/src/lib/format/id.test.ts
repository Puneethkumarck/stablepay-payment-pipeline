import { describe, expect, it } from 'vitest';
import { truncateId } from './id';

describe('truncateId', () => {
  it('truncates a UUID to 8 chars + ellipsis', () => {
    // arrange
    const id = 'a1b2c3d4-e5f6-7890-abcd-ef1234567890';

    // act
    const result = truncateId(id);

    // assert
    expect(result).toBe('a1b2c3d4…');
  });

  it('returns the original string if shorter than prefix', () => {
    // arrange
    const id = 'short';

    // act
    const result = truncateId(id);

    // assert
    expect(result).toBe('short');
  });

  it('returns the original string if exactly prefix length', () => {
    // arrange
    const id = '12345678';

    // act
    const result = truncateId(id);

    // assert
    expect(result).toBe('12345678');
  });

  it('truncates to a custom prefix length', () => {
    // arrange
    const id = 'abcdefghijklmnop';

    // act
    const result = truncateId(id, 4);

    // assert
    expect(result).toBe('abcd…');
  });
});
