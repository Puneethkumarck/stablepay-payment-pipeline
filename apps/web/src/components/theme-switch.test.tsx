import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeSwitch } from './theme-switch';

describe('ThemeSwitch', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('renders sun and moon icons', () => {
    // act
    const { container } = render(<ThemeSwitch />);

    // assert
    const svgs = container.querySelectorAll('svg');
    expect(svgs.length).toBe(2);
  });

  it('defaults to dark theme', () => {
    // act
    render(<ThemeSwitch />);

    // assert
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('respects stored preference', () => {
    // arrange
    localStorage.setItem('sp4_theme', 'light');

    // act
    render(<ThemeSwitch />);

    // assert
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('persists theme change to localStorage and updates DOM', async () => {
    // arrange
    const user = userEvent.setup();
    render(<ThemeSwitch />);

    // act
    await user.click(screen.getByRole('switch'));

    // assert
    expect(localStorage.getItem('sp4_theme')).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('respects prefers-color-scheme when no stored preference', () => {
    // arrange
    const matchMediaSpy = vi.spyOn(window, 'matchMedia').mockImplementation(
      (query) =>
        ({
          matches: query === '(prefers-color-scheme: light)',
          media: query,
          addEventListener: vi.fn(),
          removeEventListener: vi.fn(),
          addListener: vi.fn(),
          removeListener: vi.fn(),
          onchange: null,
          dispatchEvent: vi.fn(),
        }) as MediaQueryList,
    );

    // act
    render(<ThemeSwitch />);

    // assert
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    matchMediaSpy.mockRestore();
  });
});
