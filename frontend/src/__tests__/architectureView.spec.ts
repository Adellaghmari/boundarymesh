import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ArchitectureView from '@/views/ArchitectureView.vue'

describe('ArchitectureView', () => {
  it('leads with implemented architecture and keeps missing scope secondary', () => {
    const wrapper = mount(ArchitectureView)
    const headings = wrapper.findAll('h2').map((heading) => heading.text())
    expect(headings[0]).toBe('Implemented architecture')
    expect(headings).toContain('Public demo scope')
    expect(headings).not.toContain('Not in this environment')

    expect(wrapper.text()).toContain('Boundary Enforcement Engine')
    expect(wrapper.text()).toContain('Private Cloud Run API and Worker Pool')

    const scope = wrapper.get('[data-testid="public-demo-scope"]')
    expect(scope.text()).toContain('Planned')
    expect(scope.text()).toContain('Future enhancement')
    expect(scope.text()).toContain('Intentionally disabled in public demo')
    expect(scope.text()).toContain('Not enabled in public environment')
    expect(scope.text()).toContain('Microsoft Graph change notifications')
    expect(scope.text()).toContain('Live external connector delivery')
    expect(scope.text()).not.toContain('Implemented')
  })
})
