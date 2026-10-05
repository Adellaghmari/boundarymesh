import { BoundaryMeshApiError } from '@/api/errors'
import { PORTFOLIO_EXPLORER_CROSSINGS } from '@/domain/portfolioExplorer'
import type {
  AttestationListRecord,
  AttestationRecord,
  AttestationVerifyRecord,
  AuditChainListRecord,
  AuditChainRecord,
  AuditEntryRecord,
  AuditVerifyRecord,
  BoundaryGraphEdge,
  BoundaryGraphRecord,
  BoundaryOutcome,
  CredentialCatalog,
  DeliveryLabRecord,
  DeliveryResultKind,
  EvaluateRequest,
  EvaluateResponse,
  HealthResponse,
  QuarantineItemRecord,
  QuarantineListRecord,
  ReadyResponse,
} from '@/domain/types'

const REQUEST = 'bm-public-demo'
export const SYNTHETIC_DECISION_INSTANT = '2026-04-04T12:00:00Z'
const WHEN = SYNTHETIC_DECISION_INSTANT
const BANNER = 'SYNTHETIC DEMO'

interface ApiResponse<T> {
  data: T
  requestId: string
  status: number
}

const PASSING = {
  IDENTITY: 'PASS',
  AUTHORIZATION: 'PASS',
  AUTHENTICITY: 'PASS',
  FRESHNESS: 'PASS',
  REPLAY: 'PASS',
  CONTRACT: 'PASS',
  EGRESS: 'PASS',
  CREDENTIAL_RESOLVABILITY: 'PASS',
  CONNECTOR_SAFETY: 'PASS',
} as const

function envelope<T>(data: T, status = 200): ApiResponse<T> {
  return { data, requestId: REQUEST, status }
}

function outcomeFor(scenario: string): BoundaryOutcome {
  const id = scenario.toLowerCase()
  if (id === 'implicit-deny') return 'ALLOW_WITH_TRANSFORMATION'
  if (
    id.includes('missing-permission') ||
    id.includes('replay') ||
    id.includes('reject') ||
    id.includes('invalid') ||
    id.includes('deny') ||
    id.includes('expired') ||
    id.includes('missing-signature') ||
    id.includes('missing-required') ||
    id.includes('wrong-type')
  ) {
    return 'REJECT'
  }
  if (
    id.includes('quarantine') ||
    id.includes('drift') ||
    id.includes('unsupported') ||
    id.includes('unavailable') ||
    id.includes('circuit') ||
    id.includes('server-error') ||
    id.includes('missing') ||
    id.includes('revoked') ||
    id.includes('too-many')
  ) {
    return 'QUARANTINE'
  }
  if (id.includes('transform') || id.includes('redact')) return 'ALLOW_WITH_TRANSFORMATION'
  return 'ALLOW'
}

function outcomeFromRequest(request: EvaluateRequest): BoundaryOutcome {
  const controls = Object.values(request.controls)
  if (controls.some((control) => control.status === 'FAIL')) return 'REJECT'
  if (controls.some((control) => control.status === 'UNAVAILABLE')) return 'QUARANTINE'
  if (controls.some((control) => control.reason_codes.includes('EGRESS_TRANSFORMATION_REQUIRED'))) {
    return 'ALLOW_WITH_TRANSFORMATION'
  }
  return 'ALLOW'
}

function decide(
  outcome: BoundaryOutcome,
  transactionId: string,
  duplicate = false,
  controls?: EvaluateRequest['controls'],
): EvaluateResponse {
  const held = outcome === 'QUARANTINE'
  const transformed = outcome === 'ALLOW_WITH_TRANSFORMATION'
  const controlStatus = (kind: string, fallback: 'PASS' | 'FAIL' | 'UNAVAILABLE'): 'PASS' | 'FAIL' | 'UNAVAILABLE' => {
    const input = controls?.[kind]
    if (input?.status === 'FAIL' || input?.status === 'UNAVAILABLE' || input?.status === 'PASS') {
      return input.status
    }
    return fallback
  }
  const replay = controlStatus('replay', outcome === 'REJECT' && !controls ? 'FAIL' : 'PASS')
  const contract = controlStatus('contract', held ? 'FAIL' : 'PASS')
  const connector = controlStatus('connector_safety', held ? 'UNAVAILABLE' : 'PASS')
  const identity = controlStatus('identity', 'PASS')
  return {
    banner: BANNER,
    transaction_id: transactionId,
    outcome,
    ingress_kind: 'SYNTHETIC_EVALUATE',
    reason_codes: identity === 'FAIL'
      ? [{ code: 'IDENTITY_INVALID', description: 'Synthetic identity is invalid for this crossing.' }]
      : replay === 'FAIL'
        ? [{ code: 'REPLAY_DETECTED', description: 'This request identifier was already accepted.' }]
        : held
          ? [{ code: 'CONTRACT_DRIFT', description: 'The payload is outside the accepted contract.' }]
          : transformed
            ? [{ code: 'EGRESS_TRANSFORMATION_REQUIRED', description: 'A field must be redacted before it can leave.' }]
            : [],
    applied_controls: {
      ...PASSING,
      IDENTITY: identity,
      REPLAY: replay,
      CONTRACT: contract,
      CONNECTOR_SAFETY: connector,
    },
    controls: [
      { kind: 'IDENTITY', status: identity, reasonCodes: identity === 'FAIL' ? ['IDENTITY_INVALID'] : [], safeExplanation: identity === 'FAIL' ? 'Synthetic identity is invalid for this crossing.' : 'Synthetic identity accepted for this crossing.' },
      { kind: 'AUTHORIZATION', status: 'PASS', reasonCodes: [], safeExplanation: 'Synthetic authorization permits this action.' },
      { kind: 'AUTHENTICITY', status: 'PASS', reasonCodes: [], safeExplanation: 'Synthetic authenticity check passed.' },
      { kind: 'FRESHNESS', status: 'PASS', reasonCodes: [], safeExplanation: 'Synthetic freshness window is valid.' },
      { kind: 'REPLAY', status: replay, reasonCodes: replay === 'FAIL' ? ['REPLAY_DETECTED'] : [], safeExplanation: replay === 'FAIL' ? 'The request identifier was already accepted.' : 'Synthetic replay check found no reuse.' },
      { kind: 'CONTRACT', status: contract === 'UNAVAILABLE' ? 'FAIL' : contract, reasonCodes: contract === 'FAIL' ? ['CONTRACT_DRIFT'] : [], safeExplanation: contract === 'FAIL' ? 'The payload does not match the accepted contract.' : 'Synthetic contract matched the expected shape.' },
      { kind: 'EGRESS', status: 'PASS', reasonCodes: transformed ? ['EGRESS_TRANSFORMATION_REQUIRED'] : [], safeExplanation: transformed ? 'A contact field must be redacted before delivery.' : 'Synthetic egress policy allows the submitted fields.' },
      { kind: 'CREDENTIAL_RESOLVABILITY', status: 'PASS', reasonCodes: [], safeExplanation: 'Synthetic credential reference resolved.' },
      { kind: 'CONNECTOR_SAFETY', status: connector, reasonCodes: [], safeExplanation: connector === 'UNAVAILABLE' ? 'The destination connector is not available for this hold.' : 'Synthetic destination connector is available.' },
    ],
    decided_at: WHEN,
    policy_version: 'boundary.demo.v1',
    duplicate_delivery: duplicate,
    identity: {
      status: identity,
      principal: 'quayforge.orders',
      issuer: 'synthetic.boundarymesh.demo',
      method: 'JWT_BEARER',
      keyId: 'synthetic.demo.1',
      explanation: identity === 'FAIL' ? 'Synthetic identity is invalid for this crossing.' : 'Source identity is recognized for this integration.',
    },
    authorization: {
      status: 'PASS',
      grantedScopes: ['boundary.evaluate'],
      explanation: 'The requested action is permitted for this identity.',
    },
    contract: {
      id: 'quayforge.orders',
      version: 'v1',
      type: 'ORDER',
      status: held ? 'DRIFT' : 'VALID',
      classification: 'STANDARD',
      truncatedCount: 0,
      violations: held ? [{ path: '/extra', keyword: 'additionalProperties', expected: 'absent', actual: 'present' }] : [],
    },
    egress: {
      policyId: 'destination.egress',
      policyVersion: 'v1',
      destination: 'Rivertide',
      fields: transformed
        ? [{ path: '/email', action: 'REDACT', ruleId: 'email.redact', explanation: 'The email may not leave for this destination.' }]
        : [],
      redactedPaths: transformed ? ['/email'] : [],
      deniedPaths: [],
      implicitDenyPaths: [],
      transformationCount: transformed ? 1 : 0,
    },
    enforcement_trace: [
      { kind: 'IDENTITY', status: identity, explanation: identity === 'FAIL' ? 'Synthetic identity is invalid.' : 'Source identity recognized.', reference: 'quayforge.orders' },
      { kind: 'AUTHORIZATION', status: 'PASS', explanation: 'Action permitted.', reference: 'boundary.evaluate' },
      { kind: 'AUTHENTICITY', status: 'PASS', explanation: 'Signature matched.', reference: 'synthetic.demo.1' },
      { kind: 'FRESHNESS', status: 'PASS', explanation: 'Timestamp is inside the window.', reference: WHEN },
      { kind: 'REPLAY', status: replay, explanation: replay === 'FAIL' ? 'Identifier already accepted.' : 'Identifier is new.', reference: transactionId },
      { kind: 'CONTRACT', status: held ? 'FAIL' : 'PASS', explanation: held ? 'Contract drift.' : 'Contract matched.', reference: 'quayforge.orders' },
      { kind: 'EGRESS', status: 'PASS', explanation: transformed ? 'Transformation required.' : 'Policy allows the fields.', reference: 'destination.egress' },
    ],
    classification: 'STANDARD',
    source_payload: '{"order":"4108","email":"ada@example.com"}',
    transformed_payload: transformed ? '{"order":"4108","email":"[redacted]"}' : '{"order":"4108","email":"ada@example.com"}',
  }
}

function decisionResponse(scenario: string, transactionId = scenario): ApiResponse<EvaluateResponse> {
  return envelope(decide(outcomeFor(scenario), transactionId, scenario === 'duplicate'))
}

function attestationFor(id: string, source: string, destination: string, outcome: string, claim: string): AttestationRecord {
  return {
    id,
    transactionId: id,
    integrationId: 'quayforge.orders',
    source,
    destination,
    boundaryOutcome: outcome,
    contractId: 'quayforge.orders',
    contractVersion: 'v1',
    egressPolicyId: 'destination.egress',
    transformationCount: outcome === 'ALLOW_WITH_TRANSFORMATION' ? 1 : 0,
    credentialReference: 'covecrm-api:v2',
    consentResult: 'GRANTED',
    connectorRoute: 'synthetic.worker',
    deliveryResult: outcome === 'QUARANTINE' || outcome === 'REJECT' ? 'NOT_ATTEMPTED' : 'DELIVERED',
    auditChainId: 'quayforge-orders',
    auditSequence: 4,
    auditHeadShort: 'ab12cd34',
    algorithm: 'ECDSA P256',
    keyId: 'synthetic.demo.1',
    keyVersion: '1',
    status: 'VALID',
    signaturePresent: true,
    controlResults: { IDENTITY: 'PASS', AUTHORIZATION: 'PASS', REPLAY: outcome === 'REJECT' ? 'FAIL' : 'PASS' },
    claim,
  }
}

const ATTESTATIONS: AttestationRecord[] = PORTFOLIO_EXPLORER_CROSSINGS.map((crossing) =>
  attestationFor(`att${crossing.id}`, crossing.source, crossing.destination, crossing.outcome, crossing.summary),
)

const QUARANTINE: QuarantineItemRecord[] = [
  {
    id: 'q4120',
    transactionId: 'att4120',
    destination: 'Rivertide',
    reason: 'Contract drift held the crossing. The payload is outside the accepted contract.',
    status: 'OPEN',
    attempts: 1,
    maxAttempts: 3,
    eligible: true,
    credentialReference: 'covecrm-api:v2',
    connectorState: 'IDLE',
    createdAt: WHEN,
  },
  {
    id: 'q4114',
    transactionId: 'att4114',
    destination: 'Rivertide',
    reason: 'Connector unavailable after a permitted transformation.',
    status: 'OPEN',
    attempts: 2,
    maxAttempts: 3,
    eligible: false,
    credentialReference: 'lumenway-api:v1',
    connectorState: 'UNAVAILABLE',
    createdAt: WHEN,
  },
]

function chainEntries(): AuditEntryRecord[] {
  return [
    {
      sequence: 1,
      occurredAt: WHEN,
      eventType: 'BOUNDARY_DECISION',
      attributes: { outcome: 'ALLOW', integration: 'quayforge.orders' },
      previousHash: 'GENESIS',
      previousHashShort: 'GENESIS',
      currentHash: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
      currentHashShort: 'aaaaaaaa',
    },
    {
      sequence: 2,
      occurredAt: WHEN,
      eventType: 'DELIVERY_RECORDED',
      attributes: { outcome: 'ALLOW_WITH_TRANSFORMATION', integration: 'quayforge.orders' },
      previousHash: 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
      previousHashShort: 'aaaaaaaa',
      currentHash: 'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',
      currentHashShort: 'bbbbbbbb',
    },
  ]
}

function catalog(): CredentialCatalog {
  return {
    banner: BANNER,
    credentials: [
      { id: 'covecrm-api:v2', version: 'v2', status: 'ACTIVE', destination: 'CoveCRM', connector: 'CoveCRM', rotatedAt: WHEN },
      { id: 'lumenway-api:v1', version: 'v1', status: 'ACTIVE', destination: 'Lumenway', connector: 'Lumenway', rotatedAt: WHEN },
      { id: 'rivertide-api', version: 'v1', status: 'ACTIVE', destination: 'Rivertide', connector: 'Rivertide', rotatedAt: WHEN },
    ],
    consents: [
      { id: 'cove.orders', destination: 'CoveCRM', operation: 'orders.create', status: 'GRANTED', kind: 'CONNECTOR', permissions: ['orders.create'] },
      { id: 'lumen.fulfillment', destination: 'Lumenway', operation: 'fulfillment.create', status: 'GRANTED', kind: 'CONNECTOR', permissions: ['fulfillment.create'] },
    ],
  }
}

function delivery(scenario: string): DeliveryLabRecord {
  const outcome = outcomeFor(scenario)
  const duplicate = scenario === 'duplicate'
  const decision = decide(outcome, `delivery.${scenario}`, duplicate)
  const allowed = outcome === 'ALLOW' || outcome === 'ALLOW_WITH_TRANSFORMATION'
  const kind: DeliveryResultKind = duplicate
    ? 'DUPLICATE_SUPPRESSED'
    : outcome === 'QUARANTINE'
      ? 'QUARANTINED'
      : allowed
        ? 'DELIVERED'
        : 'NOT_ATTEMPTED'
  return {
    banner: duplicate ? 'SYNTHETIC DEMO. Second delivery suppressed in this browser.' : BANNER,
    stages: ['Boundary Decision', 'Delivery Instruction', 'Credential / Consent', 'Camel Route', 'Connector', 'Resilience', 'Audit / Attestation'],
    decision,
    instruction: allowed
      ? {
          eventId: `delivery.${scenario}`,
          transactionId: `delivery.${scenario}`,
          destination: scenario.includes('cove') ? 'COVECRM' : 'LUMENWAY',
          outcome,
          payloadJson: decision.transformed_payload ?? '{}',
          contractReference: 'quayforge.orders',
        }
      : null,
    delivery: {
      kind,
      attemptCount: scenario === 'server-error' ? 3 : 1,
      providerCategory: allowed ? 'SYNTHETIC' : 'NONE',
      explanation: allowed ? 'The worker recorded the instruction in this browser.' : 'No connector call was made.',
      connectorId: allowed ? 'synthetic.worker' : null,
      routeId: allowed ? 'synthetic.route' : null,
      circuitState: scenario === 'circuit-open' ? 'OPEN' : 'CLOSED',
      httpStatus: allowed ? 200 : null,
      providerCalled: false,
    },
    credential: {
      reference: scenario.includes('credential-missing') ? null : 'covecrm-api:v2',
      status: scenario.includes('credential-missing') ? 'MISSING' : 'RESOLVED',
      explanation: 'Secret values stay out of the browser. Only the reference is shown.',
    },
    consent: {
      id: 'cove.orders',
      status: scenario.includes('consent-missing') ? 'MISSING' : 'GRANTED',
      permissions: ['orders.create'],
      explanation: 'Consent is a permission check, not proof that a secret exists.',
    },
    quarantine: outcome === 'QUARANTINE'
      ? { id: 'q4120', reason: 'Held before delivery.', status: 'OPEN', attempts: 1, eligible: false, destination: 'Rivertide' }
      : null,
    evidence: {
      attestationId: 'att4108',
      attestationStatus: 'VALID',
      auditChainId: 'quayforge-orders',
    },
    requestId: REQUEST,
  }
}

function edge(id: string, source: string, destination: string, outcome: string, status: string): BoundaryGraphEdge {
  return {
    id,
    source,
    destination,
    integrationId: 'quayforge.orders',
    identityMechanism: 'JWT',
    contractReference: 'quayforge.orders',
    egressPolicy: 'destination.egress',
    credentialReference: 'covecrm-api:v2',
    consentState: 'GRANTED',
    connectorRoute: 'synthetic.worker',
    status,
    configured: true,
    observed: true,
    recentOutcome: outcome,
    recentDelivery: outcome === 'REJECT' || outcome === 'QUARANTINE' ? 'NOT_ATTEMPTED' : 'DELIVERED',
    quarantineCount: outcome === 'QUARANTINE' ? 1 : 0,
    auditChainId: 'quayforge-orders',
    attestationId: `att${id}`,
  }
}

const GRAPH: BoundaryGraphRecord = {
  banner: BANNER,
  nodes: [
    { id: 'QUAYFORGE', label: 'QuayForge', kind: 'SOURCE', synthetic: true },
    { id: 'M365', label: 'Microsoft 365', kind: 'SOURCE', synthetic: true },
    { id: 'HELIX365', label: 'Helix 365', kind: 'SOURCE', synthetic: true },
    { id: 'BOUNDARYMESH', label: 'BoundaryMesh', kind: 'BOUNDARY', synthetic: true },
    { id: 'COVECRM', label: 'CoveCRM', kind: 'DESTINATION', synthetic: true },
    { id: 'LUMENWAY', label: 'Lumenway', kind: 'DESTINATION', synthetic: true },
    { id: 'RIVERTIDE', label: 'Rivertide', kind: 'DESTINATION', synthetic: true },
  ],
  edges: [
    edge('4108', 'QUAYFORGE', 'RIVERTIDE', 'ALLOW', 'ACTIVE'),
    edge('4114', 'QUAYFORGE', 'LUMENWAY', 'ALLOW_WITH_TRANSFORMATION', 'ACTIVE'),
    edge('4120', 'HELIX365', 'COVECRM', 'QUARANTINE', 'QUARANTINED'),
    edge('4126', 'M365', 'COVECRM', 'REJECT', 'BLOCKED'),
  ],
}

function chainRecord(chainId: string): AuditChainRecord {
  const entries = chainEntries()
  return {
    banner: BANNER,
    chainId,
    entryCount: entries.length,
    continuity: 'INTACT',
    entries,
  }
}

export async function fetchHealth(): Promise<ApiResponse<HealthResponse>> {
  return envelope({ status: 'UP', service: 'BoundaryMesh', truth: 'SYNTHETIC DEMO' })
}

export async function fetchReady(): Promise<ApiResponse<ReadyResponse>> {
  return envelope({ status: 'READY', detail: 'Synthetic portfolio is ready in this browser.' })
}

export async function issueLocalToken(): Promise<string> {
  return 'synthetic.demo.token'
}

export async function evaluateBoundary(request: EvaluateRequest): Promise<ApiResponse<EvaluateResponse>> {
  return envelope(decide(outcomeFromRequest(request), request.transaction_id, false, request.controls))
}

export async function evaluateIdentity(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  if (scenario === 'EXPIRED' || scenario === 'WRONG_ISSUER' || scenario === 'WRONG_AUDIENCE') {
    throw new BoundaryMeshApiError('unauthorized', 'Authentication cannot be established.', {
      status: 401,
      requestId: REQUEST,
    })
  }
  if (scenario === 'MISSING_PERMISSION') {
    const response = decide('REJECT', scenario)
    response.authorization = {
      status: 'FAIL',
      grantedScopes: [],
      explanation: 'The token is authentic. The required permission is missing.',
    }
    response.reason_codes = [{ code: 'PERMISSION_MISSING', description: 'boundary.evaluate was not granted.' }]
    return envelope(response)
  }
  return decisionResponse(scenario)
}

export async function runContractLab(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  return decisionResponse(scenario)
}

export async function runEgressLab(_destination: string, scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  return decisionResponse(scenario)
}

export async function evaluateCrossing(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  return decisionResponse(scenario)
}

export async function runWebhookLab(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  return decisionResponse(scenario)
}

export async function runDeliveryLab(scenario: string): Promise<DeliveryLabRecord> {
  return delivery(scenario)
}

export async function fetchCredentialCatalog(): Promise<CredentialCatalog> {
  return catalog()
}

export async function runCredentialLab(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  return decisionResponse(scenario)
}

export async function rotateCoveCredential(): Promise<CredentialCatalog> {
  throw new BoundaryMeshApiError('forbidden', 'Rotation is not available in the public demo.', {
    status: 403,
    requestId: REQUEST,
  })
}

export async function fetchQuarantineList(): Promise<QuarantineListRecord> {
  return { banner: BANNER, count: QUARANTINE.length, items: QUARANTINE }
}

export async function fetchQuarantineItem(id: string): Promise<QuarantineItemRecord> {
  const found = QUARANTINE.find((item) => item.id === id)
  if (!found) {
    throw new BoundaryMeshApiError('malformed', 'That quarantine item is not in this demo.', {
      status: 404,
      requestId: REQUEST,
    })
  }
  return found
}

export async function reprocessQuarantine(): Promise<DeliveryLabRecord> {
  throw new BoundaryMeshApiError('forbidden', 'Reprocess is not available in the public demo.', {
    status: 403,
    requestId: REQUEST,
  })
}

export async function fetchAuditChains(): Promise<AuditChainListRecord> {
  return { banner: BANNER, chains: ['quayforge-orders', 'helix-calendar-crm'] }
}

export async function fetchAuditChain(integrationId: string): Promise<AuditChainRecord> {
  return chainRecord(integrationId)
}

export async function verifyAuditChain(integrationId: string): Promise<AuditVerifyRecord> {
  return {
    banner: BANNER,
    chainId: integrationId,
    status: 'VALID',
    entriesChecked: 2,
    breakSequence: null,
    explanation: 'Continuity holds for this synthetic chain. Production was not changed.',
  }
}

export async function fetchAttestations(): Promise<AttestationListRecord> {
  return { banner: BANNER, items: ATTESTATIONS }
}

export async function fetchAttestation(id: string): Promise<AttestationRecord> {
  const found = ATTESTATIONS.find((item) => item.id === id) ?? ATTESTATIONS[0]
  if (!found) {
    throw new BoundaryMeshApiError('malformed', 'That attestation is not in this demo.', {
      status: 404,
      requestId: REQUEST,
    })
  }
  return found
}

export async function verifyAttestation(id: string): Promise<AttestationVerifyRecord> {
  return { id, status: 'VALID', explanation: 'The synthetic signature matches. Production was not changed.' }
}

export async function fetchBoundaryGraph(): Promise<BoundaryGraphRecord> {
  return GRAPH
}

export async function fetchAuditTamperDemo(): Promise<{
  banner: string
  original: AuditVerifyRecord
  tampered: AuditVerifyRecord
}> {
  return {
    banner: BANNER,
    original: {
      banner: BANNER,
      chainId: 'quayforge.orders',
      status: 'VALID',
      entriesChecked: 2,
      breakSequence: null,
      explanation: 'The original copy is intact.',
    },
    tampered: {
      banner: BANNER,
      chainId: 'quayforge.orders',
      status: 'HASH_MISMATCH',
      entriesChecked: 2,
      breakSequence: 2,
      explanation: 'The isolated copy was changed. The stored chain was not.',
    },
  }
}

export async function fetchAttestationTamperDemo(): Promise<{
  banner: string
  original: AttestationVerifyRecord | null
  tampered: AttestationVerifyRecord | null
}> {
  return {
    banner: BANNER,
    original: { id: 'att4108', status: 'VALID', explanation: 'The original signature matches.' },
    tampered: { id: 'att4108', status: 'INVALID', explanation: 'The isolated copy was changed. The stored attestation was not.' },
  }
}
