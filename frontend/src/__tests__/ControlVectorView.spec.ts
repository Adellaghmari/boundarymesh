import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ControlVectorView from '@/components/ControlVectorView.vue'
import type { ControlResult } from '@/domain/types'

const controls: ControlResult[] = [
  {
    kind: 'IDENTITY',
    status: 'FAIL',
    reasonCodes: ['IDENTITY_INVALID'],
    safeExplanation: 'Synthetic identity is invalid for this crossing.',
  },
  {
    kind: 'CONNECTOR_SAFETY',
    status: 'UNAVAILABLE',
    reasonCodes: ['CONNECTOR_UNAVAILABLE'],
    safeExplanation: 'Synthetic destination connector is unavailable.',
  },
]

describe('ControlVectorView', () => {
  it('renders control statuses as text', () => {
    const wrapper = mount(ControlVectorView, { props: { controls } })
    expect(wrapper.text()).toContain('Identity')
    expect(wrapper.text()).toContain('FAIL')
    expect(wrapper.text()).toContain('Connector safety')
    expect(wrapper.text()).toContain('UNAVAILABLE')
    expect(wrapper.text()).toContain('Synthetic identity is invalid for this crossing.')
  })
})
