import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { IdChip } from './id-chip';

describe('IdChip', () => {
  const fullId = 'b8f6c8a4-7d2e-4f3b-9c1e-2a5d6f8e0b1d';

  beforeEach(() => {
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    });
  });

  it('truncates to 8 chars by default', () => {
    render(<IdChip value={fullId} />);
    expect(screen.getByTestId('id-chip')).toHaveTextContent('b8f6c8a4…');
  });

  it('shows full value when full prop is true', () => {
    render(<IdChip value={fullId} full />);
    expect(screen.getByTestId('id-chip')).toHaveTextContent(fullId);
  });

  it('copies full value on click', () => {
    render(<IdChip value={fullId} />);
    fireEvent.click(screen.getByTestId('id-chip-copy'));
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(fullId);
  });

  it('shows short ids without truncation', () => {
    render(<IdChip value="abc" />);
    expect(screen.getByTestId('id-chip')).toHaveTextContent('abc');
  });
});
