import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  PORTFOLIO_EXPLORER_CROSSINGS,
  filterPortfolioCrossings,
} from '@/domain/portfolioExplorer'
import { selectPublicEntry } from '@/publicEntry'
import { formatPortfolioDate } from '@/domain/portfolioDate'
import PublicExplorerView from '@/views/PublicExplorerView.vue'

function visibleText(wrapper: ReturnType<typeof mount>) {
  return wrapper.text()
}

describe('public product explorer', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('chooses the public document from the path and leaves the lab router alone', () => {
    expect(selectPublicEntry('/')).toBe('product')
    expect(selectPublicEntry('/explore')).toBe('redirect-home')
    expect(selectPublicEntry('/explore/')).toBe('redirect-home')
    expect(selectPublicEntry('/operator')).toBe('missing')
    expect(selectPublicEntry('/identity')).toBe('missing')
  })

  it('renders the synthetic portfolio without auth or network calls', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const wrapper = mount(PublicExplorerView)
    await flushPromises()
    const text = visibleText(wrapper)
    expect(text).toContain('Portfolio demo')
    expect(text).toContain('Synthetic data')
    expect(text).toContain('not customer traffic')
    expect(text).toContain('System crossings')
    expect(text).toContain('Overview')
    expect(text).not.toContain('Explore BoundaryMesh')
    expect(text).not.toContain('What I built')
    expect(text).toContain('QuayForge order accepted')
    expect(text).toContain('Contact field transformed')
    expect(text).toContain('Contract drift held')
    expect(text).toContain('Replay rejected')
    expect(text).toContain('ALLOW')
    expect(text).toContain('ALLOW WITH TRANSFORMATION')
    expect(text).toContain('QUARANTINE')
    expect(text).toContain('REJECT')
    expect(wrapper.get('nav button').text()).toBe('About the project')
    expect(wrapper.get('nav a[href="/operator"]').text()).toBe('Operator sign in')
    expect(wrapper.get('nav a[href="https://github.com/Adellaghmari/boundarymesh"]').text()).toBe('Source on GitHub')
    expect(text).not.toContain('Checking your session')
    expect(text).not.toContain('Submit synthetic transaction')
    expect(fetchSpy).not.toHaveBeenCalled()
    expect(text).not.toMatch(/[-—–]/)
  })

  it('walks crossing, policy, attestation, and delivery without a private API call', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const wrapper = mount(PublicExplorerView)
    await flushPromises()

    await wrapper.get('[data-testid="explorer-search"]').setValue('replay')
    expect(wrapper.text()).toContain('Replay rejected')
    expect(wrapper.text()).not.toContain('QuayForge order accepted')

    await wrapper.get('[data-testid="explorer-filter"]').setValue('QUARANTINE')
    await wrapper.get('[data-testid="explorer-search"]').setValue('')
    const quarantine = wrapper.findAll('button.crossing').find((button) => button.text().includes('Contract drift held'))
    expect(quarantine).toBeTruthy()
    await quarantine!.trigger('click')

    const crossingTab = wrapper.findAll('button[role="tab"]').find((tab) => tab.text() === 'Crossing')
    await crossingTab!.trigger('click')
    expect(wrapper.text()).toContain('Incoming crossing')
    expect(wrapper.text()).toContain('Source identity')
    expect(wrapper.text()).toContain('Authorization')
    expect(wrapper.text()).toContain('Authenticity')
    expect(wrapper.text()).toContain('Freshness')
    expect(wrapper.text()).toContain('Replay detection')
    expect(wrapper.text()).toContain('Contract validation')

    await wrapper.findAll('button[role="tab"]').find((tab) => tab.text() === 'Policy')!.trigger('click')
    expect(wrapper.get('[data-testid="policy-decision"]').text()).toContain('QUARANTINE')
    expect(wrapper.text()).toContain('Transformation')

    await wrapper.findAll('button[role="tab"]').find((tab) => tab.text() === 'Evidence')!.trigger('click')
    expect(wrapper.get('[data-testid="attestation"]').text()).toContain('Exchange attestation')
    expect(wrapper.text()).toContain('Cloud KMS')
    expect(wrapper.get('[data-testid="timeline"]').text()).toContain('Contract validation failed')

    await wrapper.findAll('button[role="tab"]').find((tab) => tab.text() === 'Delivery')!.trigger('click')
    expect(wrapper.get('[data-testid="delivery-state"]').text()).toContain('Held')
    expect(wrapper.text()).toContain('Worker processing')
    expect(wrapper.text()).not.toMatch(/[-—–]/)
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('keeps real operator actions disabled and unable to call the network', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const wrapper = mount(PublicExplorerView)
    await flushPromises()
    const locked = wrapper.findAll('button.locked')
    expect(locked.map((button) => button.text())).toEqual([
      'Acknowledge hold',
      'Retry delivery',
      'Approve release',
      'Reject crossing',
    ])
    for (const button of locked) {
      expect(button.attributes('disabled')).toBeDefined()
      await button.trigger('click')
    }
    expect(fetchSpy).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Real operator actions require authentication')
    expect(wrapper.text()).not.toMatch(/[-—–]/)
  })

  it('filters the fixture layer and does not import private modules', () => {
    expect(filterPortfolioCrossings(PORTFOLIO_EXPLORER_CROSSINGS, '4114', 'ALL')).toHaveLength(1)
    expect(filterPortfolioCrossings(PORTFOLIO_EXPLORER_CROSSINGS, '', 'REJECT').map((item) => item.id)).toEqual(['4126'])
    const files = [
      'src/views/PublicExplorerView.vue',
      'src/domain/portfolioExplorer.ts',
      'src/publicEntry.ts',
    ]
    for (const file of files) {
      const source = readFileSync(resolve(file), 'utf8')
      expect(source).not.toContain('@/api/')
      expect(source).not.toContain('cloudSession')
      expect(source).not.toContain('firebase')
    }
    const operator = readFileSync(resolve('src/views/CloudConsoleView.vue'), 'utf8')
    expect(operator).toContain("authState.value !== 'authenticated'")
    expect(operator).toContain('initializeCloudSession')
  })

  it('keeps project context in an optional panel and shows the current date', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 9, 4, 15, 0, 0))
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const wrapper = mount(PublicExplorerView)
    await flushPromises()
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    await wrapper.get('nav button').trigger('click')
    const dialog = wrapper.get('[role="dialog"]')
    expect(dialog.text()).toContain('About the project')
    expect(dialog.text()).toContain('The problem')
    expect(dialog.text()).toContain('What I built')
    expect(dialog.text()).toContain('Architecture')
    expect(dialog.text()).toContain('Security model')
    expect(dialog.text()).toContain('Technology stack')
    expect(dialog.text()).toContain('Synthetic data')
    expect(dialog.text()).toContain('not customer traffic')
    expect(wrapper.get('[data-testid="portfolio-date"]').text()).toBe(formatPortfolioDate(new Date(2026, 9, 4, 15, 0, 0)))
    expect(wrapper.find('[data-testid="explorer-search"]').exists()).toBe(true)
    expect(dialog.text()).not.toMatch(/[-—–]/)
    await wrapper.get('.about__close').trigger('click')
    expect(wrapper.find('[role="dialog"]').exists()).toBe(false)
    expect(fetchSpy).not.toHaveBeenCalled()
  })
})
