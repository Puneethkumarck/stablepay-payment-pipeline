import { expect, test } from '@playwright/test';

test.describe('Login flow', () => {
  test('signs in with demo account alice@stablepay.io', async ({ page }) => {
    // arrange
    await page.goto('/login');

    // act
    await page.getByTestId('demo-alice').click();
    await page.getByTestId('login-submit').click();

    // assert
    await expect(page).toHaveURL('/');
    await expect(page.getByRole('navigation')).toBeVisible();
    await expect(page.getByText('alice@stablepay.io')).toBeVisible();
  });
});
