import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { ThemeSwitch } from './theme-switch';

describe('ThemeSwitch', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('renders sun and moon icons', () => {
    render(<ThemeSwitch />);
    const container = screen.getByTestId('theme-switch');
    const svgs = container.querySelectorAll('svg');
    expect(svgs.length).toBe(2);
  });

  it('defaults to dark theme', () => {
    render(<ThemeSwitch />);
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('respects stored preference', () => {
    localStorage.setItem('sp4_theme', 'light');
    render(<ThemeSwitch />);
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });

  it('persists theme change to localStorage', () => {
    render(<ThemeSwitch />);
    const switchEl = screen.getByRole('switch');
    fireEvent.click(switchEl);
    expect(localStorage.getItem('sp4_theme')).toBe('light');
  });
});
