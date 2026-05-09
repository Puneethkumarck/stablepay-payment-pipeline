'use client';

import { useEffect } from 'react';
import { useTransactionFeed } from '~/lib/stores/sse-client-store';

const SSE_URL = process.env.NEXT_PUBLIC_SSE_URL ?? '/api/v1/streams/transactions';

interface LiveFeedConnectorProps {
  accessToken?: string;
}

export function LiveFeedConnector({ accessToken }: LiveFeedConnectorProps) {
  const connect = useTransactionFeed((s) => s.connect);
  const disconnect = useTransactionFeed((s) => s.disconnect);

  useEffect(() => {
    if (!accessToken) return;
    connect(accessToken, SSE_URL);
    return () => disconnect();
  }, [accessToken, connect, disconnect]);

  return null;
}
