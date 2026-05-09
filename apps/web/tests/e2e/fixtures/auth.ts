import type { Page } from '@playwright/test';

export async function loginAs(page: Page, email: string) {
  await page.goto('/login');
  const username = email.split('@')[0];
  await page.getByTestId(`demo-${username}`).click();
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL('/');
}
