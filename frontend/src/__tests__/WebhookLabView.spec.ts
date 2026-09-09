import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import WebhookLabView from '@/views/WebhookLabView.vue'
import { routes } from '@/router'
import { allowResponse } from './fixtures'
import { server } from './mswServer'

async function mountLab() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/webhooks')
  await router.isReady()
  return mount(WebhookLabView, {
    global: { plugins: [pinia, router] },
  })
}

describe('WebhookLabView', () => {
  it('renders replay as a security reject, not a duplicate delivery', async () => {
    server.use(
      http.post('/api/v1/dev/webhook-lab/:scenario', () =>
        HttpResponse.json({
          ...allowResponse,
          outcome: 'REJECT',
          ingress_kind: 'SIGNED_WEBHOOK',
          duplicate_delivery: false,
          reason_codes: [{ code: 'REPLAY_DETECTED', description: 'Nonce reused.' }],
          controls: [
            {
              kind: 'AUTHENTICITY',
              status: 'PASS',
              reasonCodes: [],
              safeExplanation: 'HMAC-SHA256 signature matched the raw body.',
            },
            {
              kind: 'FRESHNESS',
              status: 'PASS',
              reasonCodes: [],
              safeExplanation: 'Timestamp is inside the accepted window.',
            },
            {
              kind: 'REPLAY',
              status: 'FAIL',
              reasonCodes: ['REPLAY_DETECTED'],
              safeExplanation: 'A previously used nonce was reused.',
            },
          ],
        }),
      ),
    )
    const wrapper = await mountLab()
    await wrapper.get('input[value="replay"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Replay Defense')
    expect(wrapper.text()).toContain('A previously used nonce was reused.')
    expect(wrapper.text()).toContain('REJECT')
    expect(wrapper.text()).not.toContain('Legitimate duplicate delivery')
  })

  it('labels a legitimate duplicate without calling it a replay attack', async () => {
    server.use(
      http.post('/api/v1/dev/webhook-lab/:scenario', () =>
        HttpResponse.json({
          ...allowResponse,
          ingress_kind: 'SIGNED_WEBHOOK',
          duplicate_delivery: true,
          controls: [
            {
              kind: 'REPLAY',
              status: 'PASS',
              reasonCodes: [],
              safeExplanation: 'No prior nonce within the protected replay window.',
            },
          ],
        }),
      ),
    )
    const wrapper = await mountLab()
    await wrapper.get('input[value="duplicate"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Legitimate duplicate delivery')
    expect(wrapper.text()).toContain('Replay attack: no')
    expect(wrapper.text()).toContain('ALLOW')
  })
})
