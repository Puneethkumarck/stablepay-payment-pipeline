import { expect, test } from '@playwright/test';

test.describe('Login flow', () => {
  test('signs in with demo account alice@stablepay.io', async ({ page }) => {
    // arrange
    await page.goto('/login');

    // act
    await page.getByText('alice@stablepay.io').click();
    await page.getByRole('button', { name: 'Sign in' }).click();

    // assert
    await expect(page).toHaveURL('/');
    await expect(page.getByRole('navigation')).toBeVisible();
    await expect(page.getByText('alice@stablepay.io')).toBeVisible();
  });
});
