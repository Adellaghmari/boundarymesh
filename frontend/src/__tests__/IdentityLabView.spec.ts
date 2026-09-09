import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import IdentityLabView from '@/views/IdentityLabView.vue'
import { routes } from '@/router'
import { allowResponse } from './fixtures'
import { server } from './mswServer'

async function mountLab() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/identity')
  await router.isReady()
  return mount(IdentityLabView, {
    global: { plugins: [pinia, router] },
  })
}

describe('IdentityLabView', () => {
  it('renders AuthN PASS and AuthZ FAIL without collapsing them', async () => {
    server.use(
      http.post('/api/v1/identity/evaluate', () =>
        HttpResponse.json({
          ...allowResponse,
          outcome: 'REJECT',
          ingress_kind: 'TOKEN_PROTECTED_API',
          reason_codes: [{ code: 'AUTHZ_MISSING_SCOPE', description: 'Missing evaluate permission.' }],
          identity: {
            status: 'PASS',
            explanation: 'Issuer accepted. Audience matches.',
          },
          authorization: {
            status: 'FAIL',
            grantedScopes: ['boundary.read'],
            explanation: 'Required permission boundary.evaluate missing.',
          },
        }),
      ),
    )
    const wrapper = await mountLab()
    await wrapper.get('input[value="MISSING_PERMISSION"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('Authentication')
    expect(wrapper.text()).toContain('Authorization')
    expect(wrapper.text()).toContain('Required permission boundary.evaluate missing.')
    expect(wrapper.text()).toContain('REJECT')
    expect(wrapper.find('[data-testid="authn-fail"]').exists()).toBe(false)
  })

  it('explains HTTP 401 as authentication that could not be established', async () => {
    server.use(
      http.post('/api/v1/identity/evaluate', () =>
        HttpResponse.json({ error_code: 'UNAUTHORIZED', message: 'Authentication failed.' }, { status: 401 }),
      ),
    )
    const wrapper = await mountLab()
    await wrapper.get('input[value="EXPIRED"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="authn-fail"]').text()).toContain('Authentication could not be established')
    expect(wrapper.text()).toContain('HTTP 401')
    expect(wrapper.find('article.result').exists()).toBe(false)
  })
})
