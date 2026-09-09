import { expect, test } from '@playwright/test'

test('overview loads with local backend status', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'BoundaryMesh' })).toBeVisible()
  await expect(page.getByText('Secure every system crossing.').first()).toBeVisible()
  await expect(page.getByText('LOCAL PROJECT DATA')).toBeVisible()
  await expect(page.getByText('SYNTHETIC DEMO / LOCAL')).toBeVisible({ timeout: 20_000 })
  await expect(page.getByText('READY', { exact: true }).first()).toBeVisible()
})

test('synthetic scenarios evaluate against the Java backend', async ({ page }) => {
  await page.goto('/')
  const result = page.locator('article.result')

  await page.getByRole('radio', { name: /Valid crossing/ }).check()
  await page.getByRole('button', { name: 'Evaluate crossing' }).click()
  await expect(result).toContainText('ALLOW', { timeout: 20_000 })
  await expect(result).not.toContainText('ALLOW WITH TRANSFORMATION')

  await page.getByRole('radio', { name: /Invalid identity/ }).check()
  await page.getByRole('button', { name: 'Evaluate crossing' }).click()
  await expect(result).toContainText('REJECT')

  await page.getByRole('radio', { name: /Destination unavailable/ }).check()
  await page.getByRole('button', { name: 'Evaluate crossing' }).click()
  await expect(result).toContainText('QUARANTINE')

  await page.getByRole('radio', { name: /Sensitive data transformation required/ }).check()
  await page.getByRole('button', { name: 'Evaluate crossing' }).click()
  await expect(result).toContainText('ALLOW WITH TRANSFORMATION')

  const transactionId = await result.locator('dd').first().textContent()
  await page.getByRole('link', { name: 'Open event detail' }).click()
  await expect(page).toHaveURL(/\/boundary-events\//)
  await expect(page.getByRole('heading', { name: 'Boundary event' })).toBeVisible()
  await expect(page.getByText(transactionId?.trim() ?? '')).toBeVisible()
  await expect(page.getByText('SYNTHETIC DEMO').first()).toBeVisible()
})

test('identity lab distinguishes valid JWT from missing permission', async ({ page }) => {
  await page.goto('/identity')
  await expect(page.getByRole('heading', { name: 'Identity Federation Lab' })).toBeVisible()

  await page.getByRole('radio', { name: /Valid machine identity/ }).check()
  await page.getByRole('button', { name: 'Evaluate identity' }).click()
  const result = page.locator('article.result')
  await expect(result).toContainText('ALLOW', { timeout: 20_000 })
  await expect(result).toContainText('Authentication')
  await expect(result).toContainText('Authorization')

  await page.getByRole('radio', { name: /Missing permission/ }).check()
  await page.getByRole('button', { name: 'Evaluate identity' }).click()
  await expect(result).toContainText('REJECT', { timeout: 20_000 })
  await expect(result).toContainText('Authorization')
  await expect(page.locator('body')).not.toContainText('eyJ')
})

test('webhook lab proves valid signature and replay without exposing secrets', async ({ page }) => {
  await page.goto('/webhooks')
  await expect(page.getByRole('heading', { name: 'Webhook Defense Lab' })).toBeVisible()
  const result = page.locator('article.result')

  await page.getByRole('radio', { name: /Valid signed webhook/ }).check()
  await page.getByRole('button', { name: 'Evaluate webhook' }).click()
  await expect(result).toContainText('ALLOW', { timeout: 20_000 })
  await expect(result).toContainText('Authenticity')
  await expect(result).toContainText('Freshness')
  await expect(result).toContainText('Replay Defense')

  await page.getByRole('radio', { name: /Invalid signature/ }).check()
  await page.getByRole('button', { name: 'Evaluate webhook' }).click()
  await expect(result).toContainText('REJECT', { timeout: 20_000 })

  await page.getByRole('radio', { name: /Replay attempt/ }).check()
  await page.getByRole('button', { name: 'Evaluate webhook' }).click()
  await expect(result).toContainText('REJECT', { timeout: 20_000 })
  await expect(result).toContainText('REPLAY_DETECTED')
  await expect(page.locator('body')).not.toContainText('v1=')
  await expect(page.locator('body')).not.toContainText('BOUNDARYMESH_QUAYFORGE_SECRET')
})

test('unknown routes show the 404 page', async ({ page }) => {
  await page.goto('/no-such-console-route')
  await expect(page.getByRole('heading', { name: 'Page not found' })).toBeVisible()
})
