import { CONTROL_CATALOG } from '@/domain/labels'
import type {
  BoundaryEventRecord,
  BoundaryOutcome,
  ConsentCatalogRow,
  ConsentEvidenceView,
  ControlKind,
  ControlResult,
  ControlStatus,
  CredentialCatalog,
  CredentialCatalogRow,
  CredentialEvidenceView,
  DeliveryInstructionView,
  DeliveryLabRecord,
  DeliveryResultKind,
  DeliveryView,
  EvaluateRequest,
  EvaluateResponse,
  EvidenceSummaryView,
  QuarantineItemRecord,
  QuarantineListRecord,
  QuarantineSummaryView,
  AttestationListRecord,
  AttestationRecord,
  AttestationVerifyRecord,
  AuditChainListRecord,
  AuditChainRecord,
  AuditEntryRecord,
  AuditVerifyRecord,
  BoundaryGraphEdge,
  BoundaryGraphNode,
  BoundaryGraphRecord,
} from '@/domain/types'
import {
  BOUNDARY_OUTCOMES,
  CONTROL_KINDS,
  CONTROL_STATUSES,
  DELIVERY_RESULT_KINDS,
} from '@/domain/types'

export function isBoundaryOutcome(value: unknown): value is BoundaryOutcome {
  return typeof value === 'string' && (BOUNDARY_OUTCOMES as readonly string[]).includes(value)
}

export function isControlStatus(value: unknown): value is ControlStatus {
  return typeof value === 'string' && (CONTROL_STATUSES as readonly string[]).includes(value)
}

export function isControlKind(value: unknown): value is ControlKind {
  return typeof value === 'string' && (CONTROL_KINDS as readonly string[]).includes(value)
}

function normalizeControlKey(key: string): ControlKind | undefined {
  const upper = key.toUpperCase()
  if (isControlKind(upper)) {
    return upper
  }
  return undefined
}

export function parseAppliedControls(raw: unknown): Partial<Record<ControlKind, ControlStatus>> {
  if (!raw || typeof raw !== 'object') {
    return {}
  }
  const applied: Partial<Record<ControlKind, ControlStatus>> = {}
  for (const [key, value] of Object.entries(raw)) {
    const kind = normalizeControlKey(key)
    if (kind && isControlStatus(value)) {
      applied[kind] = value
    }
  }
  return applied
}

export function controlResultsFromRequest(
  request: EvaluateRequest,
  applied: Partial<Record<ControlKind, ControlStatus>>,
): ControlResult[] {
  return CONTROL_CATALOG.map((entry) => {
    const input =
      request.controls[entry.kind.toLowerCase()] ??
      request.controls[entry.kind] ??
      request.controls[entry.kind.toLowerCase().replace('_', '')]
    const backendStatus = applied[entry.kind]
    return {
      kind: entry.kind,
      status: backendStatus ?? input?.status ?? 'UNAVAILABLE',
      reasonCodes: input?.reason_codes ?? [],
      safeExplanation: input?.safe_explanation ?? entry.summary,
    }
  })
}

export function toBoundaryEvent(input: {
  scenarioId: string
  scenarioLabel: string
  request: EvaluateRequest
  response: EvaluateResponse
  requestId: string
}): BoundaryEventRecord {
  const applied = input.response.applied_controls
  return {
    transactionId: input.response.transaction_id,
    scenarioId: input.scenarioId,
    scenarioLabel: input.scenarioLabel,
    sourceSystem: input.request.source_system,
    destinationSystem: input.request.destination_system,
    integrationId: input.request.integration_id,
    outcome: input.response.outcome,
    decidedAt: input.response.decided_at,
    policyVersion: input.response.policy_version,
    reasonCodes: input.response.reason_codes,
    appliedControls: applied,
    controlResults: controlResultsFromDecision(input.response, input.request),
    banner: input.response.banner,
    requestId: input.requestId,
    ingressKind: input.response.ingress_kind,
    duplicateDelivery: input.response.duplicate_delivery,
    identity: input.response.identity,
    authorization: input.response.authorization,
    contract: input.response.contract,
    egress: input.response.egress,
    enforcementTrace: input.response.enforcement_trace,
    classification: input.response.classification,
    sourcePayload: input.response.source_payload,
    transformedPayload: input.response.transformed_payload,
  }
}

export function toDecisionEvent(input: {
  scenarioId: string
  scenarioLabel: string
  sourceSystem: EvaluateRequest['source_system']
  destinationSystem: EvaluateRequest['destination_system']
  integrationId: string
  response: EvaluateResponse
  requestId: string
}): BoundaryEventRecord {
  return {
    transactionId: input.response.transaction_id,
    scenarioId: input.scenarioId,
    scenarioLabel: input.scenarioLabel,
    sourceSystem: input.sourceSystem,
    destinationSystem: input.destinationSystem,
    integrationId: input.integrationId,
    outcome: input.response.outcome,
    decidedAt: input.response.decided_at,
    policyVersion: input.response.policy_version,
    reasonCodes: input.response.reason_codes,
    appliedControls: input.response.applied_controls,
    controlResults: controlResultsFromDecision(input.response),
    banner: input.response.banner,
    requestId: input.requestId,
    ingressKind: input.response.ingress_kind,
    duplicateDelivery: input.response.duplicate_delivery,
    identity: input.response.identity,
    authorization: input.response.authorization,
    contract: input.response.contract,
    egress: input.response.egress,
    enforcementTrace: input.response.enforcement_trace,
    classification: input.response.classification,
    sourcePayload: input.response.source_payload,
    transformedPayload: input.response.transformed_payload,
  }
}

export function controlResultsFromDecision(
  response: EvaluateResponse,
  request?: EvaluateRequest,
): ControlResult[] {
  if (response.controls && response.controls.length > 0) {
    return CONTROL_CATALOG.map((entry) => {
      const found = response.controls?.find((item) => item.kind === entry.kind)
      return {
        kind: entry.kind,
        status: found?.status ?? response.applied_controls[entry.kind] ?? 'NOT_APPLICABLE',
        reasonCodes: found?.reasonCodes ?? [],
        safeExplanation: found?.safeExplanation ?? entry.summary,
      }
    })
  }
  if (request) {
    return controlResultsFromRequest(request, response.applied_controls)
  }
  return CONTROL_CATALOG.map((entry) => ({
    kind: entry.kind,
    status: response.applied_controls[entry.kind] ?? 'NOT_APPLICABLE',
    reasonCodes: [],
    safeExplanation: entry.summary,
  }))
}

export function assertEvaluateResponse(value: unknown): EvaluateResponse {
  if (!value || typeof value !== 'object') {
    throw new Error('MALFORMED_RESPONSE')
  }
  const body = value as Record<string, unknown>
  if (typeof body.transaction_id !== 'string' || body.transaction_id.length === 0) {
    throw new Error('MALFORMED_RESPONSE')
  }
  if (!isBoundaryOutcome(body.outcome)) {
    throw new Error('MALFORMED_RESPONSE')
  }
  if (typeof body.policy_version !== 'string' || typeof body.decided_at !== 'string') {
    throw new Error('MALFORMED_RESPONSE')
  }
  const reasonCodes = Array.isArray(body.reason_codes)
    ? body.reason_codes.flatMap((item) => {
        if (!item || typeof item !== 'object') {
          return []
        }
        const row = item as Record<string, unknown>
        if (typeof row.code !== 'string') {
          return []
        }
        return [
          {
            code: row.code,
            description: typeof row.description === 'string' ? row.description : '',
          },
        ]
      })
    : []
  return {
    banner: typeof body.banner === 'string' ? body.banner : 'SYNTHETIC DEMO',
    transaction_id: body.transaction_id,
    outcome: body.outcome,
    reason_codes: reasonCodes,
    applied_controls: parseAppliedControls(body.applied_controls),
    controls: parseControls(body.controls),
    decided_at: body.decided_at,
    policy_version: body.policy_version,
    ingress_kind: body.ingress_kind as EvaluateResponse['ingress_kind'],
    duplicate_delivery: body.duplicate_delivery === true,
    identity: parseIdentity(body.identity),
    authorization: parseAuthorization(body.authorization),
    contract: parseContract(body.contract),
    egress: parseEgress(body.egress),
    enforcement_trace: parseTrace(body.enforcement_trace),
    classification: typeof body.classification === 'string' ? body.classification : undefined,
    source_payload: typeof body.source_payload === 'string' ? body.source_payload : undefined,
    transformed_payload: typeof body.transformed_payload === 'string' ? body.transformed_payload : undefined,
  }
}

function parseControls(raw: unknown): EvaluateResponse['controls'] {
  if (!Array.isArray(raw)) {
    return undefined
  }
  const controls = raw.flatMap((item) => {
    if (!item || typeof item !== 'object') {
      return []
    }
    const row = item as Record<string, unknown>
    if (!isControlKind(row.kind) || !isControlStatus(row.status)) {
      return []
    }
    const reasonCodes = Array.isArray(row.reasonCodes)
      ? row.reasonCodes.flatMap((reason) => {
          if (typeof reason === 'string') {
            return [reason]
          }
          if (reason && typeof reason === 'object' && typeof (reason as { code?: unknown }).code === 'string') {
            return [(reason as { code: string }).code]
          }
          return []
        })
      : []
    return [
      {
        kind: row.kind,
        status: row.status,
        reasonCodes,
        safeExplanation: typeof row.safeExplanation === 'string' ? row.safeExplanation : '',
      },
    ]
  })
  return controls.length > 0 ? controls : undefined
}

function parseIdentity(raw: unknown): EvaluateResponse['identity'] {
  if (!raw || typeof raw !== 'object') {
    return undefined
  }
  const body = raw as Record<string, unknown>
  if (!isControlStatus(body.status)) {
    return undefined
  }
  return {
    status: body.status,
    principal: typeof body.principal === 'string' ? body.principal : null,
    issuer: typeof body.issuer === 'string' ? body.issuer : null,
    method: typeof body.method === 'string' ? body.method : null,
    keyId: typeof body.keyId === 'string' ? body.keyId : null,
    explanation: typeof body.explanation === 'string' ? body.explanation : null,
  }
}

function parseContract(raw: unknown): EvaluateResponse['contract'] {
  if (!raw || typeof raw !== 'object') {
    return undefined
  }
  const body = raw as Record<string, unknown>
  const violations = Array.isArray(body.violations)
    ? body.violations.flatMap((item) => {
        if (!item || typeof item !== 'object') {
          return []
        }
        const row = item as Record<string, unknown>
        if (typeof row.path !== 'string') {
          return []
        }
        return [
          {
            path: row.path,
            keyword: typeof row.keyword === 'string' ? row.keyword : '',
            expected: typeof row.expected === 'string' ? row.expected : '',
            actual: typeof row.actual === 'string' ? row.actual : '',
          },
        ]
      })
    : []
  return {
    id: typeof body.id === 'string' ? body.id : null,
    version: typeof body.version === 'string' ? body.version : null,
    type: typeof body.type === 'string' ? body.type : null,
    status: typeof body.status === 'string' ? body.status : null,
    classification: typeof body.classification === 'string' ? body.classification : null,
    truncatedCount: typeof body.truncated_count === 'number' ? body.truncated_count : 0,
    violations,
  }
}

function parseEgress(raw: unknown): EvaluateResponse['egress'] {
  if (!raw || typeof raw !== 'object') {
    return undefined
  }
  const body = raw as Record<string, unknown>
  const fields = Array.isArray(body.fields)
    ? body.fields.flatMap((item) => {
        if (!item || typeof item !== 'object') {
          return []
        }
        const row = item as Record<string, unknown>
        if (typeof row.path !== 'string' || typeof row.action !== 'string') {
          return []
        }
        return [
          {
            path: row.path,
            action: row.action,
            ruleId: typeof row.rule_id === 'string' ? row.rule_id : '',
            explanation: typeof row.explanation === 'string' ? row.explanation : '',
          },
        ]
      })
    : []
  return {
    policyId: typeof body.policy_id === 'string' ? body.policy_id : '',
    policyVersion: typeof body.policy_version === 'string' ? body.policy_version : '',
    destination: typeof body.destination === 'string' ? body.destination : '',
    fields,
    redactedPaths: stringList(body.redacted_paths),
    deniedPaths: stringList(body.denied_paths),
    implicitDenyPaths: stringList(body.implicit_deny_paths),
    transformationCount: typeof body.transformation_count === 'number' ? body.transformation_count : 0,
  }
}

function parseTrace(raw: unknown): EvaluateResponse['enforcement_trace'] {
  if (!Array.isArray(raw)) {
    return undefined
  }
  const steps = raw.flatMap((item) => {
    if (!item || typeof item !== 'object') {
      return []
    }
    const row = item as Record<string, unknown>
    if (typeof row.kind !== 'string' || !isControlStatus(row.status)) {
      return []
    }
    return [
      {
        kind: row.kind,
        status: row.status,
        explanation: typeof row.explanation === 'string' ? row.explanation : '',
        reference: typeof row.reference === 'string' ? row.reference : '',
      },
    ]
  })
  return steps.length > 0 ? steps : undefined
}

function stringList(raw: unknown): string[] {
  return Array.isArray(raw) ? raw.filter((item): item is string => typeof item === 'string') : []
}

function isDeliveryKind(value: unknown): value is DeliveryResultKind {
  return typeof value === 'string' && (DELIVERY_RESULT_KINDS as readonly string[]).includes(value)
}

function asRecord(value: unknown): Record<string, unknown> | null {
  return value && typeof value === 'object' ? (value as Record<string, unknown>) : null
}

export function assertNoSecretMarker(value: unknown): void {
  const text = JSON.stringify(value)
  if (text.includes('TEST-SECRET-') || text.includes('Bearer ')) {
    throw new Error('SECRET_LEAK')
  }
}

export function assertDeliveryLabResponse(value: unknown, requestId: string): DeliveryLabRecord {
  const body = asRecord(value)
  if (!body) {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(body)
  const decision = assertEvaluateResponse(body.decision)
  return {
    banner: typeof body.banner === 'string' ? body.banner : 'SYNTHETIC DEMO',
    stages: stringList(body.stages),
    decision,
    instruction: parseInstruction(body.instruction),
    delivery: parseDelivery(body.delivery),
    credential: parseCredentialEvidence(body.credential),
    consent: parseConsentEvidence(body.consent),
    quarantine: parseQuarantineSummary(body.quarantine),
    evidence: parseEvidence(body.evidence),
    requestId,
  }
}

function parseEvidence(raw: unknown): EvidenceSummaryView | null {
  const body = asRecord(raw)
  if (!body || typeof body.attestation_id !== 'string') {
    return null
  }
  return {
    attestationId: body.attestation_id,
    attestationStatus: typeof body.attestation_status === 'string' ? body.attestation_status : '',
    auditChainId: typeof body.audit_chain_id === 'string' ? body.audit_chain_id : '',
  }
}

function parseInstruction(raw: unknown): DeliveryInstructionView | null {
  const body = asRecord(raw)
  if (!body || typeof body.transaction_id !== 'string') {
    return null
  }
  return {
    eventId: typeof body.event_id === 'string' ? body.event_id : '',
    transactionId: body.transaction_id,
    destination: typeof body.destination === 'string' ? body.destination : '',
    outcome: typeof body.outcome === 'string' ? body.outcome : '',
    payloadJson: typeof body.payload_json === 'string' ? body.payload_json : '',
    contractReference: typeof body.contract_reference === 'string' ? body.contract_reference : '',
  }
}

function parseDelivery(raw: unknown): DeliveryView | null {
  const body = asRecord(raw)
  if (!body || !isDeliveryKind(body.kind)) {
    return null
  }
  return {
    kind: body.kind,
    attemptCount: typeof body.attempt_count === 'number' ? body.attempt_count : 0,
    providerCategory: typeof body.provider_category === 'string' ? body.provider_category : '',
    reason: typeof body.reason === 'string' ? body.reason : null,
    explanation: typeof body.explanation === 'string' ? body.explanation : '',
    connectorId: typeof body.connector_id === 'string' ? body.connector_id : null,
    routeId: typeof body.route_id === 'string' ? body.route_id : null,
    circuitState: typeof body.circuit_state === 'string' ? body.circuit_state : null,
    httpStatus: typeof body.http_status === 'number' ? body.http_status : null,
    providerCalled: body.provider_called === true,
  }
}

function parseCredentialEvidence(raw: unknown): CredentialEvidenceView {
  const body = asRecord(raw)
  return {
    reference: typeof body?.reference === 'string' ? body.reference : null,
    status: typeof body?.status === 'string' ? body.status : 'UNKNOWN',
    explanation: typeof body?.explanation === 'string' ? body.explanation : '',
  }
}

function parseConsentEvidence(raw: unknown): ConsentEvidenceView {
  const body = asRecord(raw)
  return {
    id: typeof body?.id === 'string' ? body.id : null,
    status: typeof body?.status === 'string' ? body.status : 'UNKNOWN',
    permissions: stringList(body?.permissions),
    explanation: typeof body?.explanation === 'string' ? body.explanation : '',
  }
}

function parseQuarantineSummary(raw: unknown): QuarantineSummaryView | null {
  const body = asRecord(raw)
  if (!body || typeof body.id !== 'string') {
    return null
  }
  return {
    id: body.id,
    reason: typeof body.reason === 'string' ? body.reason : '',
    status: typeof body.status === 'string' ? body.status : '',
    attempts: typeof body.attempts === 'number' ? body.attempts : 0,
    eligible: body.eligible === true,
    destination: typeof body.destination === 'string' ? body.destination : '',
  }
}

export function assertCredentialCatalog(value: unknown): CredentialCatalog {
  const body = asRecord(value)
  if (!body) {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(body)
  return {
    banner: typeof body.banner === 'string' ? body.banner : 'LOCAL SECURITY MODEL / SYNTHETIC DEMO',
    credentials: Array.isArray(body.credentials)
      ? body.credentials.flatMap((item) => {
          const row = asRecord(item)
          if (!row || typeof row.id !== 'string') {
            return []
          }
          const mapped: CredentialCatalogRow = {
            id: row.id,
            version: typeof row.version === 'string' ? row.version : '',
            status: typeof row.status === 'string' ? row.status : '',
            destination: typeof row.destination === 'string' ? row.destination : '',
            connector: typeof row.connector === 'string' ? row.connector : '',
            rotatedAt: typeof row.rotated_at === 'string' ? row.rotated_at : '',
          }
          return [mapped]
        })
      : [],
    consents: Array.isArray(body.consents)
      ? body.consents.flatMap((item) => {
          const row = asRecord(item)
          if (!row || typeof row.id !== 'string') {
            return []
          }
          const mapped: ConsentCatalogRow = {
            id: row.id,
            destination: typeof row.destination === 'string' ? row.destination : '',
            operation: typeof row.operation === 'string' ? row.operation : '',
            status: typeof row.status === 'string' ? row.status : '',
            kind: typeof row.kind === 'string' ? row.kind : '',
            permissions: stringList(row.permissions),
          }
          return [mapped]
        })
      : [],
  }
}

export function assertQuarantineList(value: unknown): QuarantineListRecord {
  const body = asRecord(value)
  if (!body) {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(body)
  return {
    banner: typeof body.banner === 'string' ? body.banner : 'LOCAL / SYNTHETIC SESSION DATA',
    count: typeof body.count === 'number' ? body.count : 0,
    items: Array.isArray(body.items) ? body.items.flatMap((item) => parseQuarantineItem(item) ?? []) : [],
  }
}

export function assertQuarantineItem(value: unknown): QuarantineItemRecord {
  const item = parseQuarantineItem(value)
  if (!item) {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(item)
  return item
}

function parseQuarantineItem(raw: unknown): QuarantineItemRecord | null {
  const body = asRecord(raw)
  if (!body || typeof body.id !== 'string') {
    return null
  }
  return {
    id: body.id,
    transactionId: typeof body.transaction_id === 'string' ? body.transaction_id : '',
    destination: typeof body.destination === 'string' ? body.destination : '',
    reason: typeof body.reason === 'string' ? body.reason : '',
    status: typeof body.status === 'string' ? body.status : '',
    attempts: typeof body.attempts === 'number' ? body.attempts : 0,
    maxAttempts: typeof body.max_attempts === 'number' ? body.max_attempts : 3,
    eligible: body.eligible === true,
    credentialReference: typeof body.credential_reference === 'string' ? body.credential_reference : null,
    connectorState: typeof body.connector_state === 'string' ? body.connector_state : null,
    createdAt: typeof body.created_at === 'string' ? body.created_at : '',
  }
}

function parseAuthorization(raw: unknown): EvaluateResponse['authorization'] {
  if (!raw || typeof raw !== 'object') {
    return undefined
  }
  const body = raw as Record<string, unknown>
  if (!isControlStatus(body.status)) {
    return undefined
  }
  return {
    status: body.status,
    grantedScopes: Array.isArray(body.grantedScopes)
      ? body.grantedScopes.filter((item): item is string => typeof item === 'string')
      : [],
    explanation: typeof body.explanation === 'string' ? body.explanation : null,
  }
}

export function assertAuditChainList(value: unknown): AuditChainListRecord {
  const body = asRecord(value)
  if (!body) {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(body)
  return {
    banner: typeof body.banner === 'string' ? body.banner : 'LOCAL / SYNTHETIC SESSION DATA',
    chains: Array.isArray(body.chains)
      ? body.chains.filter((item): item is string => typeof item === 'string')
      : [],
  }
}

export function assertAuditChain(value: unknown): AuditChainRecord {
  const body = asRecord(value)
  if (!body || typeof body.chain_id !== 'string') {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(body)
  return {
    banner: typeof body.banner === 'string' ? body.banner : 'LOCAL / SYNTHETIC SESSION DATA',
    chainId: body.chain_id,
    entryCount: typeof body.entry_count === 'number' ? body.entry_count : 0,
    continuity: typeof body.continuity === 'string' ? body.continuity : '',
    entries: Array.isArray(body.entries) ? body.entries.flatMap((item) => parseAuditEntry(item) ?? []) : [],
  }
}

export function assertAuditVerify(value: unknown): AuditVerifyRecord {
  const body = asRecord(value)
  if (!body || typeof body.status !== 'string') {
    throw new Error('MALFORMED_RESPONSE')
  }
  return {
    banner: typeof body.banner === 'string' ? body.banner : '',
    chainId: typeof body.chain_id === 'string' ? body.chain_id : '',
    status: body.status,
    entriesChecked: typeof body.entries_checked === 'number' ? body.entries_checked : 0,
    breakSequence: typeof body.break_sequence === 'number' ? body.break_sequence : null,
    explanation: typeof body.explanation === 'string' ? body.explanation : '',
  }
}

function parseAuditEntry(raw: unknown): AuditEntryRecord | null {
  const body = asRecord(raw)
  if (!body || typeof body.sequence !== 'number') {
    return null
  }
  const attributes: Record<string, string> = {}
  const rawAttributes = asRecord(body.attributes)
  if (rawAttributes) {
    for (const [key, value] of Object.entries(rawAttributes)) {
      if (typeof value === 'string') {
        attributes[key] = value
      }
    }
  }
  return {
    sequence: body.sequence,
    occurredAt: typeof body.occurred_at === 'string' ? body.occurred_at : '',
    eventType: typeof body.event_type === 'string' ? body.event_type : '',
    attributes,
    previousHash: typeof body.previous_hash === 'string' ? body.previous_hash : '',
    previousHashShort: typeof body.previous_hash_short === 'string' ? body.previous_hash_short : '',
    currentHash: typeof body.current_hash === 'string' ? body.current_hash : '',
    currentHashShort: typeof body.current_hash_short === 'string' ? body.current_hash_short : '',
  }
}

export function assertAttestationList(value: unknown): AttestationListRecord {
  const body = asRecord(value)
  if (!body) {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(body)
  return {
    banner: typeof body.banner === 'string' ? body.banner : 'LOCAL / SYNTHETIC SESSION DATA',
    items: Array.isArray(body.items) ? body.items.flatMap((item) => parseAttestation(item) ?? []) : [],
  }
}

export function assertAttestation(value: unknown): AttestationRecord {
  const item = parseAttestation(value)
  if (!item) {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(item)
  return item
}

export function assertAttestationVerify(value: unknown): AttestationVerifyRecord {
  const body = asRecord(value)
  if (!body || typeof body.status !== 'string') {
    throw new Error('MALFORMED_RESPONSE')
  }
  return {
    id: typeof body.id === 'string' ? body.id : '',
    status: body.status,
    explanation: typeof body.explanation === 'string' ? body.explanation : '',
  }
}

function parseAttestation(raw: unknown): AttestationRecord | null {
  const body = asRecord(raw)
  if (!body || typeof body.id !== 'string') {
    return null
  }
  const controls: Record<string, string> = {}
  const rawControls = asRecord(body.control_results)
  if (rawControls) {
    for (const [key, value] of Object.entries(rawControls)) {
      if (typeof value === 'string') {
        controls[key] = value
      }
    }
  }
  return {
    id: body.id,
    transactionId: typeof body.transaction_id === 'string' ? body.transaction_id : '',
    integrationId: typeof body.integration_id === 'string' ? body.integration_id : '',
    source: typeof body.source === 'string' ? body.source : '',
    destination: typeof body.destination === 'string' ? body.destination : '',
    boundaryOutcome: typeof body.boundary_outcome === 'string' ? body.boundary_outcome : '',
    contractId: typeof body.contract_id === 'string' ? body.contract_id : null,
    contractVersion: typeof body.contract_version === 'string' ? body.contract_version : null,
    egressPolicyId: typeof body.egress_policy_id === 'string' ? body.egress_policy_id : null,
    transformationCount: typeof body.transformation_count === 'number' ? body.transformation_count : 0,
    credentialReference: typeof body.credential_reference === 'string' ? body.credential_reference : null,
    consentResult: typeof body.consent_result === 'string' ? body.consent_result : null,
    connectorRoute: typeof body.connector_route === 'string' ? body.connector_route : null,
    deliveryResult: typeof body.delivery_result === 'string' ? body.delivery_result : null,
    auditChainId: typeof body.audit_chain_id === 'string' ? body.audit_chain_id : '',
    auditSequence: typeof body.audit_sequence === 'number' ? body.audit_sequence : 0,
    auditHeadShort: typeof body.audit_head_short === 'string' ? body.audit_head_short : '',
    algorithm: typeof body.algorithm === 'string' ? body.algorithm : '',
    keyId: typeof body.key_id === 'string' ? body.key_id : '',
    keyVersion: typeof body.key_version === 'string' ? body.key_version : '',
    status: typeof body.status === 'string' ? body.status : '',
    signaturePresent: body.signature_present === true,
    controlResults: controls,
    claim:
      typeof body.claim === 'string'
        ? body.claim
        : 'Proves this enforcement artifact was signed and has not been modified without detection.',
  }
}

export function assertBoundaryGraph(value: unknown): BoundaryGraphRecord {
  const body = asRecord(value)
  if (!body) {
    throw new Error('MALFORMED_RESPONSE')
  }
  assertNoSecretMarker(body)
  return {
    banner: typeof body.banner === 'string' ? body.banner : 'LOCAL / SYNTHETIC SESSION DATA',
    nodes: Array.isArray(body.nodes) ? body.nodes.flatMap((item) => parseGraphNode(item) ?? []) : [],
    edges: Array.isArray(body.edges) ? body.edges.flatMap((item) => parseGraphEdge(item) ?? []) : [],
  }
}

function parseGraphNode(raw: unknown): BoundaryGraphNode | null {
  const body = asRecord(raw)
  if (!body || typeof body.id !== 'string' || typeof body.label !== 'string') {
    return null
  }
  return {
    id: body.id,
    label: body.label,
    kind: typeof body.kind === 'string' ? body.kind : 'SYSTEM',
    synthetic: body.synthetic === true,
  }
}

function parseGraphEdge(raw: unknown): BoundaryGraphEdge | null {
  const body = asRecord(raw)
  if (!body || typeof body.id !== 'string' || typeof body.source !== 'string') {
    return null
  }
  return {
    id: body.id,
    source: body.source,
    destination: typeof body.destination === 'string' ? body.destination : '',
    integrationId: typeof body.integration_id === 'string' ? body.integration_id : body.id,
    identityMechanism: typeof body.identity_mechanism === 'string' ? body.identity_mechanism : '',
    contractReference: typeof body.contract_reference === 'string' ? body.contract_reference : '',
    egressPolicy: typeof body.egress_policy === 'string' ? body.egress_policy : '',
    credentialReference: typeof body.credential_reference === 'string' ? body.credential_reference : '',
    consentState: typeof body.consent_state === 'string' ? body.consent_state : '',
    connectorRoute: typeof body.connector_route === 'string' ? body.connector_route : null,
    status: typeof body.status === 'string' ? body.status : 'CONFIGURED',
    configured: body.configured !== false,
    observed: body.observed === true,
    recentOutcome: typeof body.recent_outcome === 'string' ? body.recent_outcome : null,
    recentDelivery: typeof body.recent_delivery === 'string' ? body.recent_delivery : null,
    quarantineCount: typeof body.quarantine_count === 'number' ? body.quarantine_count : 0,
    auditChainId: typeof body.audit_chain_id === 'string' ? body.audit_chain_id : '',
    attestationId: typeof body.attestation_id === 'string' ? body.attestation_id : null,
  }
}
