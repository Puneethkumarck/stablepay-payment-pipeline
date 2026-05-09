import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { StatusBadge } from './status-badge';

describe('StatusBadge', () => {
  it('renders the label for a known status', () => {
    // act
    render(<StatusBadge status="COMPLETED" />);

    // assert
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('renders the raw status string for an unknown status', () => {
    // arrange
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    // act
    render(<StatusBadge status="UNKNOWN_STATUS" />);

    // assert
    expect(screen.getByText('UNKNOWN_STATUS')).toBeInTheDocument();
    warnSpy.mockRestore();
  });

  it('applies pulse for in-progress statuses via data attribute', () => {
    // act
    render(<StatusBadge status="STUCK" />);

    // assert
    expect(screen.getByTestId('status-badge')).toHaveAttribute('data-pulse', 'true');
  });

  it('does not apply pulse for terminal statuses', () => {
    // act
    render(<StatusBadge status="COMPLETED" />);

    // assert
    expect(screen.getByTestId('status-badge')).not.toHaveAttribute('data-pulse');
  });

  it('renders success color for COMPLETED', () => {
    // act
    render(<StatusBadge status="COMPLETED" />);

    // assert
    expect(screen.getByTestId('status-badge')).toHaveAttribute('data-color', 'success');
  });

  it('renders danger color for FAILED', () => {
    // act
    render(<StatusBadge status="FAILED" />);

    // assert
    expect(screen.getByTestId('status-badge')).toHaveAttribute('data-color', 'danger');
  });

  it('renders warning color for PENDING_APPROVAL', () => {
    // act
    render(<StatusBadge status="PENDING_APPROVAL" />);

    // assert
    expect(screen.getByTestId('status-badge')).toHaveAttribute('data-color', 'warning');
  });

  it('renders neutral color for unknown statuses', () => {
    // arrange
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

    // act
    render(<StatusBadge status="SOMETHING_NEW" />);

    // assert
    expect(screen.getByTestId('status-badge')).toHaveAttribute('data-color', 'neutral');
    warnSpy.mockRestore();
  });

  it('accepts additional className', () => {
    // act
    render(<StatusBadge status="COMPLETED" className="ml-2" />);

    // assert
    expect(screen.getByTestId('status-badge').className).toContain('ml-2');
  });
});
