import type { BoundaryEventRecord, EvaluateResponse } from '@/domain/types'

export const allowResponse: EvaluateResponse = {
  banner: 'SYNTHETIC DEMO - local Boundary Enforcement Engine only. Not production IAM.',
  transaction_id: '11111111-2222-3333-4444-555555555555',
  outcome: 'ALLOW',
  reason_codes: [],
  applied_controls: {
    IDENTITY: 'PASS',
    AUTHORIZATION: 'PASS',
    AUTHENTICITY: 'PASS',
    FRESHNESS: 'PASS',
    REPLAY: 'PASS',
    CONTRACT: 'PASS',
    EGRESS: 'PASS',
    CREDENTIAL_RESOLVABILITY: 'PASS',
    CONNECTOR_SAFETY: 'PASS',
  },
  decided_at: '2026-09-09T21:00:00Z',
  policy_version: 'boundary-enforcement.phase1.v1',
}

export const sessionEvent: BoundaryEventRecord = {
  transactionId: allowResponse.transaction_id,
  scenarioId: 'valid-crossing',
  scenarioLabel: 'Valid crossing',
  sourceSystem: 'HELIX_365',
  destinationSystem: 'COVECRM',
  integrationId: 'helix-calendar-crm',
  outcome: 'ALLOW',
  decidedAt: allowResponse.decided_at,
  policyVersion: allowResponse.policy_version,
  reasonCodes: [],
  appliedControls: allowResponse.applied_controls,
  controlResults: [
    {
      kind: 'IDENTITY',
      status: 'PASS',
      reasonCodes: [],
      safeExplanation: 'Synthetic identity accepted for this Velora crossing.',
    },
  ],
  banner: allowResponse.banner,
  requestId: 'bm-test',
}
