import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { useRuntimeStore } from '@/stores/runtime'
import { server } from './mswServer'

describe('runtime store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('loads local health and readiness', async () => {
    const store = useRuntimeStore()
    await store.refreshStatus()
    expect(store.backendReachable).toBe(true)
    expect(store.backendReady).toBe(true)
    expect(store.health?.truth).toContain('LOCAL')
  })

  it('records an unavailable backend without inventing UP', async () => {
    server.use(http.get('/api/v1/health', () => HttpResponse.error()))
    const store = useRuntimeStore()
    await store.refreshStatus()
    expect(store.backendReachable).toBe(false)
    expect(store.health).toBeNull()
    expect(store.errorMessage).toBeTruthy()
  })
})
