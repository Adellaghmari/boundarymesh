import { CONTROL_CATALOG } from '@/domain/labels'
import type {
  BoundaryEventRecord,
  BoundaryOutcome,
  ControlKind,
  ControlResult,
  ControlStatus,
  EvaluateRequest,
  EvaluateResponse,
} from '@/domain/types'
import { BOUNDARY_OUTCOMES, CONTROL_KINDS, CONTROL_STATUSES } from '@/domain/types'

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
