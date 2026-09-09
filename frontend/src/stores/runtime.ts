import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { fetchHealth, fetchReady } from '@/api/boundarymesh'
import { isRetryable, userMessageFor } from '@/api/errors'
import type { HealthResponse, ReadyResponse } from '@/domain/types'

export const useRuntimeStore = defineStore('runtime', () => {
  const health = ref<HealthResponse | null>(null)
  const ready = ref<ReadyResponse | null>(null)
  const loading = ref(false)
  const errorMessage = ref<string | null>(null)
  const retryable = ref(false)
  const lastCheckedAt = ref<string | null>(null)

  const backendReachable = computed(() => health.value?.status === 'UP')
  const backendReady = computed(() => ready.value?.status === 'READY')

  async function refreshStatus(): Promise<void> {
    loading.value = true
    errorMessage.value = null
    retryable.value = false
    try {
      const [healthResult, readyResult] = await Promise.all([fetchHealth(), fetchReady()])
      health.value = healthResult.data
      ready.value = readyResult.data
      lastCheckedAt.value = new Date().toISOString()
    } catch (error) {
      health.value = null
      ready.value = null
      errorMessage.value = userMessageFor(error)
      retryable.value = isRetryable(error)
    } finally {
      loading.value = false
    }
  }

  return {
    health,
    ready,
    loading,
    errorMessage,
    retryable,
    lastCheckedAt,
    backendReachable,
    backendReady,
    refreshStatus,
  }
})
