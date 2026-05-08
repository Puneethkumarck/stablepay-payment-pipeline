import type { JWT } from '@auth/core/jwt';
import type { Session, User } from 'next-auth';
import NextAuth from 'next-auth';
import Credentials from 'next-auth/providers/credentials';
import '~/types/auth';

const AUTH_API_URL = process.env.AUTH_API_URL ?? 'http://localhost:8081';

interface AuthLoginResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
}

interface JwtPayload {
  sub: string;
  email: string;
  roles: string[];
  customer_id: string;
  iat: number;
  exp: number;
}

function decodeJwtPayload(token: string): JwtPayload {
  const base64Payload = token.split('.')[1];
  if (!base64Payload) {
    throw new Error('Invalid JWT: missing payload segment');
  }
  return JSON.parse(atob(base64Payload)) as JwtPayload;
}

async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const response = await fetch(`${AUTH_API_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: token.refreshToken }),
    });

    if (!response.ok) {
      return { ...token, error: 'RefreshAccessTokenError' };
    }

    const data = (await response.json()) as AuthLoginResponse;
    const payload = decodeJwtPayload(data.access_token);

    return {
      ...token,
      accessToken: data.access_token,
      refreshToken: data.refresh_token,
      accessTokenExpiresAt: Date.now() + data.expires_in * 1000,
      roles: payload.roles,
      email: payload.email,
      customerId: payload.customer_id,
      error: undefined,
    };
  } catch {
    return { ...token, error: 'RefreshAccessTokenError' };
  }
}

export const { handlers, signIn, signOut, auth } = NextAuth({
  session: { strategy: 'jwt', maxAge: 7 * 24 * 60 * 60 },
  cookies: {
    sessionToken: {
      name: 'stablepay.session',
      options: {
        httpOnly: true,
        secure: true,
        sameSite: 'lax',
        path: '/',
      },
    },
  },
  pages: {
    signIn: '/login',
  },
  providers: [
    Credentials({
      credentials: {
        email: { label: 'Email', type: 'email' },
        password: { label: 'Password', type: 'password' },
      },
      async authorize(credentials) {
        try {
          const response = await fetch(`${AUTH_API_URL}/api/v1/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              email: credentials.email,
              password: credentials.password,
            }),
          });

          if (!response.ok) {
            return null;
          }

          const data = (await response.json()) as AuthLoginResponse;
          const payload = decodeJwtPayload(data.access_token);

          return {
            id: payload.sub,
            email: payload.email,
            roles: payload.roles,
            customerId: payload.customer_id,
            accessToken: data.access_token,
            refreshToken: data.refresh_token,
            accessTokenExpiresAt: Date.now() + data.expires_in * 1000,
          } satisfies User;
        } catch {
          return null;
        }
      },
    }),
  ],
  callbacks: {
    jwt({ token, user, trigger }) {
      if (trigger === 'signIn' && user) {
        token.accessToken = user.accessToken;
        token.refreshToken = user.refreshToken;
        token.accessTokenExpiresAt = user.accessTokenExpiresAt;
        token.roles = user.roles;
        token.email = user.email;
        token.customerId = user.customerId;
        token.sub = user.id;
        return token;
      }

      if (
        typeof token.accessTokenExpiresAt === 'number' &&
        token.accessTokenExpiresAt - 60_000 > Date.now()
      ) {
        return token;
      }

      return refreshAccessToken(token);
    },
    session({ session, token }) {
      session.accessToken = token.accessToken;
      session.error = token.error;
      if (session.user) {
        session.user.roles = token.roles;
        (session.user as Session['user']).email = token.email ?? null;
        session.user.customerId = token.customerId;
        session.user.id = token.sub ?? session.user.id;
      }
      return session;
    },
  },
  events: {
    async signOut(message) {
      if ('token' in message && message.token?.refreshToken) {
        try {
          await fetch(`${AUTH_API_URL}/api/v1/auth/logout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              refresh_token: message.token.refreshToken,
            }),
          });
        } catch {
          // Fire-and-forget: logout failure should not block sign-out
        }
      }
    },
  },
});
