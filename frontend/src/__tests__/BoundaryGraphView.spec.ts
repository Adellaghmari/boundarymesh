import { http, HttpResponse } from 'msw'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import BoundaryGraphView from '@/views/BoundaryGraphView.vue'
import { routes } from '@/router'
import { boundaryGraph } from './fixtures'
import { server } from './mswServer'

async function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({ history: createMemoryHistory(), routes })
  await router.push('/boundary-graph')
  await router.isReady()
  const wrapper = mount(BoundaryGraphView, {
    global: { plugins: [pinia, router] },
  })
  await flushPromises()
  return wrapper
}

describe('BoundaryGraphView', () => {
  it('renders nodes, edges, and enforcement overlay from the API', async () => {
    const wrapper = await mountView()
    expect(wrapper.text()).toContain('QuayForge Orders')
    expect(wrapper.text()).toContain('CoveCRM')
    expect(wrapper.text()).toContain('BoundaryMesh')
    const firstEdge = wrapper.findAll('path.edge')[0]
    expect(firstEdge).toBeTruthy()
    await firstEdge!.trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="edge-detail"]').text()).toContain('JWT RS256')
    expect(wrapper.get('[data-testid="edge-status"]').text()).toContain('ACTIVE')
    expect(wrapper.get('[aria-label="Enforcement stages"]').text()).toContain('Identity')
    expect(wrapper.get('[aria-label="Enforcement stages"]').text()).toContain('PASS')
    expect(wrapper.text()).not.toContain('TEST-SECRET')
  })

  it('filters quarantined edges and reflects an observed update', async () => {
    const wrapper = await mountView()
    await wrapper.get('input[value="QUARANTINED"]').setValue(true)
    expect(wrapper.findAll('path.edge')).toHaveLength(0)

    server.use(
      http.get('/api/v1/boundary-graph', () =>
        HttpResponse.json({
          ...boundaryGraph,
          edges: boundaryGraph.edges.map((edge) =>
            edge.id === 'quayforge-orders'
              ? {
                  ...edge,
                  status: 'QUARANTINED',
                  recent_outcome: 'QUARANTINE',
                  recent_delivery: 'NOT_ATTEMPTED',
                  quarantine_count: 1,
                }
              : edge,
          ),
        }),
      ),
    )
    await wrapper.get('button.action').trigger('click')
    await flushPromises()
    await wrapper.get('input[value="ALL"]').setValue(true)
    const updated = wrapper.findAll('path.edge')[0]
    expect(updated).toBeTruthy()
    await updated!.trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="edge-status"]').text()).toContain('QUARANTINED')
  })
})
