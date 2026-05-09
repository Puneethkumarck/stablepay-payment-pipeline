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
    // act
    const { container } = render(<LiveFeed visible={false} onHide={() => {}} />);

    // assert
    expect(container.firstChild).toBeNull();
  });

  it('renders when visible', () => {
    // act
    render(<LiveFeed visible onHide={() => {}} />);

    // assert
    expect(screen.getByRole('complementary')).toBeInTheDocument();
  });

  it('has ARIA live region attributes', () => {
    // act
    render(<LiveFeed visible onHide={() => {}} />);

    // assert
    const feed = screen.getByRole('complementary');
    expect(feed).toHaveAttribute('aria-live', 'polite');
    expect(feed).toHaveAttribute('aria-label', 'Live transaction feed');
  });

  it('shows SSE feed label when connected', () => {
    // act
    render(<LiveFeed visible onHide={() => {}} />);

    // assert
    expect(screen.getByText('SSE feed')).toBeInTheDocument();
  });

  it('shows empty state when no events', () => {
    // act
    render(<LiveFeed visible onHide={() => {}} />);

    // assert
    expect(screen.getByText('Listening for events…')).toBeInTheDocument();
  });

  it('shows admin eyebrow when isAdmin', () => {
    // act
    render(<LiveFeed visible onHide={() => {}} isAdmin />);

    // assert
    expect(screen.getByText('All customers')).toBeInTheDocument();
  });

  it('shows user email as eyebrow for customer view', () => {
    // act
    render(<LiveFeed visible onHide={() => {}} userEmail="alice@test.com" />);

    // assert
    expect(screen.getByText('alice@test.com')).toBeInTheDocument();
  });

  it('shows reconnecting when status is connecting', () => {
    // arrange
    mockFeedState({ status: 'connecting' });

    // act
    render(<LiveFeed visible onHide={() => {}} />);

    // assert
    expect(screen.getByText('Reconnecting…')).toBeInTheDocument();
  });
});
