import { expect, test } from '@playwright/test';
import { loginAs } from './fixtures/auth';

test.describe('Session expired flow (D-A4)', () => {
  test('expired session shows banner and redirects to login with reason param', async ({
    page,
  }) => {
    // arrange — login first
    await loginAs(page, 'alice@stablepay.io');
    await expect(page.getByRole('navigation')).toBeVisible();

    // act — clear the session cookie to simulate expiry
    await page.context().clearCookies();

    // act — navigate to a protected page to trigger auth check
    await page.goto('/transactions');

    // assert — redirected to login page with session-expired reason
    await page.waitForURL(/\/login/);
    const url = new URL(page.url());
    expect(url.pathname).toBe('/login');

    // assert — reason banner is displayed on login page
    await expect(page.getByTestId('reason-banner')).toBeVisible({ timeout: 5_000 });
    await expect(page.getByText('Your session has expired.')).toBeVisible();
  });

  test('login page shows reason banner when navigated with reason=session-expired', async ({
    page,
  }) => {
    // act — go directly to login with the reason query param
    await page.goto('/login?reason=session-expired');

    // assert — reason banner visible with correct copy
    await expect(page.getByTestId('reason-banner')).toBeVisible();
    await expect(page.getByText('Your session has expired.')).toBeVisible();
  });
});
