import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Sidebar } from './sidebar';

const defaultProps = {
  activePage: 'dashboard',
  onNavigate: vi.fn(),
  email: 'alice@stablepay.io',
  role: 'Admin',
  customerId: 'cust-001',
  onSignOut: vi.fn(),
};

describe('Sidebar', () => {
  beforeEach(() => {
    localStorage.clear();
    defaultProps.onNavigate.mockClear();
    defaultProps.onSignOut.mockClear();
  });

  it('renders navigation links', () => {
    // act
    render(<Sidebar {...defaultProps} />);

    // assert
    expect(screen.getByTestId('nav-dashboard')).toBeInTheDocument();
    expect(screen.getByTestId('nav-transactions')).toBeInTheDocument();
    expect(screen.getByTestId('nav-flows')).toBeInTheDocument();
    expect(screen.getByTestId('nav-customers')).toBeInTheDocument();
    expect(screen.getByTestId('nav-dlq')).toBeInTheDocument();
    expect(screen.getByTestId('nav-stuck')).toBeInTheDocument();
  });

  it('renders nav item labels when expanded', () => {
    // act
    render(<Sidebar {...defaultProps} />);

    // assert
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Transactions')).toBeInTheDocument();
    expect(screen.getByText('Flows')).toBeInTheDocument();
  });

  it('calls onNavigate when nav item is clicked', async () => {
    // arrange
    const user = userEvent.setup();
    render(<Sidebar {...defaultProps} />);

    // act
    await user.click(screen.getByTestId('nav-transactions'));

    // assert
    expect(defaultProps.onNavigate).toHaveBeenCalledWith('transactions', '/transactions');
  });

  it('toggles collapse state via toggle button', async () => {
    // arrange
    const user = userEvent.setup();
    render(<Sidebar {...defaultProps} />);
    const sidebar = screen.getByRole('navigation');

    // act
    expect(sidebar).toHaveAttribute('data-collapsed', 'false');
    await user.click(screen.getByTestId('sidebar-toggle'));

    // assert
    expect(sidebar).toHaveAttribute('data-collapsed', 'true');
  });

  it('persists collapse state to localStorage', async () => {
    // arrange
    const user = userEvent.setup();
    render(<Sidebar {...defaultProps} />);

    // act
    await user.click(screen.getByTestId('sidebar-toggle'));

    // assert
    expect(localStorage.getItem('sp4_sidebar_collapsed')).toBe('true');
  });

  it('supports Cmd+B keyboard shortcut', () => {
    // arrange
    render(<Sidebar {...defaultProps} />);
    const sidebar = screen.getByRole('navigation');
    expect(sidebar).toHaveAttribute('data-collapsed', 'false');

    // act
    fireEvent.keyDown(document, { key: 'b', metaKey: true });

    // assert
    expect(sidebar).toHaveAttribute('data-collapsed', 'true');
  });

  it('supports Ctrl+B keyboard shortcut', () => {
    // arrange
    render(<Sidebar {...defaultProps} />);
    const sidebar = screen.getByRole('navigation');
    expect(sidebar).toHaveAttribute('data-collapsed', 'false');

    // act
    fireEvent.keyDown(document, { key: 'b', ctrlKey: true });

    // assert
    expect(sidebar).toHaveAttribute('data-collapsed', 'true');
  });

  it('renders logo text when expanded', () => {
    // act
    render(<Sidebar {...defaultProps} />);

    // assert
    expect(screen.getByRole('navigation')).toHaveTextContent('stablepay');
  });

  it('has nav landmark with aria-label', () => {
    // act
    render(<Sidebar {...defaultProps} />);

    // assert
    expect(screen.getByRole('navigation')).toHaveAttribute('aria-label', 'Primary navigation');
  });
});
