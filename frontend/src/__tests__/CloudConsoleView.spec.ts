import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BoundaryMeshApiError } from '@/api/errors'
import CloudConsoleView from '@/views/CloudConsoleView.vue'
import {
  readAttestation, readDelivery, submitSyntheticTransaction, verifyAttestation, verifyAuditChain,
} from '@/api/cloud'
import { initializeCloudSession, signInOperator } from '@/api/cloudSession'

vi.mock('@/api/cloudSession', () => ({
  firebaseConfigured: () => true,
  initializeCloudSession: vi.fn(),
  signInOperator: vi.fn(),
  signOutOperator: vi.fn(),
}))
vi.mock('@/api/cloud', () => ({
  readAttestation: vi.fn(), readDelivery: vi.fn(), submitSyntheticTransaction: vi.fn(),
  verifyAttestation: vi.fn(), verifyAuditChain: vi.fn(),
}))

describe('hosted production console', () => {
  beforeEach(() => {
    vi.mocked(initializeCloudSession).mockReset()
    vi.mocked(signInOperator).mockReset()
    vi.mocked(submitSyntheticTransaction).mockReset()
    vi.mocked(readAttestation).mockReset()
    vi.mocked(readDelivery).mockReset()
    vi.mocked(verifyAttestation).mockReset()
    vi.mocked(verifyAuditChain).mockReset()
  })

  it('shows signed-out and unauthorized states without executing a lab call', async () => {
    vi.mocked(initializeCloudSession).mockResolvedValueOnce(null)
    vi.mocked(signInOperator).mockRejectedValueOnce(new BoundaryMeshApiError('forbidden', 'Denied', { status: 403 }))
    const wrapper = mount(CloudConsoleView)
    await flushPromises()
    expect(wrapper.text()).toContain('Sign in with your approved Google account')
    await wrapper.get('button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('not on the operator allowlist')
    expect(submitSyntheticTransaction).not.toHaveBeenCalled()
  })

  it('shows 202 acceptance, valid evidence, and terminal quarantine without provider success', async () => {
    vi.mocked(initializeCloudSession).mockResolvedValueOnce('operator@example.invalid')
    vi.mocked(submitSyntheticTransaction).mockResolvedValueOnce({
      transaction_id: 'tx', outcome: 'ALLOW', submission_status: 'ACCEPTED_FOR_ASYNCHRONOUS_DELIVERY',
      delivery_status: 'NOT_ATTEMPTED', instruction_id: 'dlv-ef133a64-fe3c-4cd3-afc6-6c4486f3dd00',
      attestation_id: '67e2f339-b122-4fc9-86f7-a7fb6f0b2cce', attestation_status: 'SIGNED',
      audit_chain_id: 'quayforge-orders', queued: true,
    })
    vi.mocked(readAttestation).mockResolvedValueOnce({
      id: '67e2f339-b122-4fc9-86f7-a7fb6f0b2cce', transaction_id: 'tx', boundary_outcome: 'ALLOW',
      status: 'SIGNED', algorithm: 'ES256', key_version: '1', audit_chain_id: 'quayforge-orders', signature_present: true,
    })
    vi.mocked(verifyAttestation).mockResolvedValueOnce({ status: 'VALID' })
    vi.mocked(verifyAuditChain).mockResolvedValueOnce({ status: 'VALID', entries_checked: 4 })
    vi.mocked(readDelivery).mockResolvedValueOnce({
      instruction_id: 'dlv-ef133a64-fe3c-4cd3-afc6-6c4486f3dd00', state: 'QUARANTINED',
      provider_category: 'CREDENTIAL_OR_CONSENT', provider_called: false, attempt_count: 1, terminal: true,
    })
    const wrapper = mount(CloudConsoleView)
    await flushPromises()
    const submit = wrapper.findAll('button').find((button) => button.text().includes('Submit synthetic transaction'))
    expect(submit).toBeDefined()
    await submit!.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('ACCEPTED_FOR_ASYNCHRONOUS_DELIVERY')
    expect(wrapper.text()).toContain('Signature verification: VALID')
    expect(wrapper.text()).toContain('Audit-chain verification: VALID')
    expect(wrapper.text()).toContain('QUARANTINED')
    expect(wrapper.text()).toContain('Provider called: NO')
    expect(wrapper.text()).toContain('External delivery did not succeed')
  })

  it('marks an expired session instead of reporting a generic network failure', async () => {
    vi.mocked(initializeCloudSession).mockRejectedValueOnce(new BoundaryMeshApiError('unauthorized', 'Expired', { status: 401 }))
    const wrapper = mount(CloudConsoleView)
    await flushPromises()
    expect(wrapper.text()).toContain('session has expired')
  })

  it('leaves a failed session bootstrap unavailable rather than indefinitely loading', async () => {
    vi.mocked(initializeCloudSession).mockRejectedValueOnce(
      new BoundaryMeshApiError('malformed', 'The BoundaryMesh service returned an unexpected response.'),
    )
    const wrapper = mount(CloudConsoleView)
    await flushPromises()
    expect(wrapper.text()).toContain('secure web edge is unavailable')
    expect(wrapper.get('button').text()).toBe('Sign in')
    expect(submitSyntheticTransaction).not.toHaveBeenCalled()
  })
})
