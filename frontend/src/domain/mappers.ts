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
    controlResults: controlResultsFromRequest(input.request, applied),
    banner: input.response.banner,
    requestId: input.requestId,
  }
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
    decided_at: body.decided_at,
    policy_version: body.policy_version,
  }
}
