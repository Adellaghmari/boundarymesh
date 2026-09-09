import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusView from '@/views/StatusView.vue'

describe('StatusView', () => {
  it('uses honest capability labels and does not mark anything LIVE VERIFIED', () => {
    const wrapper = mount(StatusView)
    expect(wrapper.text()).toContain('Vue frontend')
    expect(wrapper.text()).toContain('TESTED')
    expect(wrapper.text()).toContain('Identity Federation Gateway')
    expect(wrapper.text()).toContain('M365 / Graph')
    expect(wrapper.text()).toContain('PLANNED')
    expect(wrapper.findAll('[data-status="LIVE VERIFIED"]')).toHaveLength(0)
  })
})
