import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import App from '@/App.vue'
import { routes } from '@/router'

describe('router', () => {
  it('renders the 404 surface for unknown paths', async () => {
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/this-route-does-not-exist')
    await router.isReady()
    const wrapper = mount(App, {
      global: { plugins: [createPinia(), router] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Page not found')
    expect(wrapper.text()).toContain('Return to overview')
  })
})
