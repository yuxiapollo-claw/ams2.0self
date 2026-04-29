import { expect, test, type Page } from '@playwright/test'

async function mockCockpitApis(page: Page) {
  await page.route('**/api/dashboard/summary', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        data: {
          metrics: {
            userTotal: 12,
            departmentTotal: 4,
            deviceAccountTotal: 18,
            pendingRequestTotal: 2
          },
          alerts: [],
          recentRequests: [],
          quickActions: [
            {
              actionKey: 'user-list',
              title: '打开用户目录',
              description: '进入用户工作台。'
            },
            {
              actionKey: 'request-list',
              title: '查看申请',
              description: '检查待处理审批。'
            }
          ]
        }
      })
    })
  })

  await page.route('**/api/users', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        data: {
          list: [],
          total: 0
        }
      })
    })
  })

  await page.route('**/api/departments', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        data: {
          list: [],
          total: 0
        }
      })
    })
  })

  await page.route('**/api/device-accounts', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        data: {
          list: [],
          total: 0
        }
      })
    })
  })

  await page.route('**/api/assets/tree', async (route) => {
    await route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify({
        data: []
      })
    })
  })
}

test('authenticated shell shows cockpit navigation', async ({ page }) => {
  await mockCockpitApis(page)
  await page.addInitScript(() => {
    window.sessionStorage.setItem('ams_auth_token', 'dev-token')
  })

  await page.goto('/')
  const sidebarNav = page.locator('.sidebar__nav')

  await expect(page.getByText('资产管理系统').first()).toBeVisible()
  await expect(page.getByTestId('global-search')).toBeVisible()
  await expect(page.getByTestId('toggle-locale')).toHaveText('中文')
  await expect(page.getByTestId('toggle-theme')).toHaveText('暗黑')
  await expect(sidebarNav.locator('a[href="/users"]')).toBeVisible()
  await expect(sidebarNav.locator('a[href="/device-accounts"]')).toBeVisible()
  await expect(sidebarNav.locator('a[href="/requests"]')).toBeVisible()

  await sidebarNav.locator('a[href="/users"]').click()
  await expect(page.getByRole('heading', { name: '用户管理工作台' })).toBeVisible()

  await page.getByTestId('toggle-locale').click()
  await expect(page.getByTestId('toggle-locale')).toHaveText('English')
  await expect(page.getByRole('heading', { name: 'User Management Workbench' })).toBeVisible()

  await page.getByTestId('toggle-theme').click()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')

  await sidebarNav.locator('a[href="/device-accounts"]').click()
  await expect(page.getByRole('heading', { name: 'Device Account Management Workbench' })).toBeVisible()
})
