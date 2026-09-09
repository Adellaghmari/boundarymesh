import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import AuthnAuthzPanel from '@/components/AuthnAuthzPanel.vue'

describe('AuthnAuthzPanel', () => {
  it('keeps authentication and authorization as separate cards', () => {
    const wrapper = mount(AuthnAuthzPanel, {
      props: {
        identity: {
          status: 'PASS',
          issuer: 'http://127.0.0.1:8080/local-issuer',
          principal: 'app-quayforge-connector',
          explanation: 'Issuer accepted. Audience matches.',
        },
        authorization: {
          status: 'FAIL',
          grantedScopes: ['boundary.read'],
          explanation: 'Required permission boundary.evaluate missing.',
        },
      },
    })

    expect(wrapper.text()).toContain('Authentication')
    expect(wrapper.text()).toContain('Authorization')
    expect(wrapper.text()).toContain('Issuer accepted. Audience matches.')
    expect(wrapper.text()).toContain('Required permission boundary.evaluate missing.')
    expect(wrapper.text()).toContain('boundary.read')
    expect(wrapper.get('#authn-heading').text()).toBe('Authentication')
    expect(wrapper.get('#authz-heading').text()).toBe('Authorization')
  })
})
