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
    expect(model.state.map((fact) => fact.value).join(' ')).not.toMatch(/[-—–]/)
    expect(model.log.map((line) => `${line.clock} ${line.message}`).join(' ')).not.toMatch(/[-—–]/)
  })

  it('opens the drawer and follows a new synthetic decision', async () => {
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
    await wrapper.get('#system-console-tab-events').trigger('click')
    expect(wrapper.text()).toContain('Identity rejected')
    await wrapper.get('#system-console-tab-system').trigger('click')
    expect(wrapper.text()).toContain('Private API unavailable')
    expect(wrapper.text()).toContain('Operator authority unavailable')
    expect(wrapper.text()).toContain('Read only')
    expect(fetchSpy).not.toHaveBeenCalled()
    fetchSpy.mockRestore()
    wrapper.unmount()
  })
})