import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import ContractDriftLabView from '@/views/ContractDriftLabView.vue'
import { routes } from '@/router'
import { allowResponse } from './fixtures'
import { server } from './mswServer'

async function mountLab() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/contracts')
  await router.isReady()
  return mount(ContractDriftLabView, {
    global: { plugins: [pinia, router] },
  })
}

describe('ContractDriftLabView', () => {
  it('renders safe violation paths without raw payload values', async () => {
    server.use(
      http.post('/api/v1/dev/contract-lab/:scenario', () =>
        HttpResponse.json({
          ...allowResponse,
          outcome: 'REJECT',
          classification: 'SCHEMA_VIOLATION',
          contract: {
            id: 'quayforge-orders',
            version: 'v1',
            type: 'JSON_SCHEMA',
            status: 'SUPPORTED',
            classification: 'SCHEMA_VIOLATION',
            truncated_count: 0,
            violations: [{ path: '/amount', keyword: 'type', expected: 'number', actual: 'string' }],
          },
        }),
      ),
    )
    const wrapper = await mountLab()
    await wrapper.get('input[value="wrong-type"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('/amount')
    expect(wrapper.text()).toContain('SCHEMA_VIOLATION')
    expect(wrapper.text()).toContain('REJECT')
    expect(wrapper.text()).not.toContain('nineteen')
    expect(wrapper.text()).not.toContain('@example')
  })

  it('shows unsupported version as quarantine', async () => {
    server.use(
      http.post('/api/v1/dev/contract-lab/:scenario', () =>
        HttpResponse.json({
          ...allowResponse,
          outcome: 'QUARANTINE',
          classification: 'CONTRACT_DRIFT',
          reason_codes: [{ code: 'CONTRACT_DRIFT_DETECTED', description: 'Declared v1 matches v2.' }],
          contract: {
            id: 'quayforge-orders',
            version: 'v1',
            classification: 'CONTRACT_DRIFT',
            truncated_count: 0,
            violations: [],
          },
        }),
      ),
    )
    const wrapper = await mountLab()
    await wrapper.get('input[value="contract-drift"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('QUARANTINE')
    expect(wrapper.text()).toContain('CONTRACT_DRIFT')
  })
})
