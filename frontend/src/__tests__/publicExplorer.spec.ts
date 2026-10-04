import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'
import { fetchAttestations, fetchHealth, fetchQuarantineList, reprocessQuarantine } from '@/api/syntheticPortfolio'
import { ABOUT_NAV, PRIMARY_NAV } from '@/config/navigation'
import { PORTFOLIO_EXPLORER_CROSSINGS } from '@/domain/portfolioExplorer'
import { selectPublicEntry } from '@/publicEntry'
import { routes } from '@/router'
import AboutProjectView from '@/views/AboutProjectView.vue'
import App from '@/App.vue'

describe('public product shell', () => {
  it('serves the shared console for every hosted path except the explore redirect', () => {
    expect(selectPublicEntry('/')).toBe('product')
    expect(selectPublicEntry('/about')).toBe('product')
    expect(selectPublicEntry('/identity')).toBe('product')
    expect(selectPublicEntry('/explore')).toBe('redirect-home')
    expect(selectPublicEntry('/explore/')).toBe('redirect-home')
    expect(PRIMARY_NAV.map((item) => item.label)).toEqual([
      'Overview',
      'Boundary Events',
      'Connections',
      'Architecture',
      'Boundary Graph',
      'Full Crossing',
    ])
    expect(ABOUT_NAV.label).toBe('About the project')
    expect(ABOUT_NAV.to).toBe('/about')
  })

  it('keeps the public bundle on the synthetic adapter', () => {
    const main = readFileSync(resolve('src/main.ts'), 'utf8')
    const facade = readFileSync(resolve('vite.config.ts'), 'utf8')
    const synthetic = readFileSync(resolve('src/api/syntheticPortfolio.ts'), 'utf8')
    expect(main).toContain('App.vue')
    expect(main).not.toContain('PublicExplorerView')
    expect(facade).toContain('syntheticPortfolio.ts')
    expect(facade).toContain('boundarymeshLive.ts')
    expect(synthetic).not.toContain("from '@/api/client'")
    expect(synthetic).not.toContain('firebase')

    const operator = readFileSync(resolve('src/operator-main.ts'), 'utf8')
    const shell = readFileSync(resolve('src/components/AppShell.vue'), 'utf8')
    const consoleSource = readFileSync(resolve('src/components/shell/SystemConsole.vue'), 'utf8')
    expect(main).not.toContain('installPublicDevtools')
    expect(main).not.toContain('hostDevtoolsOverlay')
    expect(operator).not.toContain('SystemConsole')
    expect(operator).not.toContain('hostDevtoolsOverlay')
    expect(shell).toContain('SystemConsole')
    expect(shell).toContain("VITE_APP_MODE === 'cloud'")
    expect(consoleSource).not.toContain('vue-devtools')
    expect(consoleSource).not.toContain('__devtools__')
    expect(consoleSource).not.toContain('__inspect')
    expect(facade).toContain('vueDevTools()')
    expect(facade).not.toContain('__devtools__')
    expect(facade).not.toContain('__VUE_PROD_DEVTOOLS__')
    expect(facade).not.toContain('copyPublicDevtoolsClient')
  })

  it('returns the four outcomes from memory', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const health = await fetchHealth()
    const quarantine = await fetchQuarantineList()
    const attestations = await fetchAttestations()
    expect(health.data.truth).toBe('SYNTHETIC DEMO')
    expect(quarantine.items.map((item) => item.id)).toContain('q4120')
    expect(attestations.items.map((item) => item.boundaryOutcome)).toEqual(
      PORTFOLIO_EXPLORER_CROSSINGS.map((crossing) => crossing.outcome),
    )
    await expect(reprocessQuarantine()).rejects.toMatchObject({ kind: 'forbidden' })
    expect(fetchSpy).not.toHaveBeenCalled()
    fetchSpy.mockRestore()
  })

  it('renders About the project inside the console', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/about')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [pinia, router] } })
    await flushPromises()
    const text = wrapper.text()
    expect(text).toContain('About the project')
    expect(text).toContain('What BoundaryMesh is')
    expect(text).toContain('The problem it solves')
    expect(text).toContain('What was built')
    expect(text).toContain('How a system crossing works')
    expect(text).toContain('Architecture')
    expect(text).toContain('Security model')
    expect(text).toContain('Technology stack')
    expect(text).toContain('Cloud architecture')
    expect(text).toContain('CI/CD and immutable deployment')
    expect(text).toContain('Synthetic demo explanation')
    expect(text).toContain('What is public versus protected')
    expect(text).toContain('System Console')
    expect(text).toContain('built in System Console')
    expect(text).toContain('synthetic runtime state')
    expect(text).toContain('without exposing private APIs or production authority')
    expect(text).toContain('operator sessions')
    expect(text).toContain('production logs')
    expect(text).toContain('real cloud state')
    expect(text).not.toContain('Vue DevTools')
    expect(text).toContain('Source on GitHub')
    expect(text).toContain('About the builder')
    expect(text).toContain('Boundary Events')
    expect(text).not.toContain('System crossings')
    expect(wrapper.get('a[href="https://github.com/Adellaghmari/boundarymesh"]').text()).toBe('Source on GitHub')
    expect(text).not.toMatch(/[-—–]/)
    wrapper.unmount()
  })

  it('keeps the about view on the shared visual system', () => {
    const source = readFileSync(resolve('src/views/AboutProjectView.vue'), 'utf8')
    expect(source).toContain('class="card stack"')
    expect(source).not.toContain('PublicExplorerView')
    const wrapper = mount(AboutProjectView)
    expect(wrapper.find('.card').exists()).toBe(true)
  })
})
