import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import CrossingLabView from '@/views/CrossingLabView.vue'
import { routes } from '@/router'

async function mountLab() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/crossing')
  await router.isReady()
  return mount(CrossingLabView, {
    global: { plugins: [pinia, router] },
  })
}

describe('CrossingLabView', () => {
  it('renders ordered crossing stages and the four-outcome transform path', async () => {
    const wrapper = await mountLab()
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Identity')
    expect(wrapper.text()).toContain('Authorization')
    expect(wrapper.text()).toContain('Authenticity')
    expect(wrapper.text()).toContain('Freshness')
    expect(wrapper.text()).toContain('Replay Defense')
    expect(wrapper.text()).toContain('Contract')
    expect(wrapper.text()).toContain('Data Boundary')
    expect(wrapper.text()).toContain('ALLOW WITH TRANSFORMATION')
    expect(wrapper.text()).toContain('Final outcome')
  })
})
