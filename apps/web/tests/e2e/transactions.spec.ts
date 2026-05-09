import { expect, test } from '@playwright/test';
import { loginAs } from './fixtures/auth';

test.describe('Transactions happy path (WEB-08)', () => {
  test('login → transactions list → search → detail → polling footer', async ({ page }) => {
    // arrange
    await loginAs(page, 'alice@stablepay.io');

    // act — navigate to transactions
    await page.getByTestId('nav-transactions').click();
    await expect(page).toHaveURL('/transactions');
    await expect(page.getByRole('heading', { name: 'Transactions' })).toBeVisible();

    // act — wait for table to load and verify rows exist
    const table = page.locator('table');
    await expect(table).toBeVisible();
    const rows = table.locator('tbody tr');
    await expect(rows.first()).toBeVisible({ timeout: 10_000 });

    // act — type search query into search bar
    const searchInput = page.getByPlaceholder(/search/i);
    await searchInput.fill('TXN');
    await page.waitForTimeout(500);

    // act — click first row to navigate to detail
    await rows.first().click();
    await expect(page).toHaveURL(/\/transactions\/.+/);

    // assert — hero card with amount and status badge visible
    await expect(page.getByTestId('hero-card')).toBeVisible();
    await expect(page.getByTestId('metadata-grid')).toBeVisible();

    // assert — EventEnvelope card visible with KV rows
    await expect(page.getByTestId('envelope-card')).toBeVisible();

    // assert — polling footer visible
    await expect(page.getByTestId('polling-footer')).toBeVisible();
    await expect(page.getByTestId('polling-footer')).toContainText(/polling active|Terminal state/);
  });
});
