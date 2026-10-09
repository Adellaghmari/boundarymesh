import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import App from '@/App.vue'
import { routes } from '@/router'
import { useEventsStore } from '@/stores/events'
import { sessionEvent } from './fixtures'

function counts(text: string) {
  return {
    hyphen: (text.match(/-/g) ?? []).length,
    en: (text.match(/\u2013/g) ?? []).length,
    em: (text.match(/\u2014/g) ?? []).length,
  }
}

function expectClean(surface: string, text: string) {
  const result = counts(text)
  const at = text.search(/[-—–]/)
  const sample = at < 0 ? '' : text.slice(Math.max(0, at - 48), at + 64).replace(/\s+/g, ' ')
  expect({ surface, ...result, sample }).toEqual({
    surface,
    hyphen: 0,
    en: 0,
    em: 0,
    sample: '',
  })
}

async function openSurface(path: string, seedEvent = false) {
  const pinia = createPinia()
  setActivePinia(pinia)
  if (seedEvent) {
    useEventsStore().events.push(sessionEvent)
  }
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push(path)
  await router.isReady()
  const wrapper = mount(App, {
    global: { plugins: [pinia, router] },
  })
  await flushPromises()
  return wrapper
}

async function clickNamed(wrapper: VueWrapper, name: string) {
  const button = wrapper.findAll('button').find((item) => item.text().includes(name))
  if (button) {
    await button.trigger('click')
    await flushPromises()
  }
}

describe('rendered website copy has no visible dashes', () => {
  it('overview, navigation, and an evaluation', async () => {
    const wrapper = await openSurface('/')
    for (const name of ['Labs', 'Operations', 'Evidence']) {
      await clickNamed(wrapper, name)
    }
    await wrapper.get('[aria-label="Open navigation"]').trigger('click')
    await clickNamed(wrapper, 'Evaluate crossing')
    const overviewText = `${document.title}\n${wrapper.text()}`
    expect(overviewText).not.toMatch(/[-—–]/)
    expectClean('Overview', overviewText)
    wrapper.unmount()
  })

  it('session events and a recorded crossing', async () => {
    const list = await openSurface('/boundary-events', true)
    expect(list.text()).not.toMatch(/[-—–]/)
    expectClean('Boundary Events', list.text())
    list.unmount()

    const detail = await openSurface(`/boundary-events/${sessionEvent.transactionId}`, true)
    expect(detail.text()).not.toMatch(/[-—–]/)
    expectClean('Boundary event', detail.text())
    detail.unmount()

    const missing = await openSurface('/boundary-events/00000000-0000-0000-0000-000000000000')
    expect(missing.text()).not.toMatch(/[-—–]/)
    expectClean('Missing boundary event', missing.text())
    missing.unmount()
  })

  it('connections, architecture, status, and unknown route', async () => {
    for (const [surface, path] of [
      ['Connections', '/connections'],
      ['Architecture', '/architecture'],
      ['Status', '/status'],
      ['Not found', '/missing-page'],
    ] as const) {
      const wrapper = await openSurface(path)
      expect(wrapper.text()).not.toMatch(/[-—–]/)
      expectClean(surface, wrapper.text())
      wrapper.unmount()
    }
  })

  it('lab pages after their primary action', async () => {
    const pages: Array<[string, string, string]> = [
      ['Identity lab', '/identity', 'Evaluate identity'],
      ['Webhook lab', '/webhooks', 'Evaluate webhook'],
      ['Contract lab', '/contracts', 'Evaluate contract'],
      ['Data boundary lab', '/data-boundary', 'Evaluate data boundary'],
      ['Crossing lab', '/crossing', 'Inspect crossing'],
      ['Delivery lab', '/delivery', 'Run delivery'],
      ['Credential lab', '/credentials', 'Evaluate credential boundary'],
    ]
    for (const [surface, path, action] of pages) {
      const wrapper = await openSurface(path)
      await clickNamed(wrapper, action)
      expect(wrapper.text()).not.toMatch(/[-—–]/)
      expectClean(surface, wrapper.text())
      wrapper.unmount()
    }
  })

  it('quarantine, audit, attestations, and the boundary graph', async () => {
    const pages: Array<[string, string]> = [
      ['Quarantine', '/quarantine'],
      ['Quarantine item', '/quarantine/q-100'],
      ['Audit', '/audit'],
      ['Audit chain', '/audit/quayforge-orders'],
      ['Attestations', '/attestations'],
      ['Attestation', '/attestations/att-test-1'],
      ['Boundary graph', '/boundary-graph'],
      ['About the project', '/about'],
    ]
    for (const [surface, path] of pages) {
      const wrapper = await openSurface(path)
      if (surface === 'Boundary graph') {
        const edge = wrapper.find('path.edge')
        if (edge.exists()) {
          await edge.trigger('click')
          await flushPromises()
        }
      }
      expect(wrapper.text()).not.toMatch(/[-—–]/)
      expectClean(surface, wrapper.text())
      wrapper.unmount()
    }
  })
})
