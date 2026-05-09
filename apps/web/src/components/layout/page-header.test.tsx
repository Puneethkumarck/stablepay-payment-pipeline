import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PageHeader } from './page-header';

describe('PageHeader', () => {
  it('renders the title', () => {
    // act
    render(<PageHeader title="Transactions" />);

    // assert
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Transactions');
  });

  it('renders eyebrow when provided', () => {
    // act
    render(<PageHeader title="Transactions" eyebrow="Customer surface" />);

    // assert
    expect(screen.getByText('Customer surface')).toBeInTheDocument();
  });

  it('renders back button when onBack is provided', async () => {
    // arrange
    const user = userEvent.setup();
    const onBack = vi.fn();
    render(<PageHeader title="Detail" onBack={onBack} />);

    // act
    await user.click(screen.getByTestId('page-header-back'));

    // assert
    expect(onBack).toHaveBeenCalledOnce();
  });

  it('does not render back button when onBack is not provided', () => {
    // act
    render(<PageHeader title="Dashboard" />);

    // assert
    expect(screen.queryByTestId('page-header-back')).toBeNull();
  });

  it('renders actions slot', () => {
    // act
    render(<PageHeader title="Test" actions={<button type="button">Action</button>} />);

    // assert
    expect(screen.getByRole('button', { name: 'Action' })).toBeInTheDocument();
  });
});
