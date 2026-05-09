import { describe, expect, it } from 'vitest';
import { createDlqSummary } from '~/test/fixtures/dlq';
import { renderHook } from '~/test/render';
import { useDlqSummary } from './use-dlq-summary';

describe('useDlqSummary', () => {
  it('returns initial data when provided', () => {
    // arrange
    const summary = createDlqSummary({ total: 5 });

    // act
    const { result } = renderHook(() => useDlqSummary(summary));

    // assert
    expect(result.current.data).toEqual(summary);
  });

  it('starts without data when no initial data is provided', () => {
    // act
    const { result } = renderHook(() => useDlqSummary());

    // assert
    expect(result.current.data).toBeUndefined();
  });
});
