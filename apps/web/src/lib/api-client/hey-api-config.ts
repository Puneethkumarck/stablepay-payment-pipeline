import { auth } from '~/server/auth';
import type { CreateClientConfig } from './_generated/client.gen';

export const createClientConfig: CreateClientConfig = (config) => ({
  ...config,
  baseUrl: process.env.STABLEPAY_API_INTERNAL_URL ?? 'http://apps-api:8080',
  auth: async () => {
    const session = await auth();
    return session?.accessToken ? `Bearer ${session.accessToken}` : undefined;
  },
});
