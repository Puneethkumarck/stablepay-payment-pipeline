'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

const SESSION_FLAG = 'sp4_authed';

export function useMultiTabSessionSync() {
  const router = useRouter();

  useEffect(() => {
    localStorage.setItem(SESSION_FLAG, '1');

    const handler = (e: StorageEvent) => {
      if (e.key === SESSION_FLAG && e.newValue === null) {
        router.replace('/login?reason=signed-out-elsewhere');
      }
    };

    window.addEventListener('storage', handler);
    return () => window.removeEventListener('storage', handler);
  }, [router]);
}
