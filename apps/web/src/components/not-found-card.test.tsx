import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { NotFoundCard } from './not-found-card';

describe('NotFoundCard', () => {
  it('renders heading', () => {
    // act
    render(<NotFoundCard />);

    // assert
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      "We can't find that transaction",
    );
  });

  it('renders non-enumerating body text', () => {
    // act
    render(<NotFoundCard />);

    // assert
    expect(
      screen.getByText(/It may have been removed, or you may not have access/),
    ).toBeInTheDocument();
  });

  it('renders back-to-dashboard CTA', () => {
    // act
    render(<NotFoundCard />);

    // assert
    const cta = screen.getByRole('link', { name: 'Back to dashboard' });
    expect(cta).toHaveAttribute('href', '/');
  });
});
