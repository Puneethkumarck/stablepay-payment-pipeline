import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('next-auth/react', () => ({
  signOut: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({ push: vi.fn() }),
  usePathname: () => '/',
}));

vi.mock('~/components/layout/sidebar', () => ({
  Sidebar: (props: Record<string, unknown>) => (
    <nav data-testid="sidebar" data-email={props.email} data-role={props.role} />
  ),
}));

vi.mock('~/components/live-feed', () => ({
  LiveFeed: (props: Record<string, unknown>) => (
    <aside data-testid="live-feed" data-visible={String(props.visible)} />
  ),
}));

import { AuthedShell } from './authed-shell';

describe('AuthedShell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('renders sidebar, main content, and live feed', () => {
    // act
    render(
      <AuthedShell email="alice@stablepay.io" role="Admin" isAdmin>
        <div data-testid="page-content">Dashboard</div>
      </AuthedShell>,
    );

    // assert
    expect(screen.getByTestId('sidebar')).toBeInTheDocument();
    expect(screen.getByTestId('page-content')).toBeInTheDocument();
    expect(screen.getByTestId('live-feed')).toBeInTheDocument();
  });

  it('passes email and role to sidebar', () => {
    // act
    render(
      <AuthedShell email="alice@stablepay.io" role="Admin" isAdmin>
        <div>content</div>
      </AuthedShell>,
    );

    // assert
    const sidebar = screen.getByTestId('sidebar');
    expect(sidebar).toHaveAttribute('data-email', 'alice@stablepay.io');
    expect(sidebar).toHaveAttribute('data-role', 'Admin');
  });

  it('hides live feed when localStorage is set to false', async () => {
    // arrange
    localStorage.setItem('sp4_livefeed_visible', 'false');

    // act
    render(
      <AuthedShell email="alice@stablepay.io" role="Admin" isAdmin>
        <div>content</div>
      </AuthedShell>,
    );

    // assert
    await waitFor(() => {
      expect(screen.getByTestId('live-feed')).toHaveAttribute('data-visible', 'false');
    });
  });

  it('shows live feed by default', () => {
    // act
    render(
      <AuthedShell email="alice@stablepay.io" role="Admin" isAdmin>
        <div>content</div>
      </AuthedShell>,
    );

    // assert
    expect(screen.getByTestId('live-feed')).toHaveAttribute('data-visible', 'true');
  });
});
