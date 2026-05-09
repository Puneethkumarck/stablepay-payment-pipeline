import { useQueryClient } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { QueryProvider } from './query-client';

function StaleTimeInspector() {
  const client = useQueryClient();
  const staleTime = client.getDefaultOptions().queries?.staleTime;
  return <span data-testid="stale-time">{String(staleTime)}</span>;
}

describe('QueryProvider', () => {
  it('renders children', () => {
    // act
    render(
      <QueryProvider>
        <span>hello</span>
      </QueryProvider>,
    );

    // assert
    expect(screen.getByText('hello')).toBeTruthy();
  });

  it('provides a QueryClient with 10s stale time', () => {
    // act
    render(
      <QueryProvider>
        <StaleTimeInspector />
      </QueryProvider>,
    );

    // assert
    expect(screen.getByTestId('stale-time').textContent).toBe('10000');
  });
});
