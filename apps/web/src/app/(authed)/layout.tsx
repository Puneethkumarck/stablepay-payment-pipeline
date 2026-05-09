import { redirect } from 'next/navigation';
import { AuthedShell } from '~/components/layout/authed-shell';
import { LiveFeedConnector } from '~/components/layout/live-feed-connector';
import { MultiTabSessionGuard } from '~/components/multi-tab-session-guard';
import { QueryProvider } from '~/lib/query-client';
import { auth } from '~/server/auth';

export default async function AuthedLayout({ children }: { children: React.ReactNode }) {
  const session = await auth();

  if (!session?.user) {
    redirect('/login');
  }

  const roles = session.user.roles ?? [];
  const role = roles.includes('ROLE_ADMIN') ? 'Admin' : 'Customer';
  const isAdmin = roles.includes('ROLE_ADMIN');

  return (
    <QueryProvider>
      <MultiTabSessionGuard />
      <LiveFeedConnector accessToken={session.accessToken} />
      <AuthedShell
        email={session.user.email ?? ''}
        role={role}
        customerId={session.user.customerId}
        isAdmin={isAdmin}
      >
        {children}
      </AuthedShell>
    </QueryProvider>
  );
}
