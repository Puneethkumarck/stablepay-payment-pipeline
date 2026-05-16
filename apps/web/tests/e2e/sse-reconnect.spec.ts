import { expect, test } from '@playwright/test';
import { loginAs } from './fixtures/auth';

test.describe('SSE reconnect flow', () => {
  test('SSE connection indicator shows connected state after login', async ({ page }) => {
    // arrange
    await loginAs(page, 'alice@stablepay.io');

    // assert — live feed panel is visible
    const liveFeed = page.getByTestId('live-feed');
    await expect(liveFeed).toBeVisible({ timeout: 10_000 });

    // assert — SSE status eyebrow is visible (connected or reconnecting)
    await expect(liveFeed.locator('.sp-eyebrow').first()).toBeVisible();
  });

  test('SSE shows reconnecting state when stream is interrupted', async ({ page }) => {
    test.setTimeout(60_000);

    // arrange — intercept the SSE endpoint to simulate connection then abort
    await page.route('**/api/v1/streams/transactions', async (route) => {
      await route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' },
        body: ':heartbeat\n\n',
      });
    });

    await loginAs(page, 'alice@stablepay.io');

    // assert — live feed appears
    const liveFeed = page.getByTestId('live-feed');
    await expect(liveFeed).toBeVisible({ timeout: 10_000 });

    // act — abort the SSE route to simulate server going down
    await page.route('**/api/v1/streams/transactions', async (route) => {
      await route.abort('connectionfailed');
    });

    // assert — status changes to reconnecting within a reasonable timeout
    await expect(liveFeed.getByText(/Reconnecting|Disconnected|Polling/)).toBeVisible({
      timeout: 15_000,
    });

    // act — restore SSE route to simulate server coming back
    await page.route('**/api/v1/streams/transactions', async (route) => {
      await route.fulfill({
        status: 200,
        headers: { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' },
        body: ':heartbeat\n\n',
      });
    });

    // assert — eventually reconnects (status returns to SSE feed)
    await expect(liveFeed.getByText('SSE feed')).toBeVisible({ timeout: 35_000 });
  });
});
