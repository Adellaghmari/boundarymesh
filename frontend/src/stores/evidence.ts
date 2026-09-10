import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  fetchAttestation,
  fetchAttestationTamperDemo,
  fetchAttestations,
  fetchAuditChain,
  fetchAuditChains,
  fetchAuditTamperDemo,
  fetchBoundaryGraph,
  runDeliveryLab,
  verifyAttestation,
  verifyAuditChain,
} from '@/api/boundarymesh'
import { isRetryable, userMessageFor } from '@/api/errors'
import type {
  AttestationListRecord,
  AttestationRecord,
  AttestationVerifyRecord,
  AuditChainListRecord,
  AuditChainRecord,
  AuditVerifyRecord,
  BoundaryGraphRecord,
  DeliveryLabRecord,
} from '@/domain/types'

export const useEvidenceStore = defineStore('evidence', () => {
  const loading = ref(false)
  const errorMessage = ref<string | null>(null)
  const retryable = ref(false)
  const chainList = ref<AuditChainListRecord | null>(null)
  const chain = ref<AuditChainRecord | null>(null)
  const chainVerify = ref<AuditVerifyRecord | null>(null)
  const attestations = ref<AttestationListRecord | null>(null)
  const attestation = ref<AttestationRecord | null>(null)
  const attestationVerify = ref<AttestationVerifyRecord | null>(null)
  const graph = ref<BoundaryGraphRecord | null>(null)
  const lastDelivery = ref<DeliveryLabRecord | null>(null)
  const auditTamper = ref<{ banner: string; original: AuditVerifyRecord; tampered: AuditVerifyRecord } | null>(null)
  const attestationTamper = ref<{
    banner: string
    original: AttestationVerifyRecord | null
    tampered: AttestationVerifyRecord | null
  } | null>(null)

  async function run<T>(work: () => Promise<T>): Promise<T | null> {
    loading.value = true
    errorMessage.value = null
    retryable.value = false
    try {
      return await work()
    } catch (error) {
      errorMessage.value = userMessageFor(error)
      retryable.value = isRetryable(error)
      return null
    } finally {
      loading.value = false
    }
  }

  async function loadChains(): Promise<void> {
    await run(async () => {
      chainList.value = await fetchAuditChains()
    })
  }

  async function loadChain(id: string): Promise<void> {
    await run(async () => {
      chain.value = await fetchAuditChain(id)
    })
  }

  async function verifyChain(id: string): Promise<void> {
    await run(async () => {
      chainVerify.value = await verifyAuditChain(id)
      chain.value = await fetchAuditChain(id)
    })
  }

  async function loadAttestations(): Promise<void> {
    await run(async () => {
      attestations.value = await fetchAttestations()
    })
  }

  async function loadAttestation(id: string): Promise<void> {
    await run(async () => {
      attestation.value = await fetchAttestation(id)
    })
  }

  async function verifySignature(id: string): Promise<void> {
    await run(async () => {
      attestationVerify.value = await verifyAttestation(id)
    })
  }

  async function loadGraph(): Promise<void> {
    await run(async () => {
      graph.value = await fetchBoundaryGraph()
    })
  }

  async function runCrossing(scenario: string): Promise<void> {
    await run(async () => {
      lastDelivery.value = await runDeliveryLab(scenario)
      graph.value = await fetchBoundaryGraph()
    })
  }

  async function runAuditTamper(): Promise<void> {
    await run(async () => {
      auditTamper.value = await fetchAuditTamperDemo()
    })
  }

  async function runAttestationTamper(): Promise<void> {
    await run(async () => {
      attestationTamper.value = await fetchAttestationTamperDemo()
    })
  }

  return {
    loading,
    errorMessage,
    retryable,
    chainList,
    chain,
    chainVerify,
    attestations,
    attestation,
    attestationVerify,
    graph,
    lastDelivery,
    auditTamper,
    attestationTamper,
    loadChains,
    loadChain,
    verifyChain,
    loadAttestations,
    loadAttestation,
    verifySignature,
    loadGraph,
    runCrossing,
    runAuditTamper,
    runAttestationTamper,
  }
})
