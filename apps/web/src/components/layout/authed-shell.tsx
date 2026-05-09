'use client';

import { signOut } from 'next-auth/react';
import type { Route } from 'next';
import { usePathname, useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { LiveFeed } from '~/components/live-feed';
import { Sidebar } from './sidebar';

const LIVEFEED_STORAGE_KEY = 'sp4_livefeed_visible';

function deriveActivePage(pathname: string): string {
  if (pathname === '/') return 'dashboard';
  if (pathname.startsWith('/transactions')) return 'transactions';
  if (pathname.startsWith('/flows')) return 'flows';
  if (pathname.startsWith('/customers')) return 'customers';
  if (pathname.startsWith('/admin/dlq')) return 'dlq';
  if (pathname.startsWith('/admin/stuck')) return 'stuck';
  return 'dashboard';
}

interface AuthedShellProps {
  email: string;
  role: string;
  customerId?: string;
  isAdmin: boolean;
  children: React.ReactNode;
}

export function AuthedShell({
  email,
  role,
  customerId,
  isAdmin,
  children,
}: AuthedShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const activePage = deriveActivePage(pathname);

  const [liveFeedVisible, setLiveFeedVisible] = useState(true);

  useEffect(() => {
    setLiveFeedVisible(localStorage.getItem(LIVEFEED_STORAGE_KEY) !== 'false');
  }, []);

  const handleNavigate = useCallback(
    (_key: string, href: string) => {
      router.push(href as Route);
    },
    [router],
  );

  const handleSignOut = useCallback(() => {
    void signOut({ redirectTo: '/login' });
  }, []);

  const handleHideLiveFeed = useCallback(() => {
    setLiveFeedVisible(false);
    localStorage.setItem(LIVEFEED_STORAGE_KEY, 'false');
  }, []);

  return (
    <div className="flex h-screen overflow-hidden bg-surface-1">
      <Sidebar
        activePage={activePage}
        onNavigate={handleNavigate}
        email={email}
        role={role}
        customerId={customerId}
        onSignOut={handleSignOut}
      />
      <main className="min-w-0 flex-1 overflow-y-auto">{children}</main>
      <LiveFeed
        visible={liveFeedVisible}
        onHide={handleHideLiveFeed}
        isAdmin={isAdmin}
        userEmail={email}
      />
    </div>
  );
}
