import { render } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';

const mockUseMultiTabSessionSync = vi.fn();

vi.mock('~/lib/hooks/use-multi-tab-session-sync', () => ({
  useMultiTabSessionSync: () => mockUseMultiTabSessionSync(),
}));

import { MultiTabSessionGuard } from './multi-tab-session-guard';

describe('MultiTabSessionGuard', () => {
  it('calls useMultiTabSessionSync and renders nothing', () => {
    // act
    const { container } = render(<MultiTabSessionGuard />);

    // assert
    expect(mockUseMultiTabSessionSync).toHaveBeenCalled();
    expect(container.innerHTML).toBe('');
  });
});
