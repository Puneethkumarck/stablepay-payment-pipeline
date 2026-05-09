import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { KVRow } from './kv-row';

describe('KVRow', () => {
  it('renders label and value', () => {
    render(<KVRow label="Status" value="Active" />);
    const row = screen.getByTestId('kv-row');
    expect(row).toHaveTextContent('Status');
    expect(row).toHaveTextContent('Active');
  });

  it('renders a bottom border by default', () => {
    render(<KVRow label="Key" value="Value" />);
    expect(screen.getByTestId('kv-row').className).toContain('border-b');
  });

  it('omits bottom border when last is true', () => {
    render(<KVRow label="Key" value="Value" last />);
    expect(screen.getByTestId('kv-row').className).not.toContain('border-b');
  });

  it('uses mono font by default', () => {
    render(<KVRow label="Key" value="Value" />);
    const valueEl = screen.getByTestId('kv-row').querySelector('span:last-child');
    expect(valueEl?.className).toContain('font-mono');
  });

  it('uses sans font when mono is false', () => {
    render(<KVRow label="Key" value="Value" mono={false} />);
    const valueEl = screen.getByTestId('kv-row').querySelector('span:last-child');
    expect(valueEl?.className).toContain('font-sans');
  });
});
