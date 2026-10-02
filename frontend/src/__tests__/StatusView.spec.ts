import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import StatusView from '@/views/StatusView.vue'

describe('StatusView', () => {
  it('separates verified cloud capabilities from the local lab console', () => {
    const wrapper = mount(StatusView)
    expect(wrapper.text()).toContain('Vue frontend')
    expect(wrapper.text()).toContain('TESTED')
    expect(wrapper.text()).toContain('Identity Federation Gateway')
    expect(wrapper.text()).toContain('M365 / Graph')
    expect(wrapper.text()).toContain('TESTED')
    expect(wrapper.findAll('[data-status="LIVE VERIFIED"]')).toHaveLength(7)
    expect(wrapper.text()).toContain('Firebase Hosting')
    expect(wrapper.text()).not.toContain('Firebase Hosting is not deployed')
  })
})
