import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SessionExpiredBanner } from './session-expired-banner';

describe('SessionExpiredBanner', () => {
  it('renders when visible', () => {
    render(<SessionExpiredBanner visible />);
    expect(screen.getByTestId('session-expired-banner')).toHaveTextContent(
      'Your session has expired.',
    );
  });

  it('returns null when not visible', () => {
    const { container } = render(<SessionExpiredBanner visible={false} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders sign-in CTA with correct href', () => {
    render(<SessionExpiredBanner visible />);
    const cta = screen.getByTestId('session-expired-cta');
    expect(cta).toHaveAttribute('href', '/login?reason=session-expired');
    expect(cta).toHaveTextContent('Sign in again');
  });

  it('has role=alert for accessibility', () => {
    render(<SessionExpiredBanner visible />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });
});
