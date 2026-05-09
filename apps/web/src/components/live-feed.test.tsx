import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { TransactionFeedStore } from '~/lib/stores/sse-client-store';

function makeMockState(overrides?: Partial<TransactionFeedStore>): TransactionFeedStore {
  return {
    events: [],
    status: 'connected',
    connect: vi.fn(),
    disconnect: vi.fn(),
    ...overrides,
  };
}

vi.mock('~/lib/stores/sse-client-store', () => ({
  useTransactionFeed: vi.fn(<T,>(selector?: (state: TransactionFeedStore) => T) => {
    const state = makeMockState();
    return selector ? selector(state) : state;
  }),
}));

import { useTransactionFeed } from '~/lib/stores/sse-client-store';
import { LiveFeed } from './live-feed';

function mockFeedState(overrides?: Partial<TransactionFeedStore>) {
  const state = makeMockState(overrides);
  vi.mocked(useTransactionFeed).mockImplementation(
    <T,>(selector?: (s: TransactionFeedStore) => T) => {
      return selector ? selector(state) : (state as unknown as T);
    },
  );
}

describe('LiveFeed', () => {
  beforeEach(() => {
    mockFeedState();
  });

  it('returns null when not visible', () => {
    const { container } = render(<LiveFeed visible={false} onHide={() => {}} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders when visible', () => {
    render(<LiveFeed visible onHide={() => {}} />);
    expect(screen.getByTestId('live-feed')).toBeInTheDocument();
  });

  it('has ARIA live region attributes', () => {
    render(<LiveFeed visible onHide={() => {}} />);
    const feed = screen.getByTestId('live-feed');
    expect(feed).toHaveAttribute('aria-live', 'polite');
    expect(feed).toHaveAttribute('aria-label', 'Live transaction feed');
  });

  it('shows SSE feed label when connected', () => {
    render(<LiveFeed visible onHide={() => {}} />);
    expect(screen.getByTestId('live-feed')).toHaveTextContent('SSE feed');
  });

  it('shows empty state when no events', () => {
    render(<LiveFeed visible onHide={() => {}} />);
    expect(screen.getByTestId('live-feed')).toHaveTextContent('Listening for events…');
  });

  it('shows admin eyebrow when isAdmin', () => {
    render(<LiveFeed visible onHide={() => {}} isAdmin />);
    expect(screen.getByTestId('live-feed')).toHaveTextContent('All customers');
  });

  it('shows user email as eyebrow for customer view', () => {
    render(<LiveFeed visible onHide={() => {}} userEmail="alice@test.com" />);
    expect(screen.getByTestId('live-feed')).toHaveTextContent('alice@test.com');
  });

  it('shows reconnecting when status is connecting', () => {
    mockFeedState({ status: 'connecting' });
    render(<LiveFeed visible onHide={() => {}} />);
    expect(screen.getByTestId('live-feed')).toHaveTextContent('Reconnecting…');
  });
});
