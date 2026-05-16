import { expect, test } from '@playwright/test';
import { loginAs } from './fixtures/auth';

test.describe('Theme toggle persistence', () => {
  test('toggles theme from dark to light, persists on reload, toggles back', async ({ page }) => {
    // arrange
    await loginAs(page, 'alice@stablepay.io');

    // assert — default theme is dark
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');

    // act — open user dropdown to reveal theme switch
    await page.getByTestId('user-dropdown-trigger').click();
    const themeSwitch = page.getByTestId('theme-switch');
    await expect(themeSwitch).toBeVisible();

    // act — click the switch to toggle to light
    await themeSwitch.getByRole('switch').click();

    // assert — html data-theme changes to light
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');

    // assert — localStorage persists the value
    const storedTheme = await page.evaluate(() => localStorage.getItem('sp4_theme'));
    expect(storedTheme).toBe('light');

    // act — reload page
    await page.reload();
    await expect(page.getByRole('navigation')).toBeVisible();

    // assert — theme persists as light after reload
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light');

    // act — open dropdown again and toggle back to dark
    await page.getByTestId('user-dropdown-trigger').click();
    await page.getByTestId('theme-switch').getByRole('switch').click();

    // assert — back to dark
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark');
    const restoredTheme = await page.evaluate(() => localStorage.getItem('sp4_theme'));
    expect(restoredTheme).toBe('dark');
  });
});
