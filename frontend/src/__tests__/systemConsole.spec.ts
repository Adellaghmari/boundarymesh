import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'
import SystemConsole from '@/components/shell/SystemConsole.vue'
import { buildSystemConsole } from '@/domain/systemConsole'
import { routes } from '@/router'
import { useEventsStore } from '@/stores/events'
import { sessionEvent } from './fixtures'

describe('system console', () => {
  it('derives a synthetic runtime view from the selected crossing', () => {
    const model = buildSystemConsole({
      routeName: 'overview',
      events: [sessionEvent],
      selected: sessionEvent,
    })
    expect(model.runtimeMode).toBe('Synthetic demo')
    expect(model.routeLabel).toBe('Overview')
    expect(model.crossing).toBe('Valid crossing')
    expect(model.decision).toBe('ALLOW')
    expect(model.worker).toBe('Processed')
    expect(model.delivery).toBe('Completed')
    expect(model.attestation).toBe('Signed')
    expect(model.pipeline.map((stage) => stage.label)).toEqual([
      'Identity',
      'Authorization',
      'Authenticity',
      'Freshness',
      'Replay',
      'Contract',
      'Policy',
      'Transformation',
      'Attestation',
      'Delivery',
    ])
    expect(model.selectedLog.map((line) => line.message)).toContain('Identity verified')
    expect(model.log.map((line) => line.message)).toContain('Identity verified')
    expect(model.log.map((line) => line.message)).toContain('Policy result ALLOW')
    expect(model.log.map((line) => line.message)).toContain('Delivery completed')
    expect(model.stages.map((stage) => stage.label)).toEqual([
      'Authenticate',
      'Authorize',
      'Verify authenticity',
      'Check replay / freshness',
      'Validate contract',
      'Govern data',
      'Enforce boundary',
      'Resolve credential / consent',
      'Deliver or quarantine',
      'Record audit chain',
      'Sign exchange attestation',
    ])
    expect(model.groups.map((group) => group.id)).toEqual([
      'runtime',
      'events',
      'crossing',
      'policy',
      'delivery',
      'attestation',
      'route',
    ])
    expect(model.state.map((fact) => fact.value).join(' ')).not.toMatch(/[-—–]/)
    expect(model.log.map((line) => `${line.clock} ${line.message}`).join(' ')).not.toMatch(/[-—–]/)
    expect(model.stages.map((stage) => Object.values(stage).join(' ')).join(' ')).not.toMatch(/[-—–]/)
    expect(model.groups.flatMap((group) => group.fields.map((field) => `${field.key} ${field.value}`)).join(' ')).not.toMatch(/[-—–]/)
  })

  it('opens the docked inspector and follows a new synthetic decision', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const pinia = createPinia()
    setActivePinia(pinia)
    const store = useEventsStore()
    store.events.push(sessionEvent)
    store.lastEvent = sessionEvent
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/about')
    await router.isReady()
    const wrapper = mount(SystemConsole, { global: { plugins: [pinia, router] } })
    expect(wrapper.get('[data-testid="system-console"]').isVisible()).toBe(false)
    await wrapper.get('[data-testid="system-console-anchor"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="system-console-anchor"]').attributes('aria-expanded')).toBe('true')
    expect(wrapper.get('[data-testid="system-console"]').attributes('style') ?? '').not.toContain('display: none')
    expect(wrapper.get('[data-testid="inspector-rail"]').attributes('data-testid')).toBe('inspector-rail')
    expect(wrapper.get('[data-testid="inspector-split"]').attributes('data-testid')).toBe('inspector-split')
    expect(wrapper.get('.pill').text()).toBe('')
    expect(wrapper.text()).toContain('Synthetic demo')
    expect(wrapper.text()).toContain('About the project')
    expect(wrapper.text()).toContain('ALLOW')
    expect(wrapper.text()).not.toContain('Vue DevTools')
    expect(wrapper.text()).not.toContain('RouterView')
    expect(wrapper.text()).not.toMatch(/[-—–]/)

    store.lastEvent = {
      ...sessionEvent,
      outcome: 'REJECT',
      scenarioLabel: 'Invalid identity',
      appliedControls: { ...sessionEvent.appliedControls, IDENTITY: 'FAIL' },
    }
    store.events.push(store.lastEvent)
    await flushPromises()
    expect(wrapper.text()).toContain('REJECT')
    expect(wrapper.text()).toContain('Policy result REJECT')
    await wrapper.get('#system-console-tab-timeline').trigger('click')
    expect(wrapper.text()).toContain('Identity rejected')
    await wrapper.get('#system-console-tab-state').trigger('click')
    expect(wrapper.text()).toContain('readOnly')
    expect(wrapper.text()).toContain('unavailable')
    await wrapper.get('#system-console-tab-pipeline').trigger('click')
    expect(wrapper.text()).toContain('Authenticate')
    expect(wrapper.text()).toContain('Sign exchange attestation')
    await wrapper.get('#system-console-tab-routes').trigger('click')
    await wrapper.get('[data-testid="inspector-route-connections"]').trigger('click')
    await vi.waitFor(() => {
      expect(router.currentRoute.value.path).toBe('/connections')
    })
    await wrapper.get('#system-console-tab-system').trigger('click')
    expect(wrapper.text()).toContain('Private API unavailable')
    expect(wrapper.text()).toContain('Operator authority unavailable')
    expect(wrapper.text()).toContain('Read only')
    expect(fetchSpy).not.toHaveBeenCalled()
    fetchSpy.mockRestore()
    wrapper.unmount()
  })

  it('explains a public section in inspect mode without calling the network', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch')
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({ history: createMemoryHistory(), routes })
    await router.push('/')
    await router.isReady()
    const wrapper = mount(SystemConsole, { attachTo: document.body, global: { plugins: [pinia, router] } })
    const target = document.createElement('section')
    target.dataset.inspect = 'overview'
    document.body.appendChild(target)
    await wrapper.get('[data-testid="public-inspect-anchor"]').trigger('click')
    target.dispatchEvent(new PointerEvent('pointerover', { bubbles: true }))
    await flushPromises()
    const card = wrapper.get('[data-testid="public-inspect-card"]')
    expect(card.text()).toContain('Overview')
    expect(card.text()).toContain('Synthetic public demo')
    expect(card.text()).not.toContain('Vue DevTools')
    expect(card.text()).not.toMatch(/[-—–]/)
    expect(fetchSpy).not.toHaveBeenCalled()
    target.remove()
    fetchSpy.mockRestore()
    wrapper.unmount()
  })
})