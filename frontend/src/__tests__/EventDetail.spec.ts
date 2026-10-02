import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import App from '@/App.vue'
import { routes } from '@/router'
import { useEventsStore } from '@/stores/events'
import { sessionEvent } from './fixtures'

async function mountAt(path: string) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const store = useEventsStore()
  store.events.push(sessionEvent)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(App, {
    global: { plugins: [pinia, router] },
  })
  return wrapper
}

describe('boundary event detail route', () => {
  it('renders session evaluation details', async () => {
    const wrapper = await mountAt(`/boundary-events/${sessionEvent.transactionId}`)
    await flushPromises()
    expect(wrapper.text()).toContain('Recorded crossing')
    expect(wrapper.text()).toContain('ALLOW')
    expect(wrapper.text()).toContain('Helix 365')
    expect(wrapper.text()).toContain('CoveCRM')
    expect(wrapper.text()).toContain('SYNTHETIC DEMO')
  })

  it('explains missing session events without inventing persistence', async () => {
    const wrapper = await mountAt('/boundary-events/00000000-0000-0000-0000-000000000000')
    await flushPromises()
    expect(wrapper.text()).toContain('Not in this session')
    expect(wrapper.text()).toContain('no persistent event store')
  })
})
