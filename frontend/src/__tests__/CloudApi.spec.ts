import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BoundaryMeshApiError } from '@/api/errors'
import { readDelivery, submitSyntheticTransaction } from '@/api/cloud'
import { apiClient } from '@/api/client'

vi.mock('@/api/client', () => ({ apiClient: { request: vi.fn() } }))

const request = vi.mocked(apiClient.request)

describe('production cloud API client', () => {
  beforeEach(() => request.mockReset())

  it('submits the real production contract without a dev route or privileged token', async () => {
    const id = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee'
    request.mockResolvedValueOnce({ status: 202, requestId: 'one', data: {
      transaction_id: id, outcome: 'ALLOW', submission_status: 'ACCEPTED_FOR_ASYNCHRONOUS_DELIVERY',
      delivery_status: 'NOT_ATTEMPTED', instruction_id: 'dlv-ef133a64-fe3c-4cd3-afc6-6c4486f3dd00',
      attestation_id: '67e2f339-b122-4fc9-86f7-a7fb6f0b2cce', audit_chain_id: 'quayforge-orders', queued: true,
    } })
    const result = await submitSyntheticTransaction(id)
    expect(result.outcome).toBe('ALLOW')
    expect(request).toHaveBeenCalledWith('/api/v1/transactions', expect.objectContaining({
      method: 'POST',
      body: expect.objectContaining({
        transaction_id: id,
        integration_id: 'quayforge-orders',
        contract_version: 'v1',
        destination_system: 'RIVERTIDE',
      }),
    }))
  })

  it('distinguishes pending delivery from a truthful durable quarantine', async () => {
    const id = 'dlv-ef133a64-fe3c-4cd3-afc6-6c4486f3dd00'
    request.mockRejectedValueOnce(new BoundaryMeshApiError('server', 'Not found', { status: 404 }))
    expect(await readDelivery(id)).toBeNull()
    request.mockResolvedValueOnce({ status: 200, requestId: 'two', data: {
      instruction_id: id, state: 'QUARANTINED', provider_category: 'CREDENTIAL_OR_CONSENT',
      provider_called: false, attempt_count: 1, terminal: true,
    } })
    expect(await readDelivery(id)).toMatchObject({ state: 'QUARANTINED', provider_called: false })
  })
})
