import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createTransaction, createTransactionPage } from '~/test/fixtures/transaction';
import { render } from '~/test/render';
import { RecentTxnsCard } from './recent-txns-card';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('~/lib/hooks/use-transactions-list', () => ({
  useTransactionsList: vi.fn(),
}));

import { useTransactionsList } from '~/lib/hooks/use-transactions-list';

const mockUseTransactionsList = vi.mocked(useTransactionsList);

describe('RecentTxnsCard', () => {
  it('renders recent transactions heading and view-all link', () => {
    // arrange
    const page = createTransactionPage({
      data: [createTransaction({ ref: 'TXN-001' })],
    });
    mockUseTransactionsList.mockReturnValue({
      data: page,
      isLoading: false,
    } as unknown as ReturnType<typeof useTransactionsList>);

    // act
    render(<RecentTxnsCard />);

    // assert
    expect(screen.getByText('Recent transactions')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /view all/i })).toHaveAttribute(
      'href',
      '/transactions',
    );
  });

  it('renders transaction rows with ref and status', () => {
    // arrange
    const page = createTransactionPage({
      data: [
        createTransaction({ ref: 'TXN-AAA', internal_status: 'COMPLETED' }),
        createTransaction({ ref: 'TXN-BBB', internal_status: 'PENDING' }),
      ],
    });
    mockUseTransactionsList.mockReturnValue({
      data: page,
      isLoading: false,
    } as unknown as ReturnType<typeof useTransactionsList>);

    // act
    render(<RecentTxnsCard />);

    // assert
    expect(screen.getByText('TXN-AAA')).toBeInTheDocument();
    expect(screen.getByText('TXN-BBB')).toBeInTheDocument();
  });

  it('shows empty state when no transactions', () => {
    // arrange
    const page = createTransactionPage({ data: [] });
    mockUseTransactionsList.mockReturnValue({
      data: page,
      isLoading: false,
    } as unknown as ReturnType<typeof useTransactionsList>);

    // act
    render(<RecentTxnsCard />);

    // assert
    expect(screen.getByText('No transactions yet')).toBeInTheDocument();
  });
});
