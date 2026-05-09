import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { NotFoundCard } from './not-found-card';

describe('NotFoundCard', () => {
  it('renders heading', () => {
    render(<NotFoundCard />);
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      "We can't find that transaction",
    );
  });

  it('renders non-enumerating body text', () => {
    render(<NotFoundCard />);
    expect(screen.getByTestId('not-found-card')).toHaveTextContent(
      'It may have been removed, or you may not have access',
    );
  });

  it('renders back-to-dashboard CTA', () => {
    render(<NotFoundCard />);
    const cta = screen.getByTestId('not-found-cta');
    expect(cta).toHaveAttribute('href', '/');
    expect(cta).toHaveTextContent('Back to dashboard');
  });
});
