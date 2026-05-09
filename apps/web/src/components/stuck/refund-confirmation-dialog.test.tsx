import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { createStuckPayment } from '~/test/fixtures/stuck';
import { render } from '~/test/render';
import { RefundConfirmationDialog } from './refund-confirmation-dialog';

vi.mock('sonner', () => ({
  toast: { info: vi.fn() },
}));

describe('RefundConfirmationDialog', () => {
  it('renders trigger button', () => {
    // arrange & act
    render(<RefundConfirmationDialog stuckPayment={createStuckPayment()} />);

    // assert
    expect(screen.getByTestId('trigger-refund-button')).toHaveTextContent('Trigger refund');
  });

  it('opens AlertDialog on trigger click with correct copy', async () => {
    // arrange
    const user = userEvent.setup();
    render(
      <RefundConfirmationDialog
        stuckPayment={createStuckPayment({ customer_id: 'CUST-DIALOG' })}
      />,
    );

    // act
    await user.click(screen.getByTestId('trigger-refund-button'));

    // assert
    expect(await screen.findByText('Trigger refund?')).toBeInTheDocument();
    expect(screen.getByText(/irreversible refund initiation event/)).toBeInTheDocument();
    expect(screen.getByText(/CUST-DIALOG/)).toBeInTheDocument();
  });

  it('renders Cancel and Trigger refund action buttons', async () => {
    // arrange
    const user = userEvent.setup();
    render(<RefundConfirmationDialog stuckPayment={createStuckPayment()} />);

    // act
    await user.click(screen.getByTestId('trigger-refund-button'));

    // assert
    expect(await screen.findByText('Cancel')).toBeInTheDocument();
    const actions = screen.getAllByText('Trigger refund');
    expect(actions.length).toBeGreaterThanOrEqual(1);
  });

  it('fires toast.info stub on confirm', async () => {
    // arrange
    const user = userEvent.setup();
    render(<RefundConfirmationDialog stuckPayment={createStuckPayment()} />);

    // act
    await user.click(screen.getByTestId('trigger-refund-button'));
    const confirmButtons = await screen.findAllByText('Trigger refund');
    const confirmButton = confirmButtons.find(
      (btn) => btn.closest('[data-slot="alert-dialog-action"]') !== null,
    );
    expect(confirmButton).toBeTruthy();
    await user.click(confirmButton!);

    // assert
    expect(toast.info).toHaveBeenCalledWith('Refund initiation deferred to Phase 6', {
      description:
        'The Stuck refund endpoint is not in REQUIREMENTS v1; backend wiring will be added in Phase 6.',
      duration: 8000,
    });
  });
});
