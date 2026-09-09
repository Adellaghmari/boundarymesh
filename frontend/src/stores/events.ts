import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { evaluateBoundary } from '@/api/boundarymesh'
import { isRetryable, userMessageFor } from '@/api/errors'
import { toBoundaryEvent } from '@/domain/mappers'
import { buildEvaluateRequest, scenarioById, type ScenarioId } from '@/domain/scenarios'
import type { BoundaryEventRecord } from '@/domain/types'

function newTransactionId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return '00000000-0000-4000-8000-000000000000'
}

export const useEventsStore = defineStore('events', () => {
  const events = ref<BoundaryEventRecord[]>([])
  const lastEvent = ref<BoundaryEventRecord | null>(null)
  const evaluating = ref(false)
  const errorMessage = ref<string | null>(null)
  const retryable = ref(false)
  const selectedScenarioId = ref<ScenarioId>('valid-crossing')

  const newestFirst = computed(() =>
    [...events.value].sort((a, b) => b.decidedAt.localeCompare(a.decidedAt)),
  )

  function eventById(transactionId: string): BoundaryEventRecord | undefined {
    return events.value.find((event) => event.transactionId === transactionId)
  }

  async function evaluateSelected(): Promise<BoundaryEventRecord | null> {
    return evaluateScenario(selectedScenarioId.value)
  }

  async function evaluateScenario(scenarioId: ScenarioId): Promise<BoundaryEventRecord | null> {
    const scenario = scenarioById(scenarioId)
    evaluating.value = true
    errorMessage.value = null
    retryable.value = false
    try {
      const request = buildEvaluateRequest(scenario, newTransactionId())
      const response = await evaluateBoundary(request)
      const record = toBoundaryEvent({
        scenarioId: scenario.id,
        scenarioLabel: scenario.label,
        request,
        response: response.data,
        requestId: response.requestId,
      })
      events.value = [
        record,
        ...events.value.filter((item) => item.transactionId !== record.transactionId),
      ]
      lastEvent.value = record
      return record
    } catch (error) {
      errorMessage.value = userMessageFor(error)
      retryable.value = isRetryable(error)
      return null
    } finally {
      evaluating.value = false
    }
  }

  function clearSession(): void {
    events.value = []
    lastEvent.value = null
    errorMessage.value = null
  }

  return {
    events,
    lastEvent,
    evaluating,
    errorMessage,
    retryable,
    selectedScenarioId,
    newestFirst,
    eventById,
    evaluateSelected,
    evaluateScenario,
    clearSession,
  }
})
