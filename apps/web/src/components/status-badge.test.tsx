import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { StatusBadge } from './status-badge';

describe('StatusBadge', () => {
  it('renders the label for a known status', () => {
    render(<StatusBadge status="COMPLETED" />);
    expect(screen.getByTestId('status-badge')).toHaveTextContent('Completed');
  });

  it('renders the raw status string for an unknown status', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    render(<StatusBadge status="UNKNOWN_STATUS" />);
    expect(screen.getByTestId('status-badge')).toHaveTextContent('UNKNOWN_STATUS');
    warnSpy.mockRestore();
  });

  it('applies pulse animation for in-progress statuses', () => {
    render(<StatusBadge status="STUCK" />);
    const dot = screen.getByTestId('status-badge').querySelector('span:first-child');
    expect(dot?.className).toContain('animate-');
  });

  it('does not apply pulse animation for terminal statuses', () => {
    render(<StatusBadge status="COMPLETED" />);
    const dot = screen.getByTestId('status-badge').querySelector('span:first-child');
    expect(dot?.className).not.toContain('animate-');
  });

  it('renders success color classes for COMPLETED', () => {
    render(<StatusBadge status="COMPLETED" />);
    const badge = screen.getByTestId('status-badge');
    expect(badge.className).toContain('text-[#86EFAC]');
  });

  it('renders danger color classes for FAILED', () => {
    render(<StatusBadge status="FAILED" />);
    const badge = screen.getByTestId('status-badge');
    expect(badge.className).toContain('text-[#FCA5A5]');
  });

  it('renders warning color classes for PENDING_APPROVAL', () => {
    render(<StatusBadge status="PENDING_APPROVAL" />);
    const badge = screen.getByTestId('status-badge');
    expect(badge.className).toContain('text-[#FCD34D]');
  });

  it('renders neutral color for unknown statuses', () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
    render(<StatusBadge status="SOMETHING_NEW" />);
    const badge = screen.getByTestId('status-badge');
    expect(badge.className).toContain('text-[rgba(255,255,255,0.40)]');
    warnSpy.mockRestore();
  });

  it('accepts additional className', () => {
    render(<StatusBadge status="COMPLETED" className="ml-2" />);
    expect(screen.getByTestId('status-badge').className).toContain('ml-2');
  });
});
