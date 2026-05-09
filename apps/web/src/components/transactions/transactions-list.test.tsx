import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createTransaction, createTransactionPage } from '~/test/fixtures/transaction';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { TransactionsList } from './transactions-list';

const pushMock = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: pushMock }),
}));

describe('TransactionsList', () => {
  beforeEach(() => {
    pushMock.mockClear();
  });
  it('renders page header and filter bar', async () => {
    // arrange
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(createTransactionPage())));

    // act
    render(<TransactionsList />);

    // assert
    expect(await screen.findByText('Transactions')).toBeInTheDocument();
    expect(screen.getByTestId('filter-bar')).toBeInTheDocument();
  });

  it('renders transaction rows from API', async () => {
    // arrange
    const page = createTransactionPage({
      data: [
        createTransaction({ ref: 'TXN-100', internal_status: 'COMPLETED' }),
        createTransaction({ ref: 'TXN-200', internal_status: 'FAILED' }),
      ],
    });
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(page)));

    // act
    render(<TransactionsList />);

    // assert
    expect(await screen.findByText('TXN-100')).toBeInTheDocument();
    expect(screen.getByText('TXN-200')).toBeInTheDocument();
  });

  it('shows empty state when no transactions match', async () => {
    // arrange
    server.use(
      http.get('/api/v1/transactions', () =>
        HttpResponse.json(createTransactionPage({ data: [] })),
      ),
    );

    // act
    render(<TransactionsList />);

    // assert
    expect(await screen.findByText('No transactions match your filters')).toBeInTheDocument();
  });

  it('navigates to transaction detail on row click', async () => {
    // arrange
    const user = userEvent.setup();
    const page = createTransactionPage({
      data: [createTransaction({ ref: 'TXN-NAV' })],
    });
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(page)));

    // act
    render(<TransactionsList />);
    const row = await screen.findByText('TXN-NAV');
    const tr = row.closest('tr');
    expect(tr).toBeTruthy();
    await user.click(tr as HTMLElement);

    // assert
    expect(pushMock).toHaveBeenCalledWith('/transactions/TXN-NAV');
  });

  it('renders polling footer', async () => {
    // arrange
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(createTransactionPage())));

    // act
    render(<TransactionsList />);

    // assert
    expect(await screen.findByTestId('transactions-footer')).toBeInTheDocument();
    expect(screen.getByTestId('transactions-footer')).toHaveTextContent('3s polling active');
  });

  it('renders quick-filter chips that toggle status', async () => {
    // arrange
    const user = userEvent.setup();
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(createTransactionPage())));

    // act
    render(<TransactionsList />);
    const chip = await screen.findByTestId('chip-COMPLETED');
    await user.click(chip);

    // assert — chip should now have active styling (accent border visible)
    expect(chip).toHaveAttribute('aria-pressed', 'true');
  });
});
