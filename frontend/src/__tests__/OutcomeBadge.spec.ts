import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import OutcomeBadge from '@/components/OutcomeBadge.vue'

describe('OutcomeBadge', () => {
  it('renders outcome text so meaning is not color-only', () => {
    const wrapper = mount(OutcomeBadge, { props: { outcome: 'REJECT' } })
    expect(wrapper.text()).toContain('REJECT')
    expect(wrapper.attributes('data-outcome')).toBe('REJECT')
  })

  it('renders transformation outcome label', () => {
    const wrapper = mount(OutcomeBadge, { props: { outcome: 'ALLOW_WITH_TRANSFORMATION' } })
    expect(wrapper.text()).toContain('ALLOW WITH TRANSFORMATION')
  })
})
