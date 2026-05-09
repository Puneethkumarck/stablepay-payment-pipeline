import { render, screen } from '@testing-library/react';
import { Zap } from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { StatCard } from './stat-card';

describe('StatCard', () => {
  it('renders label and value', () => {
    // act
    render(<StatCard label="Total" value="1,234" />);

    // assert
    expect(screen.getByText('Total')).toBeInTheDocument();
    expect(screen.getByText('1,234')).toBeInTheDocument();
  });

  it('renders subtitle when provided', () => {
    // act
    render(<StatCard label="Volume" value="$500K" sub="Last 24h" />);

    // assert
    expect(screen.getByText('Last 24h')).toBeInTheDocument();
  });

  it('renders upward trend', () => {
    // act
    render(
      <StatCard label="Volume" value="100" trend={{ up: true, label: '12% from yesterday' }} />,
    );

    // assert
    expect(screen.getByText(/↑ 12% from yesterday/)).toBeInTheDocument();
  });

  it('renders downward trend', () => {
    // act
    render(
      <StatCard label="Volume" value="80" trend={{ up: false, label: '5% from yesterday' }} />,
    );

    // assert
    expect(screen.getByText(/↓ 5% from yesterday/)).toBeInTheDocument();
  });

  it('renders icon when provided', () => {
    // act
    const { container } = render(<StatCard label="Active" value="42" icon={Zap} />);

    // assert
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('does not render trend when not provided', () => {
    // act
    render(<StatCard label="Count" value="5" />);

    // assert
    expect(screen.queryByText(/↑/)).toBeNull();
    expect(screen.queryByText(/↓/)).toBeNull();
  });
});
