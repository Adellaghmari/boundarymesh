import { apiClient } from '@/api/client'
import { BoundaryMeshApiError } from '@/api/errors'

export interface TransactionSubmission {
  transaction_id: string
  outcome: string
  submission_status: string
  delivery_status: string
  instruction_id: string | null
  attestation_id: string | null
  attestation_status: string | null
  audit_chain_id: string | null
  queued: boolean
}

export interface DeliveryEvidence {
  instruction_id: string
  state: string
  provider_category: string
  provider_called: boolean
  attempt_count: number
  terminal: boolean
}

export interface AttestationSummary {
  id: string
  transaction_id: string
  boundary_outcome: string
  status: string
  algorithm: string
  key_version: string
  audit_chain_id: string
  signature_present: boolean
}

export interface Verification {
  status: string
  entries_checked?: number
}

function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== 'object') {
    throw new BoundaryMeshApiError('malformed', 'Cloud evidence was not valid JSON.')
  }
  return value as Record<string, unknown>
}

export async function submitSyntheticTransaction(transactionId: string): Promise<TransactionSubmission> {
  const response = await apiClient.request<unknown>('/api/v1/transactions', {
    method: 'POST',
    body: {
      transaction_id: transactionId,
      integration_id: 'quayforge-orders',
      contract_version: 'v1',
      destination_system: 'RIVERTIDE',
      payload: {
        event_id: `evt-${transactionId}`,
        event_type: 'order.updated',
        order_id: `ord-${transactionId.slice(0, 8)}`,
        customer_id: 'cust-synthetic-15j',
      },
    },
    timeoutMs: 30000,
  })
  const item = record(response.data)
  if (response.status !== 202 || item.transaction_id !== transactionId || typeof item.outcome !== 'string') {
    throw new BoundaryMeshApiError('malformed', 'Transaction acceptance did not match the production contract.')
  }
  return item as unknown as TransactionSubmission
}

export async function readDelivery(instructionId: string): Promise<DeliveryEvidence | null> {
  try {
    const item = record((await apiClient.request<unknown>(`/api/v1/deliveries/${encodeURIComponent(instructionId)}`)).data)
    if (item.instruction_id !== instructionId || typeof item.state !== 'string' || typeof item.provider_called !== 'boolean') {
      throw new BoundaryMeshApiError('malformed', 'Delivery evidence did not match the expected contract.')
    }
    return item as unknown as DeliveryEvidence
  } catch (error) {
    if (error instanceof BoundaryMeshApiError && error.status === 404) return null
    throw error
  }
}

export async function readAttestation(id: string): Promise<AttestationSummary> {
  const item = record((await apiClient.request<unknown>(`/api/v1/attestations/${encodeURIComponent(id)}/summary`)).data)
  if (item.id !== id || typeof item.status !== 'string' || typeof item.algorithm !== 'string') {
    throw new BoundaryMeshApiError('malformed', 'Attestation evidence did not match the expected contract.')
  }
  return item as unknown as AttestationSummary
}

export async function verifyAttestation(id: string): Promise<Verification> {
  const item = record((await apiClient.request<unknown>(`/api/v1/attestations/${encodeURIComponent(id)}/verify`)).data)
  if (typeof item.status !== 'string') throw new BoundaryMeshApiError('malformed', 'Attestation verification was invalid.')
  return item as unknown as Verification
}

export async function verifyAuditChain(id: string): Promise<Verification> {
  const item = record((await apiClient.request<unknown>(`/api/v1/audit/chains/${encodeURIComponent(id)}/verify`)).data)
  if (typeof item.status !== 'string') throw new BoundaryMeshApiError('malformed', 'Audit verification was invalid.')
  return item as unknown as Verification
}
