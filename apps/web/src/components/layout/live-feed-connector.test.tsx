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
    render(<LiveFeedConnector accessToken="test-token" />);

    expect(mockConnect).toHaveBeenCalledWith('test-token', '/api/v1/streams/transactions');
  });

  it('calls disconnect on unmount', () => {
    const { unmount } = render(<LiveFeedConnector accessToken="test-token" />);

    unmount();

    expect(mockDisconnect).toHaveBeenCalled();
  });

  it('does not connect when accessToken is undefined', () => {
    render(<LiveFeedConnector />);

    expect(mockConnect).not.toHaveBeenCalled();
  });

  it('renders nothing', () => {
    const { container } = render(<LiveFeedConnector accessToken="test-token" />);

    expect(container.firstChild).toBeNull();
  });
});
