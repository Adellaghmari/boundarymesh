import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import AttestationDetailView from '@/views/AttestationDetailView.vue'
import { routes } from '@/router'

async function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/attestations/att-test-1')
  await router.isReady()
  return mount(AttestationDetailView, {
    props: { id: 'att-test-1' },
    global: { plugins: [pinia, router] },
  })
}

describe('AttestationDetailView', () => {
  it('shows signed attestation metadata and Java verification', async () => {
    const wrapper = await mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('QUAYFORGE')
    expect(wrapper.text()).toContain('COVECRM')
    expect(wrapper.get('[data-testid="attestation-status"]').text()).toContain('SIGNED')
    expect(wrapper.text()).toContain('Local signing key')
    expect(wrapper.text()).not.toContain('TEST-SECRET')
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="attestation-verify"]').text()).toContain('VALID')
  })

  it('shows isolated attestation tamper as INVALID', async () => {
    const wrapper = await mountView()
    await flushPromises()
    await wrapper.findAll('button.action')[1]!.trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="attestation-tamper"]').text()).toContain('INVALID')
  })
})
