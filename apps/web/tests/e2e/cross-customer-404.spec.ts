import { expect, test } from '@playwright/test';
import { loginAs } from './fixtures/auth';

test.describe('Cross-customer 404 enforcement (D-B4)', () => {
  test('alice sees 404 not-found card when navigating to a non-existent transaction ref', async ({
    page,
  }) => {
    // arrange
    await loginAs(page, 'alice@stablepay.io');

    // act — navigate to a fabricated ref that does not belong to alice
    await page.goto('/transactions/non-existent-cross-customer-ref-xyz');

    // assert — NotFoundCard is rendered with the correct copy
    await expect(page.getByTestId('not-found-card')).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText("We can't find that transaction")).toBeVisible();

    // assert — this is a 404 page, NOT a 403
    await expect(page.getByText('403')).not.toBeVisible();
    await expect(page.getByText('Forbidden')).not.toBeVisible();
  });
});
