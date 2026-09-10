import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import AuditChainDetailView from '@/views/AuditChainDetailView.vue'
import { routes } from '@/router'

async function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/audit/quayforge-orders')
  await router.isReady()
  const wrapper = mount(AuditChainDetailView, {
    props: { integrationId: 'quayforge-orders' },
    global: { plugins: [pinia, router] },
  })
  await flushPromises()
  return wrapper
}

describe('AuditChainDetailView', () => {
  it('renders sequence hashes and verifies via the Java API', async () => {
    const wrapper = await mountView()
    expect(wrapper.text()).toContain('BOUNDARY_DECISION')
    expect(wrapper.text()).toContain('aaaaaaaaaaaa')
    expect(wrapper.text()).not.toContain('TEST-SECRET')
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="audit-verify"]').text()).toContain('VALID')
  })

  it('runs an isolated synthetic tamper demo', async () => {
    const wrapper = await mountView()
    await wrapper.findAll('button.action')[1]!.trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="audit-tamper"]').text()).toContain('SYNTHETIC DEMO')
    expect(wrapper.get('[data-testid="audit-tamper"]').text()).toContain('HASH_MISMATCH')
  })
})
