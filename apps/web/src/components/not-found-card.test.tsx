import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { NotFoundCard } from './not-found-card';

describe('NotFoundCard', () => {
  it('renders default heading', () => {
    // act
    render(<NotFoundCard />);

    // assert
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      "We can't find that transaction",
    );
  });

  it('renders default non-enumerating body text', () => {
    // act
    render(<NotFoundCard />);

    // assert
    expect(
      screen.getByText(/It may have been removed, or you may not have access/),
    ).toBeInTheDocument();
  });

  it('renders default back-to-dashboard CTA', () => {
    // act
    render(<NotFoundCard />);

    // assert
    const cta = screen.getByRole('link', { name: 'Back to dashboard' });
    expect(cta).toHaveAttribute('href', '/');
  });

  it('renders custom title and body', () => {
    // act
    render(
      <NotFoundCard
        title="We can't find that customer"
        body="The customer may not exist, or you may not have access."
      />,
    );

    // assert
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent(
      "We can't find that customer",
    );
    expect(
      screen.getByText('The customer may not exist, or you may not have access.'),
    ).toBeInTheDocument();
  });

  it('renders custom CTA', () => {
    // act
    render(<NotFoundCard primaryCta={{ label: 'Go home', href: '/home' }} />);

    // assert
    const cta = screen.getByRole('link', { name: 'Go home' });
    expect(cta).toHaveAttribute('href', '/home');
  });
});
