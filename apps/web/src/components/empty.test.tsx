import { render, screen } from '@testing-library/react';
import { AlertTriangle } from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { Empty } from './empty';

describe('Empty', () => {
  it('renders title', () => {
    render(<Empty title="No results" />);
    expect(screen.getByTestId('empty')).toHaveTextContent('No results');
  });

  it('renders subtitle when provided', () => {
    render(<Empty title="No results" sub="Try a different search" />);
    expect(screen.getByTestId('empty')).toHaveTextContent('Try a different search');
  });

  it('does not render subtitle element when sub is not provided', () => {
    render(<Empty title="No results" />);
    const container = screen.getByTestId('empty');
    const children = container.querySelectorAll('div');
    expect(children.length).toBe(1);
  });

  it('renders with custom icon', () => {
    render(<Empty icon={AlertTriangle} title="Error" />);
    expect(screen.getByTestId('empty')).toBeInTheDocument();
  });

  it('renders default database icon when no icon provided', () => {
    const { container } = render(<Empty title="Empty" />);
    const svg = container.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });
});
