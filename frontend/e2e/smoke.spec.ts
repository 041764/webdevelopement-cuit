import { expect, test } from '@playwright/test'

test('app boots and shows login', async ({ page }) => {
  const errors: string[] = []

  page.on('pageerror', (err) => {
    errors.push(err.message)
  })

  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text())
  })

  await page.goto('/login')
  await expect(page.getByTestId('login-page')).toBeVisible({ timeout: 15000 })

  if (errors.length > 0) {
    throw new Error(`page errors:\n${errors.join('\n')}`)
  }
})

test('redirects unauthenticated access to login', async ({ page }) => {
  await page.goto('/activities')
  await expect(page.getByTestId('login-page')).toBeVisible({ timeout: 15000 })
})

test('loads activities list when authenticated', async ({ page }) => {
  const errors: string[] = []

  page.on('pageerror', (err) => {
    errors.push(err.message)
  })

  page.on('console', (msg) => {
    if (msg.type() === 'error') errors.push(msg.text())
  })

  await page.route('**/api/auth/me**', async (route) => {
    const req = route.request()
    const resource = req.resourceType()

    if (resource !== 'fetch' && resource !== 'xhr') {
      await route.continue()
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        userId: 1,
        userType: 'TEACHER',
        id: '1001',
        name: 'T1',
        status: 'ACTIVE',
        collegeId: null,
        roles: ['TUTOR'],
      }),
    })
  })

  await page.route('**/api/activities**', async (route) => {
    const req = route.request()
    const resource = req.resourceType()

    if (resource !== 'fetch' && resource !== 'xhr') {
      await route.continue()
      return
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        page: 1,
        size: 20,
        total: 1,
        items: [
          {
            id: 1,
            classId: 1,
            term: '2026-02-23-1',
            title: 'A1',
            requiresReview: false,
            status: 'DRAFT',
            capacity: null,
            description: null,
            createdByUserId: 1,
            createdAt: '2026-01-10T12:00:00Z',
          },
        ],
      }),
    })
  })

  await page.addInitScript(() => {
    localStorage.setItem(
      'tutor-management.auth',
      JSON.stringify({
        accessToken: 'access',
        refreshToken: 'refresh',
        accessExpiresAt: new Date(Date.now() + 60_000).toISOString(),
        refreshExpiresAt: new Date(Date.now() + 120_000).toISOString(),
      }),
    )
  })

  const response = page.waitForResponse((r) => r.url().includes('/api/activities') && r.status() === 200)

  await page.goto('/activities')
  await response

  if (errors.length > 0) {
    throw new Error(`page errors:\n${errors.join('\n')}`)
  }

  expect(page.url()).toContain('/activities')
  await expect(page.getByText('A1')).toBeVisible({ timeout: 15000 })
})
