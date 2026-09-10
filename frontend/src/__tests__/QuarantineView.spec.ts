import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import QuarantineDetailView from '@/views/QuarantineDetailView.vue'
import QuarantineView from '@/views/QuarantineView.vue'
import { routes } from '@/router'

describe('Quarantine UI', () => {
  it('lists a recoverable item with eligibility', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/quarantine')
    await router.isReady()
    const wrapper = mount(QuarantineView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('LOCAL / SYNTHETIC SESSION DATA')
    expect(wrapper.text()).toContain('DELIVERY_RETRIES_EXHAUSTED')
    expect(wrapper.text()).toContain('OPEN')
    expect(wrapper.text()).toContain('yes')
    expect(wrapper.text()).not.toContain('TEST-SECRET-')
  })

  it('shows reprocess eligibility and resolved recovery', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/quarantine/q-100')
    await router.isReady()
    const wrapper = mount(QuarantineDetailView, {
      props: { id: 'q-100' },
      global: { plugins: [pinia, router] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Recovery eligible')
    expect(wrapper.text()).toContain('yes')
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('RESOLVED')
    expect(wrapper.text()).toContain('DELIVERED')
    expect(wrapper.text()).not.toContain('TEST-SECRET-')
    expect(wrapper.text()).not.toContain('eyJ')
  })
})
