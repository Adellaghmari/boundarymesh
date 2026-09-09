import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import WebhookDefensePanel from '@/components/WebhookDefensePanel.vue'
import type { ControlResult } from '@/domain/types'

const controls: ControlResult[] = [
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
]

describe('WebhookDefensePanel', () => {
  it('renders authenticity, freshness, and replay defense separately', () => {
    const wrapper = mount(WebhookDefensePanel, { props: { controls } })
    expect(wrapper.text()).toContain('Authenticity')
    expect(wrapper.text()).toContain('Freshness')
    expect(wrapper.text()).toContain('Replay Defense')
    expect(wrapper.text()).toContain('A previously used nonce was reused.')
    expect(wrapper.text()).not.toContain('Legitimate duplicate delivery')
  })

  it('distinguishes legitimate duplicate delivery from replay', () => {
    const duplicateControls: ControlResult[] = [
      ...controls.filter((item) => item.kind !== 'REPLAY'),
      {
        kind: 'REPLAY',
        status: 'PASS',
        reasonCodes: [],
        safeExplanation: 'No prior nonce within the protected replay window.',
      },
    ]
    const wrapper = mount(WebhookDefensePanel, {
      props: { controls: duplicateControls, duplicateDelivery: true },
    })
    expect(wrapper.text()).toContain('Legitimate duplicate delivery')
    expect(wrapper.text()).toContain('Replay attack: no')
    expect(wrapper.text()).toContain('Side effect: not repeated')
  })
})
