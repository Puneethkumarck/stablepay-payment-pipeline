import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Timeline, type TimelineStep } from './timeline';

describe('Timeline', () => {
  const steps: TimelineStep[] = [
    { label: 'Initiated', sub: '2s ago', state: 'done' },
    { label: 'Screening', state: 'live' },
    { label: 'Execution', state: 'pending' },
  ];

  it('renders all steps', () => {
    // act
    render(<Timeline steps={steps} />);

    // assert
    expect(screen.getByText('Initiated')).toBeInTheDocument();
    expect(screen.getByText('Screening')).toBeInTheDocument();
    expect(screen.getByText('Execution')).toBeInTheDocument();
  });

  it('renders subtitle for done step', () => {
    // act
    render(<Timeline steps={steps} />);

    // assert
    expect(screen.getByText('2s ago')).toBeInTheDocument();
  });

  it('renders done step with data-state attribute', () => {
    // act
    const { container } = render(<Timeline steps={[{ label: 'Done', state: 'done' }]} />);

    // assert
    const stepEl = container.querySelector('[data-state="done"]');
    expect(stepEl).toBeInTheDocument();
  });

  it('renders live step with data-state attribute', () => {
    // act
    const { container } = render(<Timeline steps={[{ label: 'Live', state: 'live' }]} />);

    // assert
    const stepEl = container.querySelector('[data-state="live"]');
    expect(stepEl).toBeInTheDocument();
  });

  it('renders pending step with data-state attribute', () => {
    // act
    const { container } = render(<Timeline steps={[{ label: 'Pending', state: 'pending' }]} />);

    // assert
    const stepEl = container.querySelector('[data-state="pending"]');
    expect(stepEl).toBeInTheDocument();
  });

  it('renders meta content when provided', () => {
    // arrange
    const stepsWithMeta: TimelineStep[] = [
      { label: 'Step', state: 'done', meta: <span data-testid="meta-content">meta</span> },
    ];

    // act
    render(<Timeline steps={stepsWithMeta} />);

    // assert
    expect(screen.getByTestId('meta-content')).toBeInTheDocument();
  });
});
