import { fireEvent, render, screen } from '@testing-library/react';
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
    render(<Sidebar {...defaultProps} />);
    expect(screen.getByTestId('nav-dashboard')).toBeInTheDocument();
    expect(screen.getByTestId('nav-transactions')).toBeInTheDocument();
    expect(screen.getByTestId('nav-flows')).toBeInTheDocument();
    expect(screen.getByTestId('nav-customers')).toBeInTheDocument();
    expect(screen.getByTestId('nav-dlq')).toBeInTheDocument();
    expect(screen.getByTestId('nav-stuck')).toBeInTheDocument();
  });

  it('highlights active page', () => {
    render(<Sidebar {...defaultProps} activePage="transactions" />);
    const txnNav = screen.getByTestId('nav-transactions');
    expect(txnNav.className).toContain('text-[#C4B5FD]');
  });

  it('calls onNavigate when nav item is clicked', () => {
    render(<Sidebar {...defaultProps} />);
    fireEvent.click(screen.getByTestId('nav-transactions'));
    expect(defaultProps.onNavigate).toHaveBeenCalledWith('transactions', '/transactions');
  });

  it('toggles collapse state via toggle button', () => {
    render(<Sidebar {...defaultProps} />);
    const sidebar = screen.getByTestId('sidebar');
    expect(sidebar.className).toContain('w-[212px]');

    fireEvent.click(screen.getByTestId('sidebar-toggle'));
    expect(sidebar.className).toContain('w-14');
  });

  it('persists collapse state to localStorage', () => {
    render(<Sidebar {...defaultProps} />);
    fireEvent.click(screen.getByTestId('sidebar-toggle'));
    expect(localStorage.getItem('sp4_sidebar_collapsed')).toBe('true');
  });

  it('supports Cmd+B keyboard shortcut', () => {
    render(<Sidebar {...defaultProps} />);
    const sidebar = screen.getByTestId('sidebar');
    expect(sidebar.className).toContain('w-[212px]');

    fireEvent.keyDown(document, { key: 'b', metaKey: true });
    expect(sidebar.className).toContain('w-14');
  });

  it('renders logo text when expanded', () => {
    render(<Sidebar {...defaultProps} />);
    expect(screen.getByTestId('sidebar')).toHaveTextContent('stablepay');
  });

  it('has nav landmark with aria-label', () => {
    render(<Sidebar {...defaultProps} />);
    expect(screen.getByRole('navigation')).toHaveAttribute('aria-label', 'Primary navigation');
  });
});
