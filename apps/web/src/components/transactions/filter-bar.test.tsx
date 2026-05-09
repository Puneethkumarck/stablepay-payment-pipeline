import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { render } from '~/test/render';
import { FilterBar } from './filter-bar';

describe('FilterBar', () => {
  it('renders search bar and all 5 quick-filter chips', () => {
    // arrange & act
    render(
      <FilterBar
        search=""
        onSearchChange={vi.fn()}
        selectedStatuses={[]}
        onStatusesChange={vi.fn()}
      />,
    );

    // assert
    expect(screen.getByTestId('search-bar')).toBeInTheDocument();
    expect(screen.getByTestId('chip-SENT_TO_PARTNER')).toBeInTheDocument();
    expect(screen.getByTestId('chip-COMPLETED')).toBeInTheDocument();
    expect(screen.getByTestId('chip-FAILED')).toBeInTheDocument();
    expect(screen.getByTestId('chip-SCREENING_HOLD')).toBeInTheDocument();
    expect(screen.getByTestId('chip-STUCK')).toBeInTheDocument();
  });

  it('calls onStatusesChange when a chip is clicked', async () => {
    // arrange
    const user = userEvent.setup();
    const onStatusesChange = vi.fn();
    render(
      <FilterBar
        search=""
        onSearchChange={vi.fn()}
        selectedStatuses={[]}
        onStatusesChange={onStatusesChange}
      />,
    );

    // act
    await user.click(screen.getByTestId('chip-COMPLETED'));

    // assert
    expect(onStatusesChange).toHaveBeenCalledWith(['COMPLETED']);
  });

  it('removes status when clicking an active chip', async () => {
    // arrange
    const user = userEvent.setup();
    const onStatusesChange = vi.fn();
    render(
      <FilterBar
        search=""
        onSearchChange={vi.fn()}
        selectedStatuses={['COMPLETED', 'FAILED']}
        onStatusesChange={onStatusesChange}
      />,
    );

    // act
    await user.click(screen.getByTestId('chip-COMPLETED'));

    // assert
    expect(onStatusesChange).toHaveBeenCalledWith(['FAILED']);
  });

  it('renders status filter dropdown', () => {
    // arrange & act
    render(
      <FilterBar
        search=""
        onSearchChange={vi.fn()}
        selectedStatuses={[]}
        onStatusesChange={vi.fn()}
      />,
    );

    // assert
    expect(screen.getByTestId('status-filter')).toBeInTheDocument();
  });
});
