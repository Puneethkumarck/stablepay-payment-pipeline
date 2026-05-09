'use client';

import Image from 'next/image';
import { useSearchParams } from 'next/navigation';
import { type FormEvent, useCallback, useState } from 'react';
import { NEXT_URL_ALLOWLIST } from '~/middleware';
import { type LoginError, login } from './actions';

const DEMO_ACCOUNTS = [
  { email: 'alice@stablepay.io', role: 'Admin · Customer' },
  { email: 'bob@stablepay.io', role: 'Customer only' },
  { email: 'admin@stablepay.io', role: 'Admin only' },
] as const;

const ERROR_MESSAGES: Record<LoginError, string> = {
  invalid_credentials: 'Email or password incorrect.',
  account_locked: 'Account temporarily locked. Try again in 15 minutes.',
  rate_limited: 'Too many attempts. Try again in 60 seconds.',
  server_error: "We couldn't sign you in. Please try again.",
};

const REASON_BANNERS: Record<string, string> = {
  'session-expired': 'Your session has expired.',
  'signed-out-elsewhere': 'You signed out in another tab.',
};

export default function LoginPage() {
  const searchParams = useSearchParams();
  const reason = searchParams.get('reason');
  const next = searchParams.get('next');

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<LoginError | null>(null);
  const [emailTouched, setEmailTouched] = useState(false);

  const emailInvalid = emailTouched && email !== '' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  const submitDisabled = loading || email === '' || password === '';

  const redirectTo = next && NEXT_URL_ALLOWLIST.test(next) ? next : '/';

  const handleSubmit = useCallback(
    async (e: FormEvent) => {
      e.preventDefault();
      setError(null);
      setLoading(true);
      try {
        const result = await login(email, password, redirectTo);
        if (result.error) {
          setError(result.error);
        }
      } catch (err: unknown) {
        const isRedirect =
          typeof err === 'object' &&
          err !== null &&
          'digest' in err &&
          typeof (err as Record<string, unknown>).digest === 'string' &&
          ((err as Record<string, unknown>).digest as string).startsWith('NEXT_REDIRECT');
        if (isRedirect) throw err;
        setError('server_error');
      } finally {
        setLoading(false);
      }
    },
    [email, password, redirectTo],
  );

  const fillDemo = useCallback((demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('demo1234');
    setError(null);
    setEmailTouched(false);
  }, []);

  const bannerText = reason ? REASON_BANNERS[reason] : null;

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-surface-0">
      {/* Background orbs */}
      <div
        className="pointer-events-none absolute -left-[120px] -top-[80px] size-[500px] rounded-full"
        style={{
          background: 'radial-gradient(circle, rgba(153,69,255,0.16), transparent 70%)',
          filter: 'blur(80px)',
        }}
      />
      <div
        className="pointer-events-none absolute -right-[60px] bottom-[40px] size-[350px] rounded-full"
        style={{
          background: 'radial-gradient(circle, rgba(0,255,163,0.10), transparent 70%)',
          filter: 'blur(60px)',
        }}
      />

      <div className="relative z-[2] w-[380px]">
        {/* Logo + wordmark */}
        <div className="mb-9 text-center">
          <div className="mx-auto mb-[14px] grid size-[48px] place-items-center rounded-[13px] bg-gradient-to-br from-solana-teal via-solana-purple to-solana-magenta shadow-[var(--glow-solana)]">
            <Image
              src="/logo-mark.svg"
              alt="StablePay"
              width={28}
              height={28}
              priority
              className="brightness-0 invert"
            />
          </div>
          <div className="text-[20px] font-bold tracking-[-0.02em] text-fg-1">
            stable<span className="sp-gradient-text">pay</span>
          </div>
          <div className="mt-[5px] text-[12px] text-fg-3">Payment Pipeline Dashboard</div>
        </div>

        {/* Reason banner */}
        {bannerText && (
          <div
            data-testid="reason-banner"
            role="alert"
            className="mb-4 flex items-center justify-center rounded-lg border border-[rgba(245,158,11,0.24)] bg-[rgba(245,158,11,0.10)] px-4 py-3 text-[13px] font-medium text-[#FCD34D]"
          >
            {bannerText}
          </div>
        )}

        {/* Form card */}
        <div className="rounded-[16px] border border-border-1 bg-surface-2 p-7">
          <h2 className="mb-[3px] text-[17px] font-bold text-fg-1">Sign in</h2>
          <p className="mb-6 text-[12px] text-fg-3">Admin & customer access</p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-[14px]">
            <div>
              <label
                htmlFor="login-email"
                className="mb-[5px] block text-[11px] font-semibold uppercase tracking-[0.05em] text-fg-3"
              >
                Email
              </label>
              <input
                id="login-email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                onBlur={() => setEmailTouched(true)}
                aria-invalid={emailInvalid || undefined}
                data-testid="login-email"
                className="w-full rounded-[9px] border border-[rgba(255,255,255,0.10)] bg-[rgba(255,255,255,0.05)] px-3 py-[9px] text-[13px] text-fg-1 outline-none transition-colors focus:border-[rgba(153,69,255,0.55)] focus:ring-2 focus:ring-[rgba(153,69,255,0.25)] aria-[invalid]:border-danger"
              />
              {emailInvalid && (
                <p data-testid="email-error" className="mt-1 text-[11px] text-danger">
                  Enter a valid email address
                </p>
              )}
            </div>

            <div>
              <label
                htmlFor="login-password"
                className="mb-[5px] block text-[11px] font-semibold uppercase tracking-[0.05em] text-fg-3"
              >
                Password
              </label>
              <input
                id="login-password"
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                data-testid="login-password"
                className="w-full rounded-[9px] border border-[rgba(255,255,255,0.10)] bg-[rgba(255,255,255,0.05)] px-3 py-[9px] text-[13px] text-fg-1 outline-none transition-colors focus:border-[rgba(153,69,255,0.55)] focus:ring-2 focus:ring-[rgba(153,69,255,0.25)]"
              />
            </div>

            {error && (
              <div
                data-testid="login-error"
                role="alert"
                className="rounded-lg border border-[rgba(239,68,68,0.22)] bg-[rgba(239,68,68,0.09)] px-[11px] py-[7px] text-[12px] text-[#FCA5A5]"
              >
                {ERROR_MESSAGES[error]}
              </div>
            )}

            <button
              type="submit"
              disabled={submitDisabled}
              data-testid="login-submit"
              className="mt-[2px] cursor-pointer rounded-[10px] bg-gradient-to-r from-solana-purple to-solana-magenta px-4 py-[11px] text-[14px] font-bold text-white shadow-[0_0_20px_rgba(153,69,255,0.28)] transition-opacity disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          {/* Demo accounts */}
          <div className="mt-5">
            <div className="mb-2 text-[10px] font-semibold uppercase tracking-[0.10em] text-fg-3">
              Demo accounts
            </div>
            <div className="flex flex-col gap-1">
              {DEMO_ACCOUNTS.map(({ email: demoEmail, role }) => (
                <button
                  key={demoEmail}
                  type="button"
                  onClick={() => fillDemo(demoEmail)}
                  data-testid={`demo-${demoEmail.split('@')[0]}`}
                  className="flex cursor-pointer items-center justify-between rounded-lg border border-[rgba(255,255,255,0.06)] bg-[rgba(255,255,255,0.03)] px-[10px] py-[7px] transition-colors duration-[120ms] hover:border-[rgba(255,255,255,0.14)]"
                >
                  <span className="font-mono text-[11px] text-fg-2">{demoEmail}</span>
                  <span className="text-[10px] text-fg-3">{role}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
