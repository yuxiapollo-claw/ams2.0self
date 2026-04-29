import { expect, test } from '@playwright/test'

test('login page loads localized hero and form', async ({ page }) => {
  await page.goto('/login')
  await expect(page).toHaveTitle(/AMS2\.0/i)
  await expect(page.getByText('AMS2.0 审批与权限驾驶舱')).toBeVisible()
  await expect(page.getByRole('heading', { level: 1, name: /账号与权限管理中台/ })).toBeVisible()
  await expect(page.locator('input[name="loginName"]')).toBeVisible()
  await expect(page.locator('input[name="password"]')).toBeVisible()
  await expect(page.getByRole('button', { name: '进入系统' })).toBeVisible()
})
