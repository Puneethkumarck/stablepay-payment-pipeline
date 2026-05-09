'use client';

import { useMultiTabSessionSync } from '~/lib/hooks/use-multi-tab-session-sync';

export function MultiTabSessionGuard() {
  useMultiTabSessionSync();
  return null;
}
