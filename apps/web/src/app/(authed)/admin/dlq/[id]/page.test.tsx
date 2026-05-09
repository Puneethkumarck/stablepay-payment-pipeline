import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createDlqEntry } from '~/test/fixtures/dlq';

const mockNotFound = vi.fn();
vi.mock('next/navigation', () => ({
  notFound: () => {
    mockNotFound();
    throw new Error('NEXT_NOT_FOUND');
  },
}));

vi.mock('~/lib/data', () => ({
  fetchDlqEntry: vi.fn(),
}));

vi.mock('~/components/dlq/dlq-detail', () => ({
  DlqDetail: ({ dlqId, initialData }: { dlqId: string; initialData: unknown }) => (
    <div data-testid="dlq-detail-mock">
      {dlqId} {JSON.stringify(!!initialData)}
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

import { fetchDlqEntry } from '~/lib/data';
import DlqDetailPage from './page';

const mockFetch = vi.mocked(fetchDlqEntry);
const params = Promise.resolve({ id: 'DLQ-TEST-001' });

async function renderPage() {
  const jsx = await DlqDetailPage({ params });
  render(jsx);
}

describe('DlqDetailPage', () => {
  it('calls notFound when entry is null', async () => {
    // arrange
    mockFetch.mockResolvedValue(null);

    // act + assert
    await expect(renderPage()).rejects.toThrow('NEXT_NOT_FOUND');
    expect(mockNotFound).toHaveBeenCalled();
  });

  it('renders DlqDetail with fetched entry', async () => {
    // arrange
    mockFetch.mockResolvedValue(createDlqEntry({ id: 'DLQ-TEST-001' }));

    // act
    await renderPage();

    // assert
    expect(screen.getByTestId('dlq-detail-mock')).toHaveTextContent('DLQ-TEST-001');
    expect(mockFetch).toHaveBeenCalledWith('DLQ-TEST-001');
  });
});
