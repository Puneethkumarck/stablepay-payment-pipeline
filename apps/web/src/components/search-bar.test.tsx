import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { SearchBar } from './search-bar';

describe('SearchBar', () => {
  it('renders with placeholder', () => {
    render(<SearchBar onChange={() => {}} placeholder="Search ref…" />);
    expect(screen.getByPlaceholderText('Search ref…')).toBeInTheDocument();
  });

  it('renders with default placeholder', () => {
    render(<SearchBar onChange={() => {}} />);
    expect(screen.getByPlaceholderText('Search…')).toBeInTheDocument();
  });

  it('calls onChange after debounce', async () => {
    vi.useFakeTimers();
    const onChange = vi.fn();
    render(<SearchBar onChange={onChange} debounceMs={300} />);

    fireEvent.change(screen.getByPlaceholderText('Search…'), {
      target: { value: 'test' },
    });

    expect(onChange).not.toHaveBeenCalled();
    vi.advanceTimersByTime(300);
    expect(onChange).toHaveBeenCalledWith('test');

    vi.useRealTimers();
  });

  it('debounces rapid input', () => {
    vi.useFakeTimers();
    const onChange = vi.fn();
    render(<SearchBar onChange={onChange} debounceMs={300} />);

    const input = screen.getByPlaceholderText('Search…');
    fireEvent.change(input, { target: { value: 'a' } });
    fireEvent.change(input, { target: { value: 'ab' } });
    fireEvent.change(input, { target: { value: 'abc' } });

    vi.advanceTimersByTime(300);
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith('abc');

    vi.useRealTimers();
  });

  it('has search icon', () => {
    render(<SearchBar onChange={() => {}} />);
    const container = screen.getByTestId('search-bar');
    expect(container.querySelector('svg')).toBeInTheDocument();
  });
});
