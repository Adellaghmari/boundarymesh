import type {
  BoundaryOutcome,
  ControlInput,
  ControlInputMap,
  ControlKind,
  ControlStatus,
  DestinationSystem,
  EvaluateRequest,
  SourceSystem,
} from '@/domain/types'

export type ScenarioId =
  'valid-crossing' | 'invalid-identity' | 'destination-unavailable' | 'transformation-required'

export interface EvaluationScenario {
  id: ScenarioId
  label: string
  summary: string
  expectedOutcome: BoundaryOutcome
  sourceSystem: SourceSystem
  destinationSystem: DestinationSystem
  integrationId: string
  controls: ControlInputMap
}

const CONTROL_KEYS: Record<ControlKind, string> = {
  IDENTITY: 'identity',
  AUTHORIZATION: 'authorization',
  AUTHENTICITY: 'authenticity',
  FRESHNESS: 'freshness',
  REPLAY: 'replay',
  CONTRACT: 'contract',
  EGRESS: 'egress',
  CREDENTIAL_RESOLVABILITY: 'credential_resolvability',
  CONNECTOR_SAFETY: 'connector_safety',
}

function control(
  status: ControlStatus,
  explanation: string,
  reasonCodes: string[] = [],
): ControlInput {
  return {
    status,
    reason_codes: reasonCodes,
    safe_explanation: explanation,
  }
}

function allPass(overrides: Partial<Record<ControlKind, ControlInput>> = {}): ControlInputMap {
  const base: Record<ControlKind, ControlInput> = {
    IDENTITY: control('PASS', 'Synthetic identity accepted for this Velora crossing.'),
    AUTHORIZATION: control('PASS', 'Synthetic authorization permits this integration action.'),
    AUTHENTICITY: control('PASS', 'Synthetic authenticity check passed.'),
    FRESHNESS: control('PASS', 'Synthetic freshness window is valid.'),
    REPLAY: control('PASS', 'Synthetic replay check found no reuse.'),
    CONTRACT: control('PASS', 'Synthetic contract matched the expected shape.'),
    EGRESS: control('PASS', 'Synthetic egress policy allows the submitted fields.'),
    CREDENTIAL_RESOLVABILITY: control('PASS', 'Synthetic credential reference resolved.'),
    CONNECTOR_SAFETY: control('PASS', 'Synthetic destination connector is available.'),
  }
  const merged = { ...base, ...overrides }
  const mapped: ControlInputMap = {}
  for (const kind of Object.keys(CONTROL_KEYS) as ControlKind[]) {
    mapped[CONTROL_KEYS[kind]] = merged[kind]
  }
  return mapped
}

export const EVALUATION_SCENARIOS: readonly EvaluationScenario[] = [
  {
    id: 'valid-crossing',
    label: 'Valid crossing',
    summary: 'All required controls pass. The Java engine should return ALLOW.',
    expectedOutcome: 'ALLOW',
    sourceSystem: 'HELIX_365',
    destinationSystem: 'COVECRM',
    integrationId: 'helix-calendar-crm',
    controls: allPass(),
  },
  {
    id: 'invalid-identity',
    label: 'Invalid identity',
    summary: 'Hard identity failure. The Java engine should return REJECT.',
    expectedOutcome: 'REJECT',
    sourceSystem: 'HELIX_365',
    destinationSystem: 'COVECRM',
    integrationId: 'helix-calendar-crm',
    controls: allPass({
      IDENTITY: control('FAIL', 'Synthetic identity is invalid for this crossing.', [
        'IDENTITY_INVALID',
      ]),
    }),
  },
  {
    id: 'destination-unavailable',
    label: 'Destination unavailable',
    summary: 'Recoverable connector unavailability. The Java engine should return QUARANTINE.',
    expectedOutcome: 'QUARANTINE',
    sourceSystem: 'QUAYFORGE',
    destinationSystem: 'RIVERTIDE',
    integrationId: 'quayforge-rivertide',
    controls: allPass({
      CONNECTOR_SAFETY: control('UNAVAILABLE', 'Synthetic destination connector is unavailable.', [
        'CONNECTOR_UNAVAILABLE',
      ]),
    }),
  },
  {
    id: 'transformation-required',
    label: 'Sensitive data transformation required',
    summary:
      'Egress requires deterministic redaction. The Java engine should return ALLOW_WITH_TRANSFORMATION.',
    expectedOutcome: 'ALLOW_WITH_TRANSFORMATION',
    sourceSystem: 'HELIX_365',
    destinationSystem: 'COVECRM',
    integrationId: 'helix-calendar-crm',
    controls: allPass({
      EGRESS: control('PASS', 'Two fields denied for this destination require deterministic redaction.', [
        'EGRESS_TRANSFORMATION_REQUIRED',
      ]),
    }),
  },
]

export function scenarioById(id: ScenarioId): EvaluationScenario {
  const found = EVALUATION_SCENARIOS.find((scenario) => scenario.id === id)
  if (!found) {
    throw new Error(`Unknown evaluation scenario: ${id}`)
  }
  return found
}

export function buildEvaluateRequest(
  scenario: EvaluationScenario,
  transactionId: string,
): EvaluateRequest {
  return {
    synthetic: true,
    transaction_id: transactionId,
    integration_id: scenario.integrationId,
    source_system: scenario.sourceSystem,
    destination_system: scenario.destinationSystem,
    controls: scenario.controls,
  }
}
