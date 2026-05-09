import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DlqErrorBlock } from './dlq-error-block';

describe('DlqErrorBlock', () => {
  const errorMessage = 'NullPointerException at com.example.Foo.bar(Foo.java:42)';

  beforeEach(() => {
    Object.assign(navigator, {
      clipboard: { writeText: vi.fn().mockResolvedValue(undefined) },
    });
  });

  it('renders the error message in a pre block', () => {
    render(<DlqErrorBlock message={errorMessage} />);
    const pre = screen.getByTestId('dlq-error-block').querySelector('pre');
    expect(pre).toHaveTextContent(errorMessage);
  });

  it('copies the full message on copy button click', () => {
    render(<DlqErrorBlock message={errorMessage} />);
    fireEvent.click(screen.getByTestId('dlq-error-copy'));
    expect(navigator.clipboard.writeText).toHaveBeenCalledWith(errorMessage);
  });

  it('renders with max-height constraint', () => {
    render(<DlqErrorBlock message={errorMessage} />);
    const pre = screen.getByTestId('dlq-error-block').querySelector('pre');
    expect(pre?.className).toContain('max-h-[240px]');
  });

  it('preserves whitespace and wraps words', () => {
    render(<DlqErrorBlock message={errorMessage} />);
    const pre = screen.getByTestId('dlq-error-block').querySelector('pre');
    expect(pre?.className).toContain('whitespace-pre-wrap');
    expect(pre?.className).toContain('break-words');
  });
});
