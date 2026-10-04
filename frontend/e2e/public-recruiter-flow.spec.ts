import { expect, test, type Page } from '@playwright/test'

async function expectNoHorizontalOverflow(page: Page): Promise<void> {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1)
  expect(overflow).toBe(false)
}

test('the public root is the product, and the operator page stays protected', async ({ page }) => {
  const privateCalls: string[] = []
  page.on('request', (request) => {
    const url = request.url()
    if (/\/api\/|\/session|identitytoolkit|securetoken/.test(url)) privateCalls.push(url)
  })

  await page.goto('/')
  await expect(page).toHaveURL(/\/$/)
  await expect(page.locator('.explorer__header')).toHaveCSS('display', 'flex')
  await expect(page.getByRole('heading', { name: 'System crossings', level: 1 })).toBeVisible()
  await expect(page.getByTestId('explorer-search')).toBeVisible()
  await expect(page.getByText('Portfolio demo').first()).toBeVisible()
  await expect(page.getByText('Synthetic data').first()).toBeVisible()
  await expect(page.getByText('not customer traffic')).toBeVisible()
  await expect(page.getByRole('link', { name: 'Explore BoundaryMesh' })).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'What I built' })).toHaveCount(0)
  await expect(page.getByRole('heading', { name: 'Operator access' })).toHaveCount(0)
  expect(privateCalls).toEqual([])

  await page.getByRole('button', { name: /Contact field transformed/ }).click()
  await page.getByRole('tab', { name: 'Crossing' }).click()
  await expect(page.getByRole('heading', { name: 'Incoming crossing' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Source identity' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Replay detection' })).toBeVisible()

  await page.getByRole('tab', { name: 'Policy' }).click()
  await expect(page.getByTestId('policy-decision')).toContainText('ALLOW WITH TRANSFORMATION')

  await page.getByRole('tab', { name: 'Evidence' }).click()
  await expect(page.getByTestId('attestation')).toContainText('signed')
  await expect(page.getByRole('heading', { name: 'Cloud KMS' })).toBeVisible()
  await page.getByText('Transformation applied').click()
  await expect(page.getByTestId('timeline')).toBeVisible()

  await page.getByRole('tab', { name: 'Delivery' }).click()
  await expect(page.getByTestId('delivery-state')).toContainText('Delivered after transformation')
  await expect(page.getByRole('button', { name: 'Acknowledge hold' })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Retry delivery' })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Approve release' })).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Reject crossing' })).toBeDisabled()

  await page.getByRole('button', { name: 'About the project' }).click()
  const about = page.getByRole('dialog', { name: 'About the project' })
  await expect(about).toBeVisible()
  await expect(about.getByRole('heading', { name: 'What I built' })).toBeVisible()
  await expect(about.getByRole('heading', { name: 'Architecture' })).toBeVisible()
  await expect(about.getByRole('heading', { name: 'Security model' })).toBeVisible()
  await expect(about.getByTestId('explorer-search')).toHaveCount(0)
  await expect(page.getByTestId('explorer-search')).toBeVisible()
  await about.getByRole('button', { name: 'Close' }).click()
  await expect(about).toHaveCount(0)
  expect(privateCalls).toEqual([])

  const productText = await page.locator('body').innerText()
  expect(productText).not.toMatch(/[-—–]/)

  await page.goto('/explore')
  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('heading', { name: 'System crossings', level: 1 })).toBeVisible()

  await page.getByRole('link', { name: 'Operator sign in' }).click()
  await expect(page).toHaveURL(/\/operator\/?$/)
  await expect(page.getByRole('heading', { name: 'Operator access' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Submit synthetic transaction' })).toHaveCount(0)
})

test('public product layouts do not overflow', async ({ page }) => {
  for (const viewport of [
    { width: 1440, height: 900 },
    { width: 768, height: 1024 },
    { width: 390, height: 844 },
  ]) {
    await page.setViewportSize(viewport)
    await page.goto('/')
    await expect(page.getByRole('heading', { name: 'System crossings' })).toBeVisible()
    await page.getByRole('tab', { name: 'Evidence' }).click()
    await page.getByRole('button', { name: /Replay rejected/ }).click()
    await expectNoHorizontalOverflow(page)
    await page.getByRole('button', { name: 'About the project' }).click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await expectNoHorizontalOverflow(page)
  }
})
