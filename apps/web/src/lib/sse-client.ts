import { createStore, useStore } from 'zustand';

export interface TransactionEvent {
  id?: string;
  type?: string;
  data: unknown;
}

export type ConnectionStatus = 'connecting' | 'connected' | 'disconnected' | 'fallback-polling';

interface TransactionFeedState {
  events: TransactionEvent[];
  status: ConnectionStatus;
}

interface TransactionFeedActions {
  connect: (accessToken: string, url: string) => void;
  disconnect: () => void;
}

type TransactionFeedStore = TransactionFeedState & TransactionFeedActions;

const HEARTBEAT_TIMEOUT_MS = 60_000;
const DEFAULT_RETRY_MS = 2_000;
const MAX_RETRY_MS = 30_000;

export interface ParsedSSEFrame {
  events: TransactionEvent[];
  retryMs?: number;
  hadHeartbeat: boolean;
}

export function parseSSEFrames(text: string): ParsedSSEFrame {
  const events: TransactionEvent[] = [];
  let retryMs: number | undefined;
  let hadHeartbeat = false;

  let currentId: string | undefined;
  let currentEvent: string | undefined;
  let currentData = '';

  const lines = text.split('\n');

  for (const line of lines) {
    if (line === '') {
      if (currentData) {
        let parsed: unknown;
        try {
          parsed = JSON.parse(currentData.trimEnd());
        } catch {
          parsed = currentData.trimEnd();
        }
        events.push({
          id: currentId,
          type: currentEvent,
          data: parsed,
        });
      }
      currentId = undefined;
      currentEvent = undefined;
      currentData = '';
      continue;
    }

    if (line.startsWith(':')) {
      hadHeartbeat = true;
      continue;
    }

    const colonIdx = line.indexOf(':');
    if (colonIdx === -1) continue;

    const field = line.slice(0, colonIdx);
    const value = line.slice(colonIdx + 1).replace(/^ /, '');

    switch (field) {
      case 'data':
        currentData += (currentData ? '\n' : '') + value;
        break;
      case 'id':
        currentId = value;
        break;
      case 'event':
        currentEvent = value;
        break;
      case 'retry': {
        const parsed = Number.parseInt(value, 10);
        if (!Number.isNaN(parsed) && parsed > 0) {
          retryMs = Math.min(parsed, MAX_RETRY_MS);
        }
        break;
      }
    }
  }

  return { events, retryMs, hadHeartbeat };
}

export function createTransactionFeedStore() {
  let abortController: AbortController | null = null;
  let heartbeatTimer: ReturnType<typeof setTimeout> | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let currentRetryMs = DEFAULT_RETRY_MS;

  function clearTimers() {
    if (heartbeatTimer) {
      clearTimeout(heartbeatTimer);
      heartbeatTimer = null;
    }
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  }

  const store = createStore<TransactionFeedStore>((set, get) => ({
    events: [],
    status: 'disconnected',

    connect(accessToken: string, url: string) {
      get().disconnect();
      set({ status: 'connecting', events: [] });

      const controller = new AbortController();
      abortController = controller;
      currentRetryMs = DEFAULT_RETRY_MS;

      const scheduleReconnect = () => {
        clearTimers();
        set({ status: 'connecting' });
        reconnectTimer = setTimeout(() => {
          get().connect(accessToken, url);
        }, currentRetryMs);
        currentRetryMs = Math.min(currentRetryMs * 2, MAX_RETRY_MS);
      };

      const resetHeartbeat = () => {
        if (heartbeatTimer) clearTimeout(heartbeatTimer);
        heartbeatTimer = setTimeout(() => {
          abortController?.abort();
          scheduleReconnect();
        }, HEARTBEAT_TIMEOUT_MS);
      };

      (async () => {
        try {
          const response = await fetch(url, {
            headers: { Authorization: `Bearer ${accessToken}` },
            signal: controller.signal,
          });

          if (response.status === 401) {
            set({ status: 'disconnected' });
            return;
          }

          if (!response.ok || !response.body) {
            scheduleReconnect();
            return;
          }

          set({ status: 'connected' });
          currentRetryMs = DEFAULT_RETRY_MS;
          resetHeartbeat();

          const reader = response.body.getReader();
          const decoder = new TextDecoder();
          let buffer = '';

          while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });

            const lastDoubleNewline = buffer.lastIndexOf('\n\n');
            if (lastDoubleNewline === -1) continue;

            const complete = buffer.slice(0, lastDoubleNewline + 2);
            buffer = buffer.slice(lastDoubleNewline + 2);

            const { events: newEvents, retryMs, hadHeartbeat } = parseSSEFrames(complete);

            if (retryMs !== undefined) {
              currentRetryMs = retryMs;
            }

            if (newEvents.length > 0 || hadHeartbeat) {
              resetHeartbeat();
            }

            if (newEvents.length > 0) {
              set((state) => ({ events: [...state.events, ...newEvents] }));
            }
          }

          scheduleReconnect();
        } catch (error) {
          if (error instanceof DOMException && error.name === 'AbortError') {
            return;
          }

          if (get().status !== 'disconnected') {
            set({ status: 'fallback-polling' });
          }
        }
      })();
    },

    disconnect() {
      clearTimers();
      abortController?.abort();
      abortController = null;
      set({ status: 'disconnected', events: [] });
    },
  }));

  return store;
}

const defaultStore = createTransactionFeedStore();

export function useTransactionFeed(): TransactionFeedStore;
export function useTransactionFeed<T>(selector: (state: TransactionFeedStore) => T): T;
export function useTransactionFeed<T>(selector?: (state: TransactionFeedStore) => T) {
  return useStore(defaultStore, selector ?? ((state) => state as unknown as T));
}
