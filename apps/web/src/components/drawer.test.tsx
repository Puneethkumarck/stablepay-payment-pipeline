import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Drawer } from './drawer';

describe('Drawer', () => {
  it('renders title and children when open', () => {
    render(
      <Drawer open onOpenChange={() => {}} title="Details">
        <p>Content</p>
      </Drawer>,
    );
    expect(screen.getByText('Details')).toBeInTheDocument();
    expect(screen.getByText('Content')).toBeInTheDocument();
  });

  it('does not render content when closed', () => {
    render(
      <Drawer open={false} onOpenChange={() => {}} title="Details">
        <p>Content</p>
      </Drawer>,
    );
    expect(screen.queryByText('Content')).toBeNull();
  });
});
