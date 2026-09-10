import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import AppShell from '@/components/AppShell.vue'
import { routes } from '@/router'

async function mountShell(path: string) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(AppShell, {
    global: { plugins: [createPinia(), router] },
    attachTo: document.body,
  })
  await flushPromises()
  return { wrapper, router }
}

describe('premium navigation', () => {
  it('marks Overview as the current primary route', async () => {
    const { wrapper } = await mountShell('/')
    const current = wrapper.findAll('[aria-current="page"]')
    expect(current.some((item) => item.text().includes('Overview'))).toBe(true)
    expect(wrapper.get('[data-testid="synthetic-badge"]').text()).toContain('SYNTHETIC DEMO')
    expect(wrapper.get('[data-testid="auth-mode"]').text()).toContain('Recruiter mode')
    wrapper.unmount()
  })

  it('opens Labs and navigates to Identity Lab', async () => {
    const { wrapper, router } = await mountShell('/')
    const labs = wrapper.findAll('button').find((button) => button.text().includes('Labs'))
    expect(labs).toBeTruthy()
    await labs!.trigger('click')
    await flushPromises()
    expect(wrapper.get('#nav-menu-labs').isVisible()).toBe(true)
    const identity = wrapper.get('#nav-menu-labs a')
    expect(identity.attributes('href')).toBe('/identity')
    await identity.trigger('click')
    await router.push('/identity')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/identity')
    wrapper.unmount()
  })

  it('opens Operations and navigates to Quarantine', async () => {
    const { wrapper, router } = await mountShell('/')
    const operations = wrapper.findAll('button').find((button) => button.text().includes('Operations'))
    expect(operations).toBeTruthy()
    await operations!.trigger('click')
    await flushPromises()
    await wrapper.get('#nav-menu-operations a[href="/quarantine"]').trigger('click')
    await router.push('/quarantine')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/quarantine')
    wrapper.unmount()
  })

  it('closes Labs with Escape', async () => {
    const { wrapper } = await mountShell('/')
    const labs = wrapper.findAll('button').find((button) => button.text().includes('Labs'))
    await labs!.trigger('click')
    await flushPromises()
    expect(wrapper.get('#nav-menu-labs').isVisible()).toBe(true)
    await wrapper.get('.dropdown').trigger('keydown', { key: 'Escape' })
    await flushPromises()
    expect(wrapper.find('#nav-menu-labs').exists()).toBe(false)
    wrapper.unmount()
  })

  it('marks Evidence current on nested audit routes', async () => {
    const { wrapper } = await mountShell('/audit/quayforge-orders')
    const evidence = wrapper.findAll('button').find((button) => button.text().includes('Evidence'))
    expect(evidence?.classes()).toContain('is-current')
    wrapper.unmount()
  })

  it('opens the mobile drawer and lists grouped routes', async () => {
    const { wrapper } = await mountShell('/')
    await wrapper.get('[aria-label="Open navigation"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('#mobile-nav-title').text()).toBe('Navigate')
    expect(wrapper.text()).toContain('Identity Lab')
    expect(wrapper.text()).toContain('Quarantine')
    expect(wrapper.text()).toContain('Attestations')
    wrapper.unmount()
  })
})
