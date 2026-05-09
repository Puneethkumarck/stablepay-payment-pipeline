import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createTransactionFeedStore, parseSSEFrames } from './sse-client-store';

describe('parseSSEFrames', () => {
  it('parses a single data event', () => {
    // arrange
    const text = 'data: {"id":"1","status":"COMPLETED"}\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.events).toHaveLength(1);
    expect(result.events[0]?.data).toEqual({ id: '1', status: 'COMPLETED' });
    expect(result.hadHeartbeat).toBe(false);
  });

  it('parses multiple events', () => {
    // arrange
    const text = 'data: {"a":1}\n\ndata: {"b":2}\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.events).toHaveLength(2);
    expect(result.events[0]?.data).toEqual({ a: 1 });
    expect(result.events[1]?.data).toEqual({ b: 2 });
  });

  it('parses event id and event type fields', () => {
    // arrange
    const text = 'id: 42\nevent: transaction\ndata: {"ref":"TXN-001"}\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.events).toHaveLength(1);
    expect(result.events[0]?.id).toBe('42');
    expect(result.events[0]?.type).toBe('transaction');
    expect(result.events[0]?.data).toEqual({ ref: 'TXN-001' });
  });

  it('extracts retry directive', () => {
    // arrange
    const text = 'retry: 5000\ndata: {"x":1}\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.retryMs).toBe(5000);
  });

  it('caps retry at 30 seconds', () => {
    // arrange
    const text = 'retry: 60000\ndata: {"x":1}\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.retryMs).toBe(30_000);
  });

  it('detects heartbeat comments', () => {
    // arrange
    const text = ': heartbeat\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.hadHeartbeat).toBe(true);
    expect(result.events).toHaveLength(0);
  });

  it('handles multi-line data fields', () => {
    // arrange
    const text = 'data: line1\ndata: line2\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.events).toHaveLength(1);
    expect(result.events[0]?.data).toBe('line1\nline2');
  });

  it('handles non-JSON data as string', () => {
    // arrange
    const text = 'data: plain text message\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.events).toHaveLength(1);
    expect(result.events[0]?.data).toBe('plain text message');
  });

  it('ignores invalid retry values', () => {
    // arrange
    const text = 'retry: abc\ndata: {"x":1}\n\n';

    // act
    const result = parseSSEFrames(text);

    // assert
    expect(result.retryMs).toBeUndefined();
  });
});

describe('createTransactionFeedStore', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  it('starts with disconnected status and empty events', () => {
    // act
    const store = createTransactionFeedStore();

    // assert
    expect(store.getState().status).toBe('disconnected');
    expect(store.getState().events).toEqual([]);
  });

  it('sets status to connecting when connect is called', () => {
    // arrange
    const store = createTransactionFeedStore();
    vi.spyOn(globalThis, 'fetch').mockImplementation(() => new Promise(() => {}));

    // act
    store.getState().connect('token', 'http://localhost/sse');

    // assert
    expect(store.getState().status).toBe('connecting');

    // cleanup
    store.getState().disconnect();
  });

  it('sets status to disconnected on 401 response', async () => {
    // arrange
    const store = createTransactionFeedStore();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 401 }));

    // act
    store.getState().connect('bad-token', 'http://localhost/sse');
    await vi.advanceTimersByTimeAsync(0);

    // assert
    expect(store.getState().status).toBe('disconnected');
  });

  it('disconnect clears events and sets status', () => {
    // arrange
    const store = createTransactionFeedStore();
    vi.spyOn(globalThis, 'fetch').mockImplementation(() => new Promise(() => {}));
    store.getState().connect('token', 'http://localhost/sse');

    // act
    store.getState().disconnect();

    // assert
    expect(store.getState().status).toBe('disconnected');
    expect(store.getState().events).toEqual([]);
  });

  it('sends Authorization header with fetch', async () => {
    // arrange
    const store = createTransactionFeedStore();
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(() => new Promise(() => {}));

    // act
    store.getState().connect('my-jwt-token', 'http://localhost/sse');

    // assert
    expect(fetchSpy).toHaveBeenCalledWith('http://localhost/sse', {
      headers: { Authorization: 'Bearer my-jwt-token' },
      signal: expect.any(AbortSignal),
    });

    // cleanup
    store.getState().disconnect();
  });

  it('parses streamed SSE events and adds to state', async () => {
    // arrange
    const store = createTransactionFeedStore();
    const chunk = new TextEncoder().encode('data: {"ref":"TXN-001"}\n\n');
    const stream = new ReadableStream({
      start(controller) {
        controller.enqueue(chunk);
        controller.close();
      },
    });
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(stream, { status: 200 }));

    // act
    store.getState().connect('token', 'http://localhost/sse');
    await vi.advanceTimersByTimeAsync(0);

    // assert
    expect(store.getState().events).toHaveLength(1);
    expect(store.getState().events[0]?.data).toEqual({ ref: 'TXN-001' });

    // cleanup
    store.getState().disconnect();
  });

  it('preserves events across reconnections after stream ends', async () => {
    // arrange
    const store = createTransactionFeedStore();
    const chunk1 = new TextEncoder().encode('data: {"ref":"TXN-001"}\n\n');
    const stream1 = new ReadableStream({
      start(controller) {
        controller.enqueue(chunk1);
        controller.close();
      },
    });
    const chunk2 = new TextEncoder().encode('data: {"ref":"TXN-002"}\n\n');
    const stream2 = new ReadableStream({
      start(controller) {
        controller.enqueue(chunk2);
        controller.close();
      },
    });
    const fetchSpy = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(stream1, { status: 200 }))
      .mockResolvedValueOnce(new Response(stream2, { status: 200 }));

    // act — first connection delivers TXN-001, stream closes, schedules reconnect
    store.getState().connect('token', 'http://localhost/sse');
    await vi.advanceTimersByTimeAsync(0);

    expect(store.getState().events).toHaveLength(1);
    expect(store.getState().events[0]?.data).toEqual({ ref: 'TXN-001' });

    // act — reconnect timer fires (2s default), second stream delivers TXN-002
    await vi.advanceTimersByTimeAsync(2_000);
    await vi.advanceTimersByTimeAsync(0);

    // assert — both events preserved
    expect(fetchSpy).toHaveBeenCalledTimes(2);
    expect(store.getState().events).toHaveLength(2);
    expect(store.getState().events[0]?.data).toEqual({ ref: 'TXN-001' });
    expect(store.getState().events[1]?.data).toEqual({ ref: 'TXN-002' });

    // cleanup
    store.getState().disconnect();
  });

  it('caps event buffer at MAX_EVENTS (500)', async () => {
    // arrange
    const store = createTransactionFeedStore();
    const lines = Array.from({ length: 510 }, (_, i) => `data: {"i":${i}}\n\n`).join('');
    const chunk = new TextEncoder().encode(lines);
    const stream = new ReadableStream({
      start(controller) {
        controller.enqueue(chunk);
        controller.close();
      },
    });
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(stream, { status: 200 }));

    // act
    store.getState().connect('token', 'http://localhost/sse');
    await vi.advanceTimersByTimeAsync(0);

    // assert — capped at 500, oldest events evicted
    expect(store.getState().events).toHaveLength(500);
    expect(store.getState().events[0]?.data).toEqual({ i: 10 });
    expect(store.getState().events[499]?.data).toEqual({ i: 509 });

    // cleanup
    store.getState().disconnect();
  });
});
