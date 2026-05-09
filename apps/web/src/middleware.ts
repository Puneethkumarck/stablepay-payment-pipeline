import { NextResponse } from 'next/server';
import { auth } from '~/server/auth';

// Rejects scheme-relative //evil.com and bare / to prevent open-redirect
export const NEXT_URL_ALLOWLIST = /^\/[^/]/;

export default auth((req) => {
  const session = req.auth;
  const path = req.nextUrl.pathname;

  if (session?.error === 'RefreshAccessTokenError') {
    const url = req.nextUrl.clone();
    url.pathname = '/login';
    url.searchParams.set('reason', 'session-expired');
    if (path !== '/' && NEXT_URL_ALLOWLIST.test(path)) {
      url.searchParams.set('next', path + req.nextUrl.search);
    }
    return NextResponse.redirect(url);
  }

  if (!session?.user) {
    const url = req.nextUrl.clone();
    url.pathname = '/login';
    if (path !== '/' && NEXT_URL_ALLOWLIST.test(path)) {
      url.searchParams.set('next', path + req.nextUrl.search);
    }
    return NextResponse.redirect(url);
  }

  const roles = session.user.roles;
  const isAdminRoute = path === '/admin' || path.startsWith('/admin/');
  const isAgentRoute = path === '/agent' || path.startsWith('/agent/');
  if (isAdminRoute && !roles?.includes('ROLE_ADMIN')) {
    return NextResponse.rewrite(new URL('/not-found', req.url));
  }
  if (isAgentRoute && !roles?.includes('ROLE_AGENT')) {
    return NextResponse.rewrite(new URL('/not-found', req.url));
  }

  return NextResponse.next();
});

export const config = {
  matcher: ['/((?!login|api/auth|api/health|_next/static|_next/image|favicon|logo-).*)'],
};
