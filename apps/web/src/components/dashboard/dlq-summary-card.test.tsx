import { screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createDlqSummary } from '~/test/fixtures/dlq';
import { render } from '~/test/render';
import { DlqSummaryCard } from './dlq-summary-card';

vi.mock('~/lib/hooks/use-dlq-summary', () => ({
  useDlqSummary: vi.fn(),
}));

import { useDlqSummary } from '~/lib/hooks/use-dlq-summary';

const mockUseDlqSummary = vi.mocked(useDlqSummary);

describe('DlqSummaryCard', () => {
  it('renders all 4 error class labels', () => {
    // arrange
    mockUseDlqSummary.mockReturnValue({ data: createDlqSummary() } as unknown as ReturnType<
      typeof useDlqSummary
    >);

    // act
    render(<DlqSummaryCard />);

    // assert
    expect(screen.getByText('Schema invalid')).toBeInTheDocument();
    expect(screen.getByText('Proc. failed')).toBeInTheDocument();
    expect(screen.getByText('Sink failure')).toBeInTheDocument();
    expect(screen.getByText('Late event')).toBeInTheDocument();
  });

  it('renders counts from the summary data', () => {
    // arrange
    const summary = createDlqSummary({
      by_error_class: {
        SCHEMA_INVALID: 5,
        PROCESSING_FAILED: 2,
        SINK_FAILURE: 0,
        LATE_EVENT: 1,
      },
    });
    mockUseDlqSummary.mockReturnValue({ data: summary } as unknown as ReturnType<
      typeof useDlqSummary
    >);

    // act
    render(<DlqSummaryCard />);

    // assert
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument();
  });

  it('renders DLQ inspector link', () => {
    // arrange
    mockUseDlqSummary.mockReturnValue({ data: createDlqSummary() } as unknown as ReturnType<
      typeof useDlqSummary
    >);

    // act
    render(<DlqSummaryCard />);

    // assert
    expect(screen.getByRole('link', { name: /open dlq inspector/i })).toHaveAttribute(
      'href',
      '/admin/dlq',
    );
  });

  it('renders zero counts when data is undefined', () => {
    // arrange
    mockUseDlqSummary.mockReturnValue({ data: undefined } as unknown as ReturnType<
      typeof useDlqSummary
    >);

    // act
    render(<DlqSummaryCard />);

    // assert
    const zeros = screen.getAllByText('0');
    expect(zeros).toHaveLength(4);
  });
});
