import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SearchBar } from './search-bar';

describe('SearchBar', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders with placeholder', () => {
    // act
    render(<SearchBar onChange={() => {}} placeholder="Search ref…" />);

    // assert
    expect(screen.getByPlaceholderText('Search ref…')).toBeInTheDocument();
  });

  it('renders with default placeholder', () => {
    // act
    render(<SearchBar onChange={() => {}} />);

    // assert
    expect(screen.getByPlaceholderText('Search…')).toBeInTheDocument();
  });

  it('calls onChange after debounce', () => {
    // arrange
    vi.useFakeTimers();
    const onChange = vi.fn();
    render(<SearchBar onChange={onChange} debounceMs={300} />);

    // act
    fireEvent.change(screen.getByPlaceholderText('Search…'), { target: { value: 'test' } });

    // assert
    expect(onChange).not.toHaveBeenCalled();
    vi.advanceTimersByTime(300);
    expect(onChange).toHaveBeenCalledWith('test');
  });

  it('debounces rapid input', () => {
    // arrange
    vi.useFakeTimers();
    const onChange = vi.fn();
    render(<SearchBar onChange={onChange} debounceMs={300} />);

    // act
    fireEvent.change(screen.getByPlaceholderText('Search…'), { target: { value: 'a' } });
    vi.advanceTimersByTime(100);
    fireEvent.change(screen.getByPlaceholderText('Search…'), { target: { value: 'ab' } });
    vi.advanceTimersByTime(100);
    fireEvent.change(screen.getByPlaceholderText('Search…'), { target: { value: 'abc' } });

    // assert
    vi.advanceTimersByTime(300);
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith('abc');
  });

  it('has search icon', () => {
    // act
    const { container } = render(<SearchBar onChange={() => {}} />);

    // assert
    expect(container.querySelector('svg')).toBeInTheDocument();
  });
});
