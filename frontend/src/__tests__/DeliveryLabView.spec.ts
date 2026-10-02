import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import DeliveryLabView from '@/views/DeliveryLabView.vue'
import { routes } from '@/router'

async function mountLab() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/delivery')
  await router.isReady()
  return mount(DeliveryLabView, {
    global: { plugins: [pinia, router] },
  })
}

describe('DeliveryLabView', () => {
  it('shows transformed delivery stages and never renders a secret marker', async () => {
    const wrapper = await mountLab()
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('ALLOW WITH TRANSFORMATION')
    expect(wrapper.text()).toContain('DELIVERED')
    expect(wrapper.text()).toContain('sink.lumenway.fulfillment')
    expect(wrapper.text()).toContain('Lumenway credential v1')
    expect(wrapper.text()).toContain('[REDACTED]')
    expect(wrapper.text()).toContain('Circuit CLOSED')
    expect(wrapper.text()).not.toContain('TEST-SECRET-')
    expect(wrapper.get('[aria-label="Approved delivery body"]').text()).not.toContain(
      'ada.lovelace@example.test',
    )
  })

  it('shows consent failure without a Camel route', async () => {
    const wrapper = await mountLab()
    await wrapper.get('input[value="consent-missing"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('QUARANTINE')
    expect(wrapper.text()).toContain('MISSING')
    expect(wrapper.text()).toContain('not routed')
    expect(wrapper.text()).toContain('provider called no')
    expect(wrapper.text()).not.toContain('TEST-SECRET-')
  })

  it('shows circuit-open and duplicate-suppressed outcomes', async () => {
    const wrapper = await mountLab()
    await wrapper.get('input[value="circuit-open"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Circuit OPEN')
    expect(wrapper.text()).toContain('QUARANTINE')

    await wrapper.get('input[value="duplicate"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('DUPLICATE_SUPPRESSED')
    expect(wrapper.text()).toContain('provider called no')
  })
})
