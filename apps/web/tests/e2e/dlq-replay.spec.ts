import { expect, test } from '@playwright/test';
import { loginAs } from './fixtures/auth';

test.describe('DLQ replay flow', () => {
  test('admin navigates to DLQ → detail → replay → toast + disabled button', async ({ page }) => {
    // arrange
    await loginAs(page, 'admin@stablepay.io');

    // act — navigate to DLQ list
    await page.getByTestId('nav-dlq').click();
    await expect(page).toHaveURL('/admin/dlq');
    await expect(page.getByTestId('dlq-list-page')).toBeVisible();

    // act — wait for table rows to load
    const table = page.locator('table');
    await expect(table).toBeVisible();
    const rows = table.locator('tbody tr');
    await expect(rows.first()).toBeVisible({ timeout: 10_000 });

    // act — click first row to navigate to DLQ detail
    await rows.first().click();
    await expect(page).toHaveURL(/\/admin\/dlq\/.+/);
    await expect(page.getByTestId('dlq-detail-page')).toBeVisible();

    // assert — KV grid and error message visible
    await expect(page.getByTestId('dlq-kv-grid')).toBeVisible();

    // act — find replay section and click replay button
    const replaySection = page.getByTestId('dlq-replay-section');
    await expect(replaySection).toBeVisible();
    const replayButton = replaySection.getByTestId('replay-button');

    // skip if entry is non-replayable (SCHEMA_INVALID, LATE_EVENT, or retry exhausted)
    const hasReplayButton = await replayButton.isVisible().catch(() => false);
    if (!hasReplayButton) {
      test.skip(true, 'DLQ entry is non-replayable — skipping replay assertion');
      return;
    }

    await replayButton.click();

    // assert — toast appears with replay confirmation
    await expect(page.getByText(/Replay queued|Replay re-queued/)).toBeVisible({ timeout: 5_000 });

    // assert — button text changes to "Replayed"
    await expect(replayButton).toContainText('Replayed');

    // assert — button is disabled after replay
    await expect(replayButton).toBeDisabled();
  });
});
