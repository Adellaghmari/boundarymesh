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

export const transformedDecision: EvaluateResponse = {
  ...allowResponse,
  outcome: 'ALLOW_WITH_TRANSFORMATION',
  policy_version: 'boundary-enforcement.phase10.v1',
  source_payload: '{"email":"ada.lovelace@example.test","order_id":"ord-1"}',
  transformed_payload: '{"email":"[REDACTED]","order_id":"ord-1"}',
  reason_codes: [{ code: 'EGRESS_REDACTION_REQUIRED', description: 'Email redacted.' }],
}

export const deliverySuccess = {
  banner: 'SYNTHETIC DEMO - local delivery worker. Not Pub/Sub. Not Secret Manager.',
  stages: [
    'Boundary Decision',
    'Delivery Instruction',
    'Credential / Consent',
    'Camel Route',
    'Connector',
    'Resilience',
    'Delivery Result',
  ],
  decision: transformedDecision,
  instruction: {
    event_id: 'evt-delivery-1',
    transaction_id: transformedDecision.transaction_id,
    destination: 'LUMENWAY',
    outcome: 'ALLOW_WITH_TRANSFORMATION',
    payload_json: '{"email":"[REDACTED]","order_id":"ord-1"}',
    contract_reference: 'quayforge-orders:v1',
  },
  delivery: {
    kind: 'DELIVERED',
    attempt_count: 1,
    provider_category: 'SUCCESS',
    reason: null,
    explanation: 'Provider accepted the delivery.',
    connector_id: 'lumenway',
    route_id: 'sink.lumenway.fulfillment',
    circuit_state: 'CLOSED',
    http_status: 200,
    provider_called: true,
  },
  credential: {
    reference: 'lumenway-api:v1',
    status: 'PASS',
    explanation: 'Active credential version is resolvable.',
  },
  consent: {
    id: 'lumenway-fulfillment',
    status: 'GRANTED',
    permissions: ['fulfillment.create'],
    explanation: 'Connector operation is permitted.',
  },
  quarantine: null,
}

export const credentialCatalog = {
  banner: 'LOCAL SECURITY MODEL / SYNTHETIC DEMO. Google Secret Manager remains PLANNED.',
  credentials: [
    {
      id: 'covecrm-api',
      version: '1',
      status: 'RETIRED',
      destination: 'COVECRM',
      connector: 'covecrm',
      rotated_at: '2026-09-01T00:00:00Z',
    },
    {
      id: 'covecrm-api',
      version: '2',
      status: 'ACTIVE',
      destination: 'COVECRM',
      connector: 'covecrm',
      rotated_at: '2026-09-09T00:00:00Z',
    },
  ],
  consents: [
    {
      id: 'covecrm-activity',
      destination: 'COVECRM',
      operation: 'activity.write',
      status: 'GRANTED',
      kind: 'APPLICATION',
      permissions: ['activity.write'],
    },
  ],
}

export const quarantineItem = {
  id: 'q-100',
  transaction_id: 'txn-q-1',
  destination: 'COVECRM',
  reason: 'DELIVERY_RETRIES_EXHAUSTED',
  status: 'OPEN',
  attempts: 1,
  max_attempts: 3,
  eligible: true,
  credential_reference: 'covecrm-api:v2',
  connector_state: 'CLOSED',
  created_at: '2026-09-10T00:00:00Z',
}
