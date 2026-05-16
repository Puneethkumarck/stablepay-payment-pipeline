import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createStuckPayment } from '~/test/fixtures/stuck';

vi.mock('~/lib/data', () => ({
  fetchStuckList: vi.fn(),
}));

vi.mock('~/components/stuck/stuck-list', () => ({
  StuckList: () => <div data-testid="stuck-list-mock">StuckList</div>,
}));

vi.mock('@tanstack/react-query', async () => {
  const actual =
    await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');
  return {
    ...actual,
    dehydrate: () => ({}),
    HydrationBoundary: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  };
});

import { fetchStuckList } from '~/lib/data';
import StuckPage from './page';

const mockFetchStuckList = vi.mocked(fetchStuckList);

describe('StuckPage', () => {
  it('prefetches stuck list and renders StuckList', async () => {
    // arrange
    mockFetchStuckList.mockResolvedValue([createStuckPayment()]);

    // act
    const jsx = await StuckPage();
    render(jsx);

    // assert
    expect(screen.getByTestId('stuck-list-mock')).toBeInTheDocument();
    expect(mockFetchStuckList).toHaveBeenCalled();
  });
});
