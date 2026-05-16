import { renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const replaceFn = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: replaceFn }),
}));

import { useMultiTabSessionSync } from './use-multi-tab-session-sync';

describe('useMultiTabSessionSync', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('sets sp4_authed flag in localStorage on mount', () => {
    // act
    renderHook(() => useMultiTabSessionSync());

    // assert
    expect(localStorage.getItem('sp4_authed')).toBe('1');
  });

  it('redirects to login when sp4_authed is removed via storage event', () => {
    // arrange
    renderHook(() => useMultiTabSessionSync());

    // act
    window.dispatchEvent(new StorageEvent('storage', { key: 'sp4_authed', newValue: null }));

    // assert
    expect(replaceFn).toHaveBeenCalledWith('/login?reason=signed-out-elsewhere');
  });

  it('ignores storage events for other keys', () => {
    // arrange
    renderHook(() => useMultiTabSessionSync());

    // act
    window.dispatchEvent(new StorageEvent('storage', { key: 'sp4_theme', newValue: 'light' }));

    // assert
    expect(replaceFn).not.toHaveBeenCalled();
  });

  it('ignores storage events where sp4_authed is set (not removed)', () => {
    // arrange
    renderHook(() => useMultiTabSessionSync());

    // act
    window.dispatchEvent(new StorageEvent('storage', { key: 'sp4_authed', newValue: '1' }));

    // assert
    expect(replaceFn).not.toHaveBeenCalled();
  });

  it('cleans up event listener on unmount', () => {
    // arrange
    const { unmount } = renderHook(() => useMultiTabSessionSync());

    // act
    unmount();
    window.dispatchEvent(new StorageEvent('storage', { key: 'sp4_authed', newValue: null }));

    // assert
    expect(replaceFn).not.toHaveBeenCalled();
  });
});
