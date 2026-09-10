import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import CredentialLabView from '@/views/CredentialLabView.vue'
import { routes } from '@/router'

async function mountLab() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/credentials')
  await router.isReady()
  const wrapper = mount(CredentialLabView, {
    global: { plugins: [pinia, router] },
  })
  await flushPromises()
  return wrapper
}

describe('CredentialLabView', () => {
  it('renders credential references and versions without secret values', async () => {
    const wrapper = await mountLab()
    expect(wrapper.text()).toContain('LOCAL SECURITY MODEL / SYNTHETIC DEMO')
    expect(wrapper.text()).toContain('covecrm-api')
    expect(wrapper.text()).toContain('ACTIVE')
    expect(wrapper.text()).toContain('RETIRED')
    expect(wrapper.text()).toContain('activity.write')
    expect(wrapper.text()).not.toContain('TEST-SECRET-')
    expect(wrapper.text()).not.toContain('client_secret')
  })

  it('evaluates missing consent as a hold', async () => {
    const wrapper = await mountLab()
    await wrapper.get('input[value="consent-missing"]').setValue(true)
    await wrapper.get('[data-action="evaluate"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('QUARANTINE')
    expect(wrapper.text()).not.toContain('TEST-SECRET-')
  })
})
