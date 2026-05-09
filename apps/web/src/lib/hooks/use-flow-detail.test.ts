import { waitFor } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { createFlow } from '~/test/fixtures/flow';
import { server } from '~/test/msw-server';
import { renderHook } from '~/test/render';
import { useFlowDetail } from './use-flow-detail';

describe('useFlowDetail', () => {
  it('returns initial data immediately', () => {
    // arrange
    const flow = createFlow({ id: 'FLOW-100' });

    // act
    const { result } = renderHook(() => useFlowDetail('FLOW-100', flow));

    // assert
    expect(result.current.data).toEqual(flow);
  });

  it('fetches flow data from the API when no initial data provided', async () => {
    // arrange
    const flow = createFlow({ id: 'FLOW-200', status: 'COMPLETED' });
    server.use(http.get('/api/v1/flows/FLOW-200', () => HttpResponse.json(flow)));

    // act
    const { result } = renderHook(() => useFlowDetail('FLOW-200'));

    // assert
    await waitFor(() => {
      expect(result.current.data?.status).toBe('COMPLETED');
    });
  });

  it('stops polling when status is terminal', async () => {
    // arrange
    const flow = createFlow({ status: 'FAILED' });
    server.use(http.get('/api/v1/flows/FLOW-001', () => HttpResponse.json(flow)));

    // act
    const { result } = renderHook(() => useFlowDetail('FLOW-001', flow));

    // assert
    await waitFor(() => {
      expect(result.current.data).toEqual(flow);
    });
  });

  it('throws on API error', async () => {
    // arrange
    const initial = createFlow({ id: 'FLOW-ERR', status: 'INITIATED' });
    server.use(
      http.get('/api/v1/flows/FLOW-ERR', () =>
        HttpResponse.json({ error_code: 'STBLPAY-5000' }, { status: 500 }),
      ),
    );

    // act
    const { result } = renderHook(() => useFlowDetail('FLOW-ERR', initial));

    // assert
    await waitFor(() => {
      expect(result.current.error).toBeDefined();
    });
  });
});
