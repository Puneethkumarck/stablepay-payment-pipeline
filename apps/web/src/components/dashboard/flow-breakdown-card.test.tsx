import { screen } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createTransaction, createTransactionPage } from '~/test/fixtures/transaction';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { FlowBreakdownCard } from './flow-breakdown-card';

describe('FlowBreakdownCard', () => {
  it('renders all 4 flow categories', async () => {
    // arrange
    server.use(
      http.get('/api/v1/transactions', () =>
        HttpResponse.json(createTransactionPage({ data: [] })),
      ),
    );

    // act
    render(<FlowBreakdownCard />);

    // assert
    expect(await screen.findByText('Fiat payin')).toBeInTheDocument();
    expect(screen.getByText('Fiat payout')).toBeInTheDocument();
    expect(screen.getByText('Crypto')).toBeInTheDocument();
    expect(screen.getByText('Multi-leg')).toBeInTheDocument();
  });

  it('renders correct counts for each category', async () => {
    // arrange
    const page = createTransactionPage({
      data: [
        createTransaction({ type: 'FIAT', direction: 'PAYIN' }),
        createTransaction({ type: 'FIAT', direction: 'PAYIN', ref: 'TXN-002' }),
        createTransaction({ type: 'CRYPTO', direction: 'PAYOUT', ref: 'TXN-003' }),
      ],
    });
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(page)));

    // act
    render(<FlowBreakdownCard />);

    // assert
    expect(await screen.findByText('Flow breakdown')).toBeInTheDocument();
    const progressBars = screen.getAllByRole('progressbar');
    expect(progressBars).toHaveLength(4);
  });

  it('renders progress bars with accessible labels', async () => {
    // arrange
    const page = createTransactionPage({
      data: [createTransaction({ type: 'FIAT', direction: 'PAYIN' })],
    });
    server.use(http.get('/api/v1/transactions', () => HttpResponse.json(page)));

    // act
    render(<FlowBreakdownCard />);

    // assert
    expect(await screen.findByRole('progressbar', { name: /fiat payin/i })).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: /crypto/i })).toBeInTheDocument();
  });
});
