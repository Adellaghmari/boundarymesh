import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { evaluateBoundary, evaluateIdentity, runWebhookLab } from '@/api/boundarymesh'
import { BoundaryMeshApiError, isRetryable, userMessageFor } from '@/api/errors'
import { toBoundaryEvent, toDecisionEvent } from '@/domain/mappers'
import { buildEvaluateRequest, scenarioById, type ScenarioId } from '@/domain/scenarios'
import type { BoundaryEventRecord } from '@/domain/types'

export interface SecurityHttpFailure {
  status?: number
  kind: 'unauthorized' | 'forbidden' | 'other'
  message: string
}

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
  const identityFailure = ref<SecurityHttpFailure | null>(null)
  const webhookFailure = ref<SecurityHttpFailure | null>(null)
  const lastIdentityEvent = ref<BoundaryEventRecord | null>(null)
  const lastWebhookEvent = ref<BoundaryEventRecord | null>(null)

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
      remember(record)
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

  function remember(record: BoundaryEventRecord): void {
    events.value = [
      record,
      ...events.value.filter((item) => item.transactionId !== record.transactionId),
    ]
  }

  function asSecurityFailure(error: unknown): SecurityHttpFailure {
    if (error instanceof BoundaryMeshApiError) {
      if (error.kind === 'unauthorized' || error.kind === 'forbidden') {
        return { status: error.status, kind: error.kind, message: error.message }
      }
      return { status: error.status, kind: 'other', message: error.message }
    }
    return { kind: 'other', message: userMessageFor(error) }
  }

  async function evaluateIdentityScenario(scenarioId: string): Promise<BoundaryEventRecord | null> {
    evaluating.value = true
    errorMessage.value = null
    retryable.value = false
    identityFailure.value = null
    try {
      const response = await evaluateIdentity(scenarioId)
      const record = toDecisionEvent({
        scenarioId,
        scenarioLabel: scenarioId,
        sourceSystem: 'SYNTHETIC',
        destinationSystem: 'NONE',
        integrationId: 'identity-lab',
        response: response.data,
        requestId: response.requestId,
      })
      remember(record)
      lastEvent.value = record
      lastIdentityEvent.value = record
      return record
    } catch (error) {
      identityFailure.value = asSecurityFailure(error)
      errorMessage.value = userMessageFor(error)
      retryable.value = isRetryable(error)
      return null
    } finally {
      evaluating.value = false
    }
  }

  async function evaluateWebhookScenario(scenarioId: string): Promise<BoundaryEventRecord | null> {
    evaluating.value = true
    errorMessage.value = null
    retryable.value = false
    webhookFailure.value = null
    try {
      const response = await runWebhookLab(scenarioId)
      const record = toDecisionEvent({
        scenarioId,
        scenarioLabel: scenarioId,
        sourceSystem: 'QUAYFORGE',
        destinationSystem: 'RIVERTIDE',
        integrationId: 'quayforge-orders',
        response: response.data,
        requestId: response.requestId,
      })
      remember(record)
      lastEvent.value = record
      lastWebhookEvent.value = record
      return record
    } catch (error) {
      webhookFailure.value = asSecurityFailure(error)
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
    lastIdentityEvent.value = null
    lastWebhookEvent.value = null
    errorMessage.value = null
    identityFailure.value = null
    webhookFailure.value = null
  }

  return {
    events,
    lastEvent,
    lastIdentityEvent,
    lastWebhookEvent,
    evaluating,
    errorMessage,
    retryable,
    selectedScenarioId,
    identityFailure,
    webhookFailure,
    newestFirst,
    eventById,
    evaluateSelected,
    evaluateScenario,
    evaluateIdentityScenario,
    evaluateWebhookScenario,
    clearSession,
  }
})
