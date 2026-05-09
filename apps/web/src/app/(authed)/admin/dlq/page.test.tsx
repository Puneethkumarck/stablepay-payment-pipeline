import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createDlqPage, createDlqSummary } from '~/test/fixtures/dlq';

vi.mock('~/lib/data', () => ({
  fetchDlqList: vi.fn(),
  fetchDlqSummary: vi.fn(),
}));

vi.mock('~/components/dlq/dlq-list', () => ({
  DlqList: ({ initialData, initialSummary }: { initialData: unknown; initialSummary: unknown }) => (
    <div data-testid="dlq-list-mock">
      {JSON.stringify({ hasData: !!initialData, hasSummary: !!initialSummary })}
    </div>
  ),
}));

vi.mock('@tanstack/react-query', async () => {
  const actual = await vi.importActual<typeof import('@tanstack/react-query')>(
    '@tanstack/react-query',
  );
  return {
    ...actual,
    dehydrate: () => ({}),
    HydrationBoundary: ({ children }: { children: React.ReactNode }) => <>{children}</>,
  };
});

import { fetchDlqList, fetchDlqSummary } from '~/lib/data';
import DlqListPage from './page';

const mockFetchList = vi.mocked(fetchDlqList);
const mockFetchSummary = vi.mocked(fetchDlqSummary);

describe('DlqListPage', () => {
  it('prefetches DLQ list and summary then renders DlqList', async () => {
    // arrange
    mockFetchList.mockResolvedValue(createDlqPage());
    mockFetchSummary.mockResolvedValue(createDlqSummary());

    // act
    const jsx = await DlqListPage();
    render(jsx);

    // assert
    expect(screen.getByTestId('dlq-list-mock')).toBeInTheDocument();
    expect(mockFetchList).toHaveBeenCalled();
    expect(mockFetchSummary).toHaveBeenCalled();
  });
});
