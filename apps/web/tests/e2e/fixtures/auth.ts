import type { Page } from '@playwright/test';

export async function loginAs(page: Page, email: string) {
  await page.goto('/login');
  await page.getByText(email).click();
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL('/');
}
