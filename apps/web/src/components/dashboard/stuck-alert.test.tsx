import { screen, waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createStuckPayment } from '~/test/fixtures/stuck';
import { server } from '~/test/msw-server';
import { render } from '~/test/render';
import { StuckAlert } from './stuck-alert';

describe('StuckAlert', () => {
  it('renders nothing when stuck count is 0', async () => {
    // arrange
    server.use(http.get('/api/v1/admin/stuck', () => HttpResponse.json([])));

    // act
    render(<StuckAlert />);

    // assert
    await waitFor(() => {
      expect(screen.queryByTestId('stuck-alert')).not.toBeInTheDocument();
    });
  });

  it('renders alert with stuck count', async () => {
    // arrange
    const stuck = [createStuckPayment(), createStuckPayment({ transaction_ref: 'TXN-STUCK-002' })];
    server.use(http.get('/api/v1/admin/stuck', () => HttpResponse.json(stuck)));

    // act
    render(<StuckAlert />);

    // assert
    expect(await screen.findByTestId('stuck-alert')).toBeInTheDocument();
    expect(screen.getByText('2 stuck payments')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /review/i })).toHaveAttribute('href', '/admin/stuck');
  });

  it('renders singular text for 1 stuck payment', async () => {
    // arrange
    const stuck = [createStuckPayment()];
    server.use(http.get('/api/v1/admin/stuck', () => HttpResponse.json(stuck)));

    // act
    render(<StuckAlert />);

    // assert
    expect(await screen.findByText('1 stuck payment')).toBeInTheDocument();
  });

  it('shows critical badge when payments are stuck for over 24h', async () => {
    // arrange
    const stuck = [
      createStuckPayment({
        stuck_since: new Date(Date.now() - 48 * 60 * 60 * 1000).toISOString(),
      }),
    ];
    server.use(http.get('/api/v1/admin/stuck', () => HttpResponse.json(stuck)));

    // act
    render(<StuckAlert />);

    // assert
    expect(await screen.findByText('1 critical')).toBeInTheDocument();
  });
});
