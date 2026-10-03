import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import PublicLandingView from '@/views/PublicLandingView.vue'
import { ROUTE_NAMES, routes } from '@/router'
import { formatPortfolioDate } from '@/domain/portfolioDate'

function renderedText() {
  const wrapper = mount(PublicLandingView)
  return wrapper
}

describe('public portfolio landing', () => {
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('renders the portfolio without auth, and does not call the network', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const wrapper = renderedText()
    await flushPromises()
    const text = wrapper.text()
    expect(text).toContain('BoundaryMesh')
    expect(text).toContain('Secure every system crossing.')
    expect(text).toContain('What I built')
    expect(text).toContain('Architecture')
    expect(text).toContain('Example crossing')
    expect(text).toContain('Engineering highlights')
    expect(text).toContain('Tech stack')
    expect(text).toContain('Live portfolio status')
    expect(text).toContain('Security and delivery')
    expect(text).toContain('Testing')
    expect(text).toContain('Project scope')
    expect(text).toContain('Source on GitHub')
    expect(text).toContain('Current date')
    expect(wrapper.get('[data-testid="portfolio-date"]').text()).toBe(formatPortfolioDate(new Date()))
    expect(wrapper.find('button').exists()).toBe(false)
    expect(text).not.toContain('Checking your session')
    expect(text).not.toContain('secure web edge is unavailable')
    expect(wrapper.find('a[href="/operator"]').exists()).toBe(true)
    expect(wrapper.get('a[href="https://github.com/Adellaghmari/boundarymesh"]').text()).toBe('Source on GitHub')
    expect(fetchSpy).not.toHaveBeenCalled()
    expect(text).not.toMatch(/[-—–]/)
  })

  it('updates the displayed date when the local day changes', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 9, 4, 15, 0, 0))
    const wrapper = mount(PublicLandingView)
    expect(wrapper.get('[data-testid="portfolio-date"]').text()).toBe('Sunday, 4 October 2026')
    vi.setSystemTime(new Date(2026, 9, 5, 0, 30, 0))
    await vi.advanceTimersByTimeAsync(9 * 60 * 60 * 1000)
    expect(wrapper.get('[data-testid="portfolio-date"]').text()).toBe('Monday, 5 October 2026')
    expect(wrapper.text()).not.toMatch(/[-—–]/)
    wrapper.unmount()
  })

  it('keeps the local lab router intact and the hosted documents split', () => {
    expect(routes.map((route) => route.name)).toEqual([
      ROUTE_NAMES.overview,
      ROUTE_NAMES.boundaryEvents,
      ROUTE_NAMES.boundaryEventDetail,
      ROUTE_NAMES.connections,
      ROUTE_NAMES.architecture,
      ROUTE_NAMES.identity,
      ROUTE_NAMES.webhooks,
      ROUTE_NAMES.contracts,
      ROUTE_NAMES.dataBoundary,
      ROUTE_NAMES.crossing,
      ROUTE_NAMES.delivery,
      ROUTE_NAMES.credentials,
      ROUTE_NAMES.quarantine,
      ROUTE_NAMES.quarantineDetail,
      ROUTE_NAMES.audit,
      ROUTE_NAMES.auditDetail,
      ROUTE_NAMES.attestations,
      ROUTE_NAMES.attestationDetail,
      ROUTE_NAMES.boundaryGraph,
      ROUTE_NAMES.status,
      ROUTE_NAMES.notFound,
    ])

    const main = readFileSync(resolve('src/main.ts'), 'utf8')
    const operator = readFileSync(resolve('src/operator-main.ts'), 'utf8')
    const landing = readFileSync(resolve('src/views/PublicLandingView.vue'), 'utf8')
    const firebase = JSON.parse(readFileSync(resolve('../firebase.json'), 'utf8'))

    expect(main).toContain("import.meta.env.VITE_APP_MODE === 'cloud'")
    expect(main).toContain('PublicLandingView.vue')
    expect(main).toContain('App.vue')
    expect(main).not.toContain('CloudConsoleView')
    expect(operator).toContain('CloudConsoleView')
    expect(operator).not.toContain('App.vue')
    expect(operator).not.toContain('PublicLandingView')
    expect(landing).not.toContain("@/api/")
    expect(landing).not.toContain("@/stores/")
    expect(firebase.hosting.rewrites.map((rule: { source: string }) => rule.source)).toEqual([
      '/session/**',
      '/api/**',
      '/operator',
      '/operator/**',
      '**',
    ])
    expect(firebase.hosting.rewrites[2].destination).toBe('/operator.html')
    expect(firebase.hosting.rewrites.at(-1).destination).toBe('/index.html')
  })
})
