import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('next/image', () => ({
  default: (props: Record<string, unknown>) => <img {...props} />,
}));

vi.mock('next/navigation', () => ({
  useSearchParams: vi.fn(),
}));

vi.mock('~/middleware', () => ({
  NEXT_URL_ALLOWLIST: /^\/[^/]/,
}));

vi.mock('./actions', () => ({
  login: vi.fn(),
}));

import { useSearchParams } from 'next/navigation';
import { login } from './actions';
import LoginPage from './page';

const mockSearchParams = new URLSearchParams();
const mockUseSearchParams = vi.mocked(useSearchParams);
const mockLogin = vi.mocked(login);

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUseSearchParams.mockReturnValue(mockSearchParams as ReturnType<typeof useSearchParams>);
    mockLogin.mockResolvedValue({});
  });

  it('renders the sign-in form with title and subtitle', () => {
    render(<LoginPage />);

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByText('Admin & customer access')).toBeInTheDocument();
  });

  it('renders three demo account buttons', () => {
    render(<LoginPage />);

    expect(screen.getByTestId('demo-alice')).toBeInTheDocument();
    expect(screen.getByTestId('demo-bob')).toBeInTheDocument();
    expect(screen.getByTestId('demo-admin')).toBeInTheDocument();
  });

  it('disables submit button when fields are empty', () => {
    render(<LoginPage />);

    expect(screen.getByTestId('login-submit')).toBeDisabled();
  });

  it('fills email and password when demo account is clicked', () => {
    render(<LoginPage />);

    fireEvent.click(screen.getByTestId('demo-alice'));

    expect(screen.getByTestId('login-email')).toHaveValue('alice@stablepay.io');
    expect(screen.getByTestId('login-password')).toHaveValue('demo1234');
  });

  it('enables submit button after demo account fill', () => {
    render(<LoginPage />);

    fireEvent.click(screen.getByTestId('demo-alice'));

    expect(screen.getByTestId('login-submit')).not.toBeDisabled();
  });

  it('shows email validation error on blur with invalid email', () => {
    render(<LoginPage />);

    const emailInput = screen.getByTestId('login-email');
    fireEvent.change(emailInput, { target: { value: 'notanemail' } });
    fireEvent.blur(emailInput);

    expect(screen.getByTestId('email-error')).toHaveTextContent('Enter a valid email address');
  });

  it('does not show email error for valid email after blur', () => {
    render(<LoginPage />);

    const emailInput = screen.getByTestId('login-email');
    fireEvent.change(emailInput, { target: { value: 'alice@stablepay.io' } });
    fireEvent.blur(emailInput);

    expect(screen.queryByTestId('email-error')).not.toBeInTheDocument();
  });

  it('renders session-expired reason banner', () => {
    const params = new URLSearchParams('reason=session-expired');
    mockUseSearchParams.mockReturnValue(params as ReturnType<typeof useSearchParams>);

    render(<LoginPage />);

    const banner = screen.getByTestId('reason-banner');
    expect(banner).toHaveTextContent('Your session has expired.');
  });

  it('renders signed-out-elsewhere reason banner', () => {
    const params = new URLSearchParams('reason=signed-out-elsewhere');
    mockUseSearchParams.mockReturnValue(params as ReturnType<typeof useSearchParams>);

    render(<LoginPage />);

    const banner = screen.getByTestId('reason-banner');
    expect(banner).toHaveTextContent('You signed out in another tab.');
  });

  it('does not render reason banner for unknown reason', () => {
    const params = new URLSearchParams('reason=unknown');
    mockUseSearchParams.mockReturnValue(params as ReturnType<typeof useSearchParams>);

    render(<LoginPage />);

    expect(screen.queryByTestId('reason-banner')).not.toBeInTheDocument();
  });

  it('shows login error message on failed login', async () => {
    mockLogin.mockResolvedValue({ error: 'invalid_credentials' });

    render(<LoginPage />);
    fireEvent.click(screen.getByTestId('demo-alice'));
    fireEvent.submit(screen.getByTestId('login-submit'));

    const errorEl = await screen.findByTestId('login-error');
    expect(errorEl).toHaveTextContent('Email or password incorrect.');
  });

  it('shows account locked error message', async () => {
    mockLogin.mockResolvedValue({ error: 'account_locked' });

    render(<LoginPage />);
    fireEvent.click(screen.getByTestId('demo-alice'));
    fireEvent.submit(screen.getByTestId('login-submit'));

    const errorEl = await screen.findByTestId('login-error');
    expect(errorEl).toHaveTextContent('Account temporarily locked. Try again in 15 minutes.');
  });

  it('shows rate limited error message', async () => {
    mockLogin.mockResolvedValue({ error: 'rate_limited' });

    render(<LoginPage />);
    fireEvent.click(screen.getByTestId('demo-alice'));
    fireEvent.submit(screen.getByTestId('login-submit'));

    const errorEl = await screen.findByTestId('login-error');
    expect(errorEl).toHaveTextContent('Too many attempts. Try again in 60 seconds.');
  });

  it('shows server error message', async () => {
    mockLogin.mockResolvedValue({ error: 'server_error' });

    render(<LoginPage />);
    fireEvent.click(screen.getByTestId('demo-alice'));
    fireEvent.submit(screen.getByTestId('login-submit'));

    const errorEl = await screen.findByTestId('login-error');
    expect(errorEl).toHaveTextContent("We couldn't sign you in. Please try again.");
  });

  it('calls login with correct arguments', async () => {
    render(<LoginPage />);
    fireEvent.click(screen.getByTestId('demo-alice'));
    fireEvent.submit(screen.getByTestId('login-submit'));

    await vi.waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('alice@stablepay.io', 'demo1234', '/');
    });
  });

  it('passes validated next param as redirectTo', async () => {
    const params = new URLSearchParams('next=/transactions');
    mockUseSearchParams.mockReturnValue(params as ReturnType<typeof useSearchParams>);

    render(<LoginPage />);
    fireEvent.click(screen.getByTestId('demo-alice'));
    fireEvent.submit(screen.getByTestId('login-submit'));

    await vi.waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('alice@stablepay.io', 'demo1234', '/transactions');
    });
  });

  it('rejects scheme-relative next param', async () => {
    const params = new URLSearchParams('next=//evil.com');
    mockUseSearchParams.mockReturnValue(params as ReturnType<typeof useSearchParams>);

    render(<LoginPage />);
    fireEvent.click(screen.getByTestId('demo-alice'));
    fireEvent.submit(screen.getByTestId('login-submit'));

    await vi.waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('alice@stablepay.io', 'demo1234', '/');
    });
  });

  it('renders logo image', () => {
    render(<LoginPage />);

    const logo = screen.getByAltText('StablePay');
    expect(logo).toBeInTheDocument();
    expect(logo).toHaveAttribute('src', '/logo-mark.svg');
  });

  it('renders stablepay wordmark', () => {
    render(<LoginPage />);

    expect(screen.getByText('Payment Pipeline Dashboard')).toBeInTheDocument();
  });
});
