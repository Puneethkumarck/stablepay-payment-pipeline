import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createStuckPayment } from '~/test/fixtures/stuck';
import { render } from '~/test/render';
import { StuckAlert } from './stuck-alert';

vi.mock('~/lib/hooks/use-stuck-list', () => ({
  useStuckList: vi.fn(),
}));

import { useStuckList } from '~/lib/hooks/use-stuck-list';

const mockUseStuckList = vi.mocked(useStuckList);

describe('StuckAlert', () => {
  it('renders nothing when stuck count is 0', () => {
    // arrange
    mockUseStuckList.mockReturnValue({ data: [] } as unknown as ReturnType<typeof useStuckList>);

    // act
    const { container } = render(<StuckAlert />);

    // assert
    expect(container.firstChild).toBeNull();
  });

  it('renders alert with stuck count', () => {
    // arrange
    const stuck = [createStuckPayment(), createStuckPayment({ transaction_ref: 'TXN-STUCK-002' })];
    mockUseStuckList.mockReturnValue({ data: stuck } as unknown as ReturnType<
      typeof useStuckList
    >);

    // act
    render(<StuckAlert />);

    // assert
    expect(screen.getByTestId('stuck-alert')).toBeInTheDocument();
    expect(screen.getByText('2 stuck payments')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /review/i })).toHaveAttribute('href', '/admin/stuck');
  });

  it('renders singular text for 1 stuck payment', () => {
    // arrange
    const stuck = [createStuckPayment()];
    mockUseStuckList.mockReturnValue({ data: stuck } as unknown as ReturnType<
      typeof useStuckList
    >);

    // act
    render(<StuckAlert />);

    // assert
    expect(screen.getByText('1 stuck payment')).toBeInTheDocument();
  });

  it('shows critical badge when payments are stuck for over 24h', () => {
    // arrange
    const stuck = [
      createStuckPayment({
        stuck_since: new Date(Date.now() - 48 * 60 * 60 * 1000).toISOString(),
      }),
    ];
    mockUseStuckList.mockReturnValue({ data: stuck } as unknown as ReturnType<
      typeof useStuckList
    >);

    // act
    render(<StuckAlert />);

    // assert
    expect(screen.getByText('1 critical')).toBeInTheDocument();
  });
});
