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
    render(<Timeline steps={steps} />);
    const timeline = screen.getByTestId('timeline');
    expect(timeline).toHaveTextContent('Initiated');
    expect(timeline).toHaveTextContent('Screening');
    expect(timeline).toHaveTextContent('Execution');
  });

  it('renders subtitle for done step', () => {
    render(<Timeline steps={steps} />);
    expect(screen.getByTestId('timeline')).toHaveTextContent('2s ago');
  });

  it('renders check icon for done state', () => {
    const { container } = render(<Timeline steps={[{ label: 'Done', state: 'done' }]} />);
    expect(container.querySelector('.bg-success')).toBeInTheDocument();
  });

  it('renders pulsing dot for live state', () => {
    const { container } = render(<Timeline steps={[{ label: 'Live', state: 'live' }]} />);
    const liveNode = container.querySelector('.animate-\\[badge-pulse_2s_ease-in-out_infinite\\]');
    expect(liveNode).toBeInTheDocument();
  });

  it('renders subdued label for pending state', () => {
    const { container } = render(<Timeline steps={[{ label: 'Pending', state: 'pending' }]} />);
    expect(container.querySelector('.text-fg-3')).toBeInTheDocument();
  });

  it('renders meta content when provided', () => {
    const stepsWithMeta: TimelineStep[] = [
      { label: 'Step', state: 'done', meta: <span data-testid="meta-content">meta</span> },
    ];
    render(<Timeline steps={stepsWithMeta} />);
    expect(screen.getByTestId('meta-content')).toBeInTheDocument();
  });
});
