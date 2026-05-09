import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { IdChip } from './id-chip';

const writeTextSpy = vi.fn().mockResolvedValue(undefined);
const originalDescriptor = Object.getOwnPropertyDescriptor(Navigator.prototype, 'clipboard');

describe('IdChip', () => {
  const fullId = 'b8f6c8a4-7d2e-4f3b-9c1e-2a5d6f8e0b1d';

  beforeEach(() => {
    writeTextSpy.mockClear();
    Object.defineProperty(Navigator.prototype, 'clipboard', {
      get: () => ({ writeText: writeTextSpy }),
      configurable: true,
    });
  });

  afterEach(() => {
    if (originalDescriptor) {
      Object.defineProperty(Navigator.prototype, 'clipboard', originalDescriptor);
    }
  });

  it('truncates to 8 chars by default', () => {
    // act
    render(<IdChip value={fullId} />);

    // assert
    expect(screen.getByText('b8f6c8a4…')).toBeInTheDocument();
  });

  it('shows full value when full prop is true', () => {
    // act
    render(<IdChip value={fullId} full />);

    // assert
    expect(screen.getByText(fullId)).toBeInTheDocument();
  });

  it('copies full value on click', () => {
    // act
    render(<IdChip value={fullId} />);
    fireEvent.click(screen.getByTestId('id-chip-copy'));

    // assert
    expect(writeTextSpy).toHaveBeenCalledWith(fullId);
  });

  it('shows short ids without truncation', () => {
    // act
    render(<IdChip value="abc" />);

    // assert
    expect(screen.getByText('abc')).toBeInTheDocument();
  });
});
