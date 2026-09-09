import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import DataBoundaryLabView from '@/views/DataBoundaryLabView.vue'
import { routes } from '@/router'

async function mountLab() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/data-boundary')
  await router.isReady()
  return mount(DataBoundaryLabView, {
    global: { plugins: [pinia, router] },
  })
}

describe('DataBoundaryLabView', () => {
  it('shows field actions and before/after without treating contract as egress', async () => {
    const wrapper = await mountLab()
    await wrapper.get('input[value="email-only"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('ALLOW WITH TRANSFORMATION')
    expect(wrapper.text()).toContain('/email')
    expect(wrapper.text()).toContain('REDACT')
    expect(wrapper.text()).toContain('[REDACTED]')
    expect(wrapper.text()).toContain('does not automatically mean data egress is allowed')
    expect(wrapper.text()).toContain('Before / after')
  })
})
