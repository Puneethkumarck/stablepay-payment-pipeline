import { screen } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { createTransaction, createTransactionPage } from '~/test/fixtures/transaction';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { RecentTxnsCard } from './recent-txns-card';

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

describe('RecentTxnsCard', () => {
  it('renders recent transactions heading and view-all link', async () => {
    // arrange
    const page = createTransactionPage({
      data: [createTransaction({ ref: 'TXN-001' })],
    });
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(page)));

    // act
    render(<RecentTxnsCard />);

    // assert
    expect(await screen.findByText('Recent transactions')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /view all/i })).toHaveAttribute(
      'href',
      '/transactions',
    );
  });

  it('renders transaction rows with ref and status', async () => {
    // arrange
    const page = createTransactionPage({
      data: [
        createTransaction({ ref: 'TXN-AAA', internal_status: 'COMPLETED' }),
        createTransaction({ ref: 'TXN-BBB', internal_status: 'PENDING' }),
      ],
    });
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(page)));

    // act
    render(<RecentTxnsCard />);

    // assert
    expect(await screen.findByText('TXN-AAA')).toBeInTheDocument();
    expect(screen.getByText('TXN-BBB')).toBeInTheDocument();
  });

  it('shows empty state when no transactions', async () => {
    // arrange
    server.use(
      http.get('/api/v1/transactions', () =>
        HttpResponse.json(createTransactionPage({ data: [] })),
      ),
    );

    // act
    render(<RecentTxnsCard />);

    // assert
    expect(await screen.findByText('No transactions yet')).toBeInTheDocument();
  });
});
