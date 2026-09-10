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

test('contract drift and data boundary labs use Java outcomes', async ({ page }) => {
  await page.goto('/contracts')
  await expect(page.getByRole('heading', { name: 'Contract Drift Lab' })).toBeVisible()
  const result = page.locator('article.result')

  await page.getByRole('radio', { name: /Valid v1 contract/ }).check()
  await page.getByRole('button', { name: 'Evaluate contract' }).click()
  await expect(result).toContainText('ALLOW', { timeout: 20_000 })

  await page.getByRole('radio', { name: /Contract drift/ }).check()
  await page.getByRole('button', { name: 'Evaluate contract' }).click()
  await expect(result).toContainText('QUARANTINE', { timeout: 20_000 })

  await page.goto('/data-boundary')
  await expect(page.getByRole('heading', { name: 'Data Boundary Lab' })).toBeVisible()
  await page.getByRole('radio', { name: /Rivertide Settlements/ }).check()
  await page.getByRole('radio', { name: /Email contrast/ }).check()
  await page.getByRole('button', { name: 'Evaluate data boundary' }).click()
  await expect(page.locator('article.result')).toContainText('ALLOW WITH TRANSFORMATION', { timeout: 20_000 })
  await expect(page.getByText('[REDACTED]').first()).toBeVisible()

  await page.getByRole('radio', { name: /Forbidden critical field/ }).check()
  await page.getByRole('button', { name: 'Evaluate data boundary' }).click()
  await expect(page.locator('article.result')).toContainText('REJECT', { timeout: 20_000 })
})

test('full crossing inspection shows real composed stages', async ({ page }) => {
  await page.goto('/crossing')
  await expect(page.getByRole('heading', { name: 'Full Boundary Crossing' })).toBeVisible()
  await page.getByRole('radio', { name: /Egress redaction/ }).check()
  await page.getByRole('button', { name: 'Inspect crossing' }).click()
  const result = page.locator('article.result')
  await expect(result).toContainText('ALLOW WITH TRANSFORMATION', { timeout: 20_000 })
  await expect(page.getByLabel('Boundary crossing inspection')).toContainText('Identity')
  await expect(page.getByLabel('Boundary crossing inspection')).toContainText('Data Boundary')
  await expect(page.getByText('PASS').first()).toBeVisible()
})

test('delivery lab delivers a transformed payload without secrets', async ({ page }) => {
  await page.goto('/delivery')
  await expect(page.getByRole('heading', { name: 'Delivery Lab' })).toBeVisible()
  await page.getByRole('radio', { name: /Successful transformed delivery/ }).check()
  await page.getByRole('button', { name: 'Run delivery' }).click()
  await expect(page.getByTestId('delivery-kind')).toHaveText('DELIVERED', { timeout: 20_000 })
  await expect(page.getByText('ALLOW WITH TRANSFORMATION').first()).toBeVisible()
  await expect(page.getByText('[REDACTED]').first()).toBeVisible()
  await expect(page.getByLabel('Approved delivery body')).not.toContainText(
    'ada.lovelace@example.test',
  )
  await expect(page.locator('body')).not.toContainText('TEST-SECRET-')
})

test('delivery lab holds credential and consent failures without routing', async ({ page }) => {
  await page.goto('/delivery')
  await page.getByRole('radio', { name: /Missing credential/ }).check()
  await page.getByRole('button', { name: 'Run delivery' }).click()
  await expect(page.getByTestId('delivery-kind')).toHaveText('NOT_ATTEMPTED', { timeout: 20_000 })
  await expect(page.getByText('not routed')).toBeVisible()

  await page.getByRole('radio', { name: /Missing consent/ }).check()
  await page.getByRole('button', { name: 'Run delivery' }).click()
  await expect(page.getByText('Consent MISSING')).toBeVisible({ timeout: 20_000 })
  await expect(page.getByTestId('delivery-kind')).toHaveText('NOT_ATTEMPTED')
})

test('quarantine recovery reprocesses after a hold', async ({ page }) => {
  await page.goto('/delivery')
  await page.getByRole('radio', { name: /Missing credential/ }).check()
  await page.getByRole('button', { name: 'Run delivery' }).click()
  await expect(page.getByTestId('delivery-kind')).toHaveText('NOT_ATTEMPTED', { timeout: 20_000 })
  await expect(page.getByRole('link', { name: 'Open quarantine item' })).toBeVisible()
  const quarantineHref = await page.getByRole('link', { name: 'Open quarantine item' }).getAttribute('href')
  await page.goto(quarantineHref ?? '/quarantine')
  await expect(page.getByRole('heading', { name: 'Quarantine item' })).toBeVisible()
  await page.getByRole('button', { name: 'Reprocess as operator' }).click()
  await expect(page.getByText('Recovery result')).toBeVisible({ timeout: 20_000 })
  await expect(page.getByText('Delivery DELIVERED')).toBeVisible()
  await expect(page.getByText('RESOLVED').first()).toBeVisible()
})

test('delivery lab suppresses a duplicate instruction', async ({ page }) => {
  await page.goto('/delivery')
  await page.getByRole('radio', { name: /Duplicate delivery instruction/ }).check()
  await page.getByRole('button', { name: 'Run delivery' }).click()
  await expect(page.getByTestId('delivery-kind')).toHaveText('DUPLICATE_SUPPRESSED', {
    timeout: 20_000,
  })
  await expect(page.getByText('provider called no')).toBeVisible()
})

test('credential lab shows references and never secret values', async ({ page }) => {
  await page.goto('/credentials')
  await expect(page.getByRole('heading', { name: 'Credential & Consent' })).toBeVisible()
  await expect(page.getByText('LOCAL SECURITY MODEL / SYNTHETIC DEMO').first()).toBeVisible()
  await expect(page.getByText('covecrm-api').first()).toBeVisible({ timeout: 20_000 })
  await expect(page.getByText('ACTIVE').first()).toBeVisible()
  await page.getByRole('radio', { name: /Valid credential \+ missing consent/ }).check()
  await page.getByRole('button', { name: 'Evaluate credential boundary' }).click()
  await expect(page.getByText('QUARANTINE').first()).toBeVisible({ timeout: 20_000 })
  await expect(page.locator('body')).not.toContainText('TEST-SECRET-')
})

test('unknown routes show the 404 page', async ({ page }) => {
  await page.goto('/no-such-console-route')
  await expect(page.getByRole('heading', { name: 'Page not found' })).toBeVisible()
})
