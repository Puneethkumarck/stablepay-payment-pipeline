import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { PageHeader } from './page-header';

describe('PageHeader', () => {
  it('renders the title', () => {
    render(<PageHeader title="Transactions" />);
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Transactions');
  });

  it('renders eyebrow when provided', () => {
    render(<PageHeader title="Transactions" eyebrow="Customer surface" />);
    expect(screen.getByTestId('page-header')).toHaveTextContent('Customer surface');
  });

  it('renders back button when onBack is provided', () => {
    const onBack = vi.fn();
    render(<PageHeader title="Detail" onBack={onBack} />);
    fireEvent.click(screen.getByTestId('page-header-back'));
    expect(onBack).toHaveBeenCalledOnce();
  });

  it('does not render back button when onBack is not provided', () => {
    render(<PageHeader title="Dashboard" />);
    expect(screen.queryByTestId('page-header-back')).toBeNull();
  });

  it('renders actions slot', () => {
    render(<PageHeader title="Test" actions={<button type="button">Action</button>} />);
    expect(screen.getByText('Action')).toBeInTheDocument();
  });
});
