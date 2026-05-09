import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SessionExpiredBanner } from './session-expired-banner';

describe('SessionExpiredBanner', () => {
  it('renders when visible', () => {
    // act
    render(<SessionExpiredBanner visible />);

    // assert
    expect(screen.getByRole('alert')).toHaveTextContent('Your session has expired.');
  });

  it('returns null when not visible', () => {
    // act
    const { container } = render(<SessionExpiredBanner visible={false} />);

    // assert
    expect(container.firstChild).toBeNull();
  });

  it('renders sign-in CTA with correct href', () => {
    // act
    render(<SessionExpiredBanner visible />);

    // assert
    const cta = screen.getByRole('link', { name: 'Sign in again' });
    expect(cta).toHaveAttribute('href', '/login?reason=session-expired');
  });

  it('has role=alert for accessibility', () => {
    // act
    render(<SessionExpiredBanner visible />);

    // assert
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });
});
