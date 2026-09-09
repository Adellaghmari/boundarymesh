import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import DecisionResult from '@/components/DecisionResult.vue'
import { routes } from '@/router'
import { sessionEvent } from './fixtures'
import type { BoundaryEventRecord } from '@/domain/types'

async function render(event: BoundaryEventRecord) {
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/')
  await router.isReady()
  return mount(DecisionResult, {
    props: { event },
    global: { plugins: [router] },
  })
}

describe('DecisionResult', () => {
  it('shows authentication and authorization separately for JWT events', async () => {
    const wrapper = await render({
      ...sessionEvent,
      ingressKind: 'TOKEN_PROTECTED_API',
      identity: {
        status: 'PASS',
        explanation: 'Issuer accepted. Audience matches.',
      },
      authorization: {
        status: 'FAIL',
        grantedScopes: [],
        explanation: 'Required permission boundary.evaluate missing.',
      },
      outcome: 'REJECT',
    })
    expect(wrapper.text()).toContain('Authentication')
    expect(wrapper.text()).toContain('Authorization')
    expect(wrapper.text()).toContain('Required permission boundary.evaluate missing.')
    expect(wrapper.text()).toContain('REJECT')
  })

  it('shows webhook defense cards and duplicate wording', async () => {
    const wrapper = await render({
      ...sessionEvent,
      ingressKind: 'SIGNED_WEBHOOK',
      duplicateDelivery: true,
      controlResults: [
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
          status: 'PASS',
          reasonCodes: [],
          safeExplanation: 'No prior nonce within the protected replay window.',
        },
      ],
    })
    expect(wrapper.text()).toContain('Authenticity')
    expect(wrapper.text()).toContain('Freshness')
    expect(wrapper.text()).toContain('Replay Defense')
    expect(wrapper.text()).toContain('Legitimate duplicate delivery')
    expect(wrapper.text()).toContain('SIGNED_WEBHOOK')
  })
})
