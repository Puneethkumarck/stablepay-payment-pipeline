import { render } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const mockConnect = vi.fn();
const mockDisconnect = vi.fn();

vi.mock('~/lib/stores/sse-client-store', () => ({
  useTransactionFeed: (selector: (s: Record<string, unknown>) => unknown) =>
    selector({ connect: mockConnect, disconnect: mockDisconnect }),
}));

import { LiveFeedConnector } from './live-feed-connector';

describe('LiveFeedConnector', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('calls connect with accessToken on mount', () => {
    // act
    render(<LiveFeedConnector accessToken="test-token" />);

    // assert
    expect(mockConnect).toHaveBeenCalledWith('test-token', '/api/v1/streams/transactions');
  });

  it('calls disconnect on unmount', () => {
    // arrange
    const { unmount } = render(<LiveFeedConnector accessToken="test-token" />);

    // act
    unmount();

    // assert
    expect(mockDisconnect).toHaveBeenCalled();
  });

  it('does not connect when accessToken is undefined', () => {
    // act
    render(<LiveFeedConnector />);

    // assert
    expect(mockConnect).not.toHaveBeenCalled();
  });

  it('renders nothing', () => {
    // act
    const { container } = render(<LiveFeedConnector accessToken="test-token" />);

    // assert
    expect(container.firstChild).toBeNull();
  });
});
