import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  fetchCredentialCatalog,
  fetchQuarantineItem,
  fetchQuarantineList,
  reprocessQuarantine,
  rotateCoveCredential,
  runCredentialLab,
  runDeliveryLab,
} from '@/api/boundarymesh'
import { isRetryable, userMessageFor } from '@/api/errors'
import { toDecisionEvent } from '@/domain/mappers'
import type {
  BoundaryEventRecord,
  CredentialCatalog,
  DeliveryLabRecord,
  QuarantineItemRecord,
  QuarantineListRecord,
} from '@/domain/types'

export const useDeliveryStore = defineStore('delivery', () => {
  const evaluating = ref(false)
  const errorMessage = ref<string | null>(null)
  const retryable = ref(false)
  const lastDelivery = ref<DeliveryLabRecord | null>(null)
  const lastCredentialEvent = ref<BoundaryEventRecord | null>(null)
  const catalog = ref<CredentialCatalog | null>(null)
  const quarantineList = ref<QuarantineListRecord | null>(null)
  const selectedQuarantine = ref<QuarantineItemRecord | null>(null)
  const lastReprocess = ref<DeliveryLabRecord | null>(null)

  async function run<T>(work: () => Promise<T>): Promise<T | null> {
    evaluating.value = true
    errorMessage.value = null
    retryable.value = false
    try {
      return await work()
    } catch (error) {
      errorMessage.value = userMessageFor(error)
      retryable.value = isRetryable(error)
      return null
    } finally {
      evaluating.value = false
    }
  }

  async function evaluateDelivery(scenario: string): Promise<DeliveryLabRecord | null> {
    return run(async () => {
      const record = await runDeliveryLab(scenario)
      lastDelivery.value = record
      return record
    })
  }

  async function loadCatalog(): Promise<CredentialCatalog | null> {
    return run(async () => {
      const next = await fetchCredentialCatalog()
      catalog.value = next
      return next
    })
  }

  async function evaluateCredential(scenario: string): Promise<BoundaryEventRecord | null> {
    return run(async () => {
      const response = await runCredentialLab(scenario)
      const record = toDecisionEvent({
        scenarioId: scenario,
        scenarioLabel: scenario,
        sourceSystem: 'QUAYFORGE',
        destinationSystem: 'COVECRM',
        integrationId: 'credential-lab',
        response: response.data,
        requestId: response.requestId,
      })
      lastCredentialEvent.value = record
      return record
    })
  }

  async function rotate(): Promise<CredentialCatalog | null> {
    return run(async () => {
      const next = await rotateCoveCredential()
      catalog.value = next
      return next
    })
  }

  async function loadQuarantine(): Promise<QuarantineListRecord | null> {
    return run(async () => {
      const next = await fetchQuarantineList()
      quarantineList.value = next
      return next
    })
  }

  async function loadQuarantineItem(id: string): Promise<QuarantineItemRecord | null> {
    return run(async () => {
      const next = await fetchQuarantineItem(id)
      selectedQuarantine.value = next
      return next
    })
  }

  async function reprocess(id: string): Promise<DeliveryLabRecord | null> {
    return run(async () => {
      const record = await reprocessQuarantine(id)
      lastReprocess.value = record
      lastDelivery.value = record
      if (record.quarantine) {
        selectedQuarantine.value = {
          id: record.quarantine.id,
          transactionId: record.decision.transaction_id,
          destination: record.quarantine.destination,
          reason: record.quarantine.reason,
          status: record.quarantine.status,
          attempts: record.quarantine.attempts,
          maxAttempts: 3,
          eligible: record.quarantine.eligible,
          credentialReference: record.credential.reference,
          connectorState: record.delivery?.circuitState ?? null,
          createdAt: record.decision.decided_at,
        }
      }
      return record
    })
  }

  return {
    evaluating,
    errorMessage,
    retryable,
    lastDelivery,
    lastCredentialEvent,
    catalog,
    quarantineList,
    selectedQuarantine,
    lastReprocess,
    evaluateDelivery,
    loadCatalog,
    evaluateCredential,
    rotate,
    loadQuarantine,
    loadQuarantineItem,
    reprocess,
  }
})
