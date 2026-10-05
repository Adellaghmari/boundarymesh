import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import ConnectionsView from '@/views/ConnectionsView.vue'
import { routes } from '@/router'

async function mountConnections() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/connections')
  await router.isReady()
  return mount(ConnectionsView, { global: { plugins: [pinia, router] } })
}

describe('ConnectionsView', () => {
  it('keeps the Microsoft 365 contract harness and opens synthetic connection details', async () => {
    const wrapper = await mountConnections()
    await flushPromises()
    expect(wrapper.text()).toContain('Helix 365 is not Microsoft 365')
    const harness = wrapper.get('[data-testid="m365-connector"]')
    expect(harness.text()).toContain('Run contract harness crossing')

    const helix = wrapper.get('[data-testid="connection-helix-365"]')
    expect(helix.text()).not.toContain('View connection details is Microsoft')
    await helix.get('button').trigger('click')
    expect(helix.text()).toContain('Synthetic evaluate only')
    expect(helix.text()).toContain('Helix 365')
    expect(helix.text()).toContain('CoveCRM')
    expect(helix.text()).toContain('This is not a Microsoft Graph tenant.')
    expect(helix.text()).toContain('No live connector is called.')
    expect(helix.get('a[href="/contracts"]').text()).toBe('View contract')
    expect(helix.get('a[href="/data-boundary"]').text()).toBe('Inspect policy path')
    expect(helix.get('a[href="/crossing"]').text()).toBe('View synthetic flow')

    const lumenway = wrapper.get('[data-testid="connection-lumenway"]')
    await lumenway.get('button').trigger('click')
    expect(lumenway.text()).toContain('Not enabled')
    expect(lumenway.text()).toContain('No live connector exists.')
    expect(lumenway.text()).toContain('Planned')
  })
})
