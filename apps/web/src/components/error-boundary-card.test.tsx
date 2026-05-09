import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ErrorBoundaryCard } from './error-boundary-card';

describe('ErrorBoundaryCard', () => {
  it('renders heading', () => {
    // act
    render(<ErrorBoundaryCard />);

    // assert
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Something went wrong');
  });

  it('renders support text', () => {
    // act
    render(<ErrorBoundaryCard />);

    // assert
    expect(screen.getByText(/logged the error/)).toBeInTheDocument();
  });

  it('renders reload CTA', () => {
    // act
    render(<ErrorBoundaryCard />);

    // assert
    expect(screen.getByRole('button', { name: 'Reload' })).toBeInTheDocument();
  });

  it('calls onReset when reload is clicked', async () => {
    // arrange
    const user = userEvent.setup();
    const onReset = vi.fn();
    render(<ErrorBoundaryCard onReset={onReset} />);

    // act
    await user.click(screen.getByRole('button', { name: 'Reload' }));

    // assert
    expect(onReset).toHaveBeenCalledOnce();
  });

  it('calls window.location.reload when no onReset provided', async () => {
    // arrange
    const user = userEvent.setup();
    const reloadMock = vi.fn();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, reload: reloadMock },
      writable: true,
    });
    render(<ErrorBoundaryCard />);

    // act
    await user.click(screen.getByRole('button', { name: 'Reload' }));

    // assert
    expect(reloadMock).toHaveBeenCalledOnce();
  });
});
