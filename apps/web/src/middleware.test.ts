import { describe, expect, it, vi } from 'vitest';

// Mock next-auth's auth export to avoid importing next/server in tests
vi.mock('~/server/auth', () => ({
  auth: vi.fn((handler: unknown) => handler),
}));

// Mock next/server since it is not available in the jsdom test environment
vi.mock('next/server', () => ({
  NextResponse: {
    redirect: vi.fn(),
    rewrite: vi.fn(),
    next: vi.fn(),
  },
}));

const { NEXT_URL_ALLOWLIST } = await import('~/middleware');

describe('NEXT_URL_ALLOWLIST', () => {
  it('accepts internal paths like /transactions', () => {
    // arrange
    const path = '/transactions';

    // act
    const result = NEXT_URL_ALLOWLIST.test(path);

    // assert
    expect(result).toBe(true);
  });

  it('accepts nested paths like /admin/users', () => {
    // arrange
    const path = '/admin/users';

    // act
    const result = NEXT_URL_ALLOWLIST.test(path);

    // assert
    expect(result).toBe(true);
  });

  it('rejects scheme-relative URLs like //evil.com', () => {
    // arrange
    const path = '//evil.com';

    // act
    const result = NEXT_URL_ALLOWLIST.test(path);

    // assert
    expect(result).toBe(false);
  });

  it('rejects root path /', () => {
    // arrange
    const path = '/';

    // act
    const result = NEXT_URL_ALLOWLIST.test(path);

    // assert
    expect(result).toBe(false);
  });

  it('accepts paths with search params appended', () => {
    // arrange
    const path = '/transactions?status=pending';

    // act
    const result = NEXT_URL_ALLOWLIST.test(path);

    // assert
    expect(result).toBe(true);
  });

  it('rejects empty string', () => {
    // arrange
    const path = '';

    // act
    const result = NEXT_URL_ALLOWLIST.test(path);

    // assert
    expect(result).toBe(false);
  });
});
