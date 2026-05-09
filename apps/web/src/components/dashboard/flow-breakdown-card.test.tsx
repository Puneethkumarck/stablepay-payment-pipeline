import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createTransaction, createTransactionPage } from '~/test/fixtures/transaction';
import { render } from '~/test/render';
import { FlowBreakdownCard } from './flow-breakdown-card';

vi.mock('~/lib/hooks/use-transactions-list', () => ({
  useTransactionsList: vi.fn(),
}));

import { useTransactionsList } from '~/lib/hooks/use-transactions-list';

const mockUseTransactionsList = vi.mocked(useTransactionsList);

describe('FlowBreakdownCard', () => {
  it('renders all 4 flow categories', () => {
    // arrange
    const page = createTransactionPage({ data: [] });
    mockUseTransactionsList.mockReturnValue({
      data: page,
      isLoading: false,
    } as unknown as ReturnType<typeof useTransactionsList>);

    // act
    render(<FlowBreakdownCard />);

    // assert
    expect(screen.getByText('Fiat payin')).toBeInTheDocument();
    expect(screen.getByText('Fiat payout')).toBeInTheDocument();
    expect(screen.getByText('Crypto')).toBeInTheDocument();
    expect(screen.getByText('Multi-leg')).toBeInTheDocument();
  });

  it('renders correct counts for each category', () => {
    // arrange
    const page = createTransactionPage({
      data: [
        createTransaction({ type: 'FIAT', direction: 'PAYIN' }),
        createTransaction({ type: 'FIAT', direction: 'PAYIN', ref: 'TXN-002' }),
        createTransaction({ type: 'CRYPTO', direction: 'PAYOUT', ref: 'TXN-003' }),
      ],
    });
    mockUseTransactionsList.mockReturnValue({
      data: page,
      isLoading: false,
    } as unknown as ReturnType<typeof useTransactionsList>);

    // act
    render(<FlowBreakdownCard />);

    // assert
    expect(screen.getByText('Flow breakdown')).toBeInTheDocument();
    const progressBars = screen.getAllByRole('progressbar');
    expect(progressBars).toHaveLength(4);
  });

  it('renders progress bars with accessible labels', () => {
    // arrange
    const page = createTransactionPage({
      data: [createTransaction({ type: 'FIAT', direction: 'PAYIN' })],
    });
    mockUseTransactionsList.mockReturnValue({
      data: page,
      isLoading: false,
    } as unknown as ReturnType<typeof useTransactionsList>);

    // act
    render(<FlowBreakdownCard />);

    // assert
    expect(screen.getByRole('progressbar', { name: /fiat payin/i })).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: /crypto/i })).toBeInTheDocument();
  });
});
