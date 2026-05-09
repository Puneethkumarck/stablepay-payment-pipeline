import { render, screen } from '@testing-library/react';
import { Zap } from 'lucide-react';
import { describe, expect, it } from 'vitest';
import { StatCard } from './stat-card';

describe('StatCard', () => {
  it('renders label and value', () => {
    render(<StatCard label="Total" value="1,234" />);
    const card = screen.getByTestId('stat-card');
    expect(card).toHaveTextContent('Total');
    expect(card).toHaveTextContent('1,234');
  });

  it('renders subtitle when provided', () => {
    render(<StatCard label="Volume" value="$500K" sub="Last 24h" />);
    expect(screen.getByTestId('stat-card')).toHaveTextContent('Last 24h');
  });

  it('renders upward trend', () => {
    render(
      <StatCard label="Volume" value="100" trend={{ up: true, label: '12% from yesterday' }} />,
    );
    expect(screen.getByTestId('stat-card')).toHaveTextContent('↑ 12% from yesterday');
  });

  it('renders downward trend', () => {
    render(
      <StatCard label="Volume" value="80" trend={{ up: false, label: '5% from yesterday' }} />,
    );
    expect(screen.getByTestId('stat-card')).toHaveTextContent('↓ 5% from yesterday');
  });

  it('renders icon when provided', () => {
    const { container } = render(<StatCard label="Active" value="42" icon={Zap} />);
    expect(container.querySelector('svg')).toBeInTheDocument();
  });

  it('renders accent halo when accentColor provided', () => {
    const { container } = render(<StatCard label="Test" value="1" accentColor="#9945FF" />);
    const halo = container.querySelector('.rounded-full.pointer-events-none');
    expect(halo).toBeInTheDocument();
  });
});
