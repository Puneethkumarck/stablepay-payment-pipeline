import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { KVRow } from './kv-row';

describe('KVRow', () => {
  it('renders label and value', () => {
    // act
    render(<KVRow label="Status" value="Active" />);

    // assert
    expect(screen.getByText('Status')).toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('does not mark as last row by default', () => {
    // act
    render(<KVRow label="Key" value="Value" />);

    // assert
    expect(screen.getByTestId('kv-row')).not.toHaveAttribute('data-last');
  });

  it('marks as last row when last is true', () => {
    // act
    render(<KVRow label="Key" value="Value" last />);

    // assert
    expect(screen.getByTestId('kv-row')).toHaveAttribute('data-last', 'true');
  });

  it('uses mono font by default', () => {
    // act
    render(<KVRow label="Key" value="Value" />);

    // assert
    expect(screen.getByTestId('kv-row')).toHaveAttribute('data-mono', 'true');
  });

  it('uses sans font when mono is false', () => {
    // act
    render(<KVRow label="Key" value="Value" mono={false} />);

    // assert
    expect(screen.getByTestId('kv-row')).toHaveAttribute('data-mono', 'false');
  });
});
