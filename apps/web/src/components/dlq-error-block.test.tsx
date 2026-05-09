import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DlqErrorBlock } from './dlq-error-block';

const writeTextSpy = vi.fn().mockResolvedValue(undefined);
const originalDescriptor = Object.getOwnPropertyDescriptor(Navigator.prototype, 'clipboard');

describe('DlqErrorBlock', () => {
  const errorMessage = 'NullPointerException at com.example.Foo.bar(Foo.java:42)';

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

  it('renders the error message in a pre block', () => {
    // act
    render(<DlqErrorBlock message={errorMessage} />);

    // assert
    const pre = screen.getByTestId('dlq-error-block').querySelector('pre');
    expect(pre).toHaveTextContent(errorMessage);
  });

  it('copies the full message on copy button click', () => {
    // act
    render(<DlqErrorBlock message={errorMessage} />);
    fireEvent.click(screen.getByTestId('dlq-error-copy'));

    // assert
    expect(writeTextSpy).toHaveBeenCalledWith(errorMessage);
  });

  it('shows copied feedback after clicking copy', async () => {
    // arrange
    const user = userEvent.setup();
    render(<DlqErrorBlock message={errorMessage} />);

    // act
    await user.click(screen.getByTestId('dlq-error-copy'));

    // assert
    expect(screen.getByText('Copied')).toBeInTheDocument();
  });

  it('does not crash or show copied when clipboard is unavailable', () => {
    // arrange
    Object.defineProperty(Navigator.prototype, 'clipboard', {
      get: () => undefined,
      configurable: true,
    });
    render(<DlqErrorBlock message={errorMessage} />);

    // act
    fireEvent.click(screen.getByTestId('dlq-error-copy'));

    // assert
    expect(screen.queryByText('Copied')).toBeNull();
    expect(screen.getByText('Copy')).toBeInTheDocument();
  });
});
