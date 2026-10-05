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
    expect(wrapper.get('[data-testid="identity-test"]').text()).toContain('SECURITY CHECK')
    expect(wrapper.get('[data-testid="identity-test"]').text()).toContain('PASSED')
    expect(wrapper.text()).toContain('Authentication')
    expect(wrapper.text()).toContain('Authorization')
    expect(wrapper.text()).toContain('Required permission boundary.evaluate missing.')
    expect(wrapper.text()).toContain('REJECT')
    expect(wrapper.find('[data-testid="authn-fail"]').exists()).toBe(false)
  })

  it.each([
    [
      'EXPIRED',
      'The identity token is no longer valid.',
      'The request was stopped before a crossing was created.',
    ],
    [
      'WRONG_ISSUER',
      'The token issuer is not trusted by BoundaryMesh.',
      'The request was stopped at authentication.',
    ],
    [
      'WRONG_AUDIENCE',
      'The token audience does not match BoundaryMesh.',
      'The crossing was blocked before authorization and policy evaluation.',
    ],
  ])(
    'presents a correct rejection for %s as a passed security check',
    async (scenarioId, reason, boundaryAction) => {
      server.use(
        http.post('/api/v1/identity/evaluate', () =>
          HttpResponse.json({ error_code: 'UNAUTHORIZED', message: 'Authentication failed.' }, { status: 401 }),
        ),
      )
      const wrapper = await mountLab()
      await wrapper.get(`input[value="${scenarioId}"]`).setValue(true)
      await wrapper.get('button.action').trigger('click')
      await flushPromises()
      const test = wrapper.get('[data-testid="identity-test"]')
      expect(test.text()).toContain('SECURITY CHECK')
      expect(test.text()).toContain('PASSED')
      expect(test.text()).toContain('Observed result')
      expect(test.text()).toContain('HTTP 401 Unauthorized')
      expect(test.text()).toContain('Reason')
      expect(test.text()).toContain(reason)
      expect(test.text()).toContain('Boundary action')
      expect(test.text()).toContain(boundaryAction)
      expect(wrapper.find('article.result').exists()).toBe(false)
      expect(wrapper.find('[data-testid="authn-fail"]').exists()).toBe(false)
    },
  )

  it('fails when an invalid identity is unexpectedly accepted', async () => {
    server.use(
      http.post('/api/v1/identity/evaluate', () => HttpResponse.json(allowResponse)),
    )
    const wrapper = await mountLab()
    await wrapper.get('input[value="WRONG_AUDIENCE"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    const test = wrapper.get('[data-testid="identity-test"]')
    expect(test.text()).toContain('FAILED')
    expect(test.text()).toContain('The invalid identity was unexpectedly accepted.')
    expect(test.text()).toContain('The observed response does not match the security contract.')
  })

  it('fails when the identity service returns an unexpected server error', async () => {
    server.use(
      http.post('/api/v1/identity/evaluate', () =>
        HttpResponse.json({ error_code: 'SERVER', message: 'boom' }, { status: 500 }),
      ),
    )
    const wrapper = await mountLab()
    await wrapper.get('input[value="EXPIRED"]').setValue(true)
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    const test = wrapper.get('[data-testid="identity-test"]')
    expect(test.text()).toContain('FAILED')
    expect(test.text()).toContain('HTTP 500 Server Error')
    expect(test.text()).toContain('An unexpected system or server error occurred.')
  })
})
