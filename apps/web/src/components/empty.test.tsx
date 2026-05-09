import { render, screen } from '@testing-library/react';
import { AlertTriangle } from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { Empty } from './empty';

describe('Empty', () => {
  it('renders title', () => {
    // act
    render(<Empty title="No results" />);

    // assert
    expect(screen.getByText('No results')).toBeInTheDocument();
  });

  it('renders subtitle when provided', () => {
    // act
    render(<Empty title="No results" sub="Try a different search" />);

    // assert
    expect(screen.getByText('Try a different search')).toBeInTheDocument();
  });

  it('does not render subtitle when sub is not provided', () => {
    // act
    render(<Empty title="No results" />);

    // assert
    expect(screen.queryByText('Try a different search')).toBeNull();
  });

  it('renders with custom icon', () => {
    // act
    render(<Empty icon={AlertTriangle} title="Error" />);

    // assert
    expect(screen.getByText('Error')).toBeInTheDocument();
  });

  it('renders default database icon when no icon provided', () => {
    // act
    const { container } = render(<Empty title="Empty" />);

    // assert
    expect(container.querySelector('svg')).toBeInTheDocument();
  });
});
