import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ErrorBoundaryCard } from './error-boundary-card';

describe('ErrorBoundaryCard', () => {
  it('renders heading', () => {
    render(<ErrorBoundaryCard />);
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Something went wrong');
  });

  it('renders support text', () => {
    render(<ErrorBoundaryCard />);
    expect(screen.getByTestId('error-boundary-card')).toHaveTextContent('logged the error');
  });

  it('renders reload CTA', () => {
    render(<ErrorBoundaryCard />);
    expect(screen.getByTestId('error-boundary-reload')).toHaveTextContent('Reload');
  });

  it('calls onReset when reload is clicked', () => {
    const onReset = vi.fn();
    render(<ErrorBoundaryCard onReset={onReset} />);
    fireEvent.click(screen.getByTestId('error-boundary-reload'));
    expect(onReset).toHaveBeenCalledOnce();
  });
});
