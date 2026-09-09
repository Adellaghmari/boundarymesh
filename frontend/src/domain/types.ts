export const BOUNDARY_OUTCOMES = [
  'ALLOW',
  'ALLOW_WITH_TRANSFORMATION',
  'QUARANTINE',
  'REJECT',
] as const

export type BoundaryOutcome = (typeof BOUNDARY_OUTCOMES)[number]

export const CONTROL_STATUSES = ['PASS', 'FAIL', 'NOT_APPLICABLE', 'UNAVAILABLE'] as const

export type ControlStatus = (typeof CONTROL_STATUSES)[number]

export const CONTROL_KINDS = [
  'IDENTITY',
  'AUTHORIZATION',
  'AUTHENTICITY',
  'FRESHNESS',
  'REPLAY',
  'CONTRACT',
  'EGRESS',
  'CREDENTIAL_RESOLVABILITY',
  'CONNECTOR_SAFETY',
] as const

export type ControlKind = (typeof CONTROL_KINDS)[number]

export const SOURCE_SYSTEMS = ['HELIX_365', 'QUAYFORGE', 'SYNTHETIC'] as const
export type SourceSystem = (typeof SOURCE_SYSTEMS)[number]

export const DESTINATION_SYSTEMS = ['COVECRM', 'RIVERTIDE', 'LUMENWAY', 'NONE'] as const
export type DestinationSystem = (typeof DESTINATION_SYSTEMS)[number]

export interface ReasonCode {
  code: string
  description: string
}

export interface ControlResult {
  kind: ControlKind
  status: ControlStatus
  reasonCodes: string[]
  safeExplanation: string
}

export interface ControlInput {
  status: ControlStatus
  reason_codes: string[]
  safe_explanation: string
}

export type ControlInputMap = Record<string, ControlInput>

export interface EvaluateRequest {
  synthetic: true
  transaction_id: string
  integration_id: string
  source_system: SourceSystem
  destination_system: DestinationSystem
  controls: ControlInputMap
}

export type IngressKind = 'TOKEN_PROTECTED_API' | 'SIGNED_WEBHOOK' | 'SYNTHETIC_EVALUATE'

export interface IdentityView {
  status: ControlStatus
  principal?: string | null
  issuer?: string | null
  method?: string | null
  keyId?: string | null
  explanation?: string | null
}

export interface AuthorizationView {
  status: ControlStatus
  grantedScopes: string[]
  explanation?: string | null
}

export interface ControlView {
  kind: ControlKind
  status: ControlStatus
  reasonCodes: string[]
  safeExplanation: string
}

export interface EvaluateResponse {
  banner: string
  transaction_id: string
  outcome: BoundaryOutcome
  ingress_kind?: IngressKind
  reason_codes: ReasonCode[]
  applied_controls: Partial<Record<ControlKind, ControlStatus>>
  controls?: ControlView[]
  decided_at: string
  policy_version: string
  duplicate_delivery?: boolean
  identity?: IdentityView
  authorization?: AuthorizationView
}

export interface HealthResponse {
  status: string
  service: string
  truth: string
}

export interface ReadyResponse {
  status: string
  detail: string
}

export interface ApiErrorBody {
  timestamp?: string
  request_id?: string
  error_code?: string
  message?: string
}

export interface BoundaryEventRecord {
  transactionId: string
  scenarioId: string
  scenarioLabel: string
  sourceSystem: SourceSystem
  destinationSystem: DestinationSystem
  integrationId: string
  outcome: BoundaryOutcome
  decidedAt: string
  policyVersion: string
  reasonCodes: ReasonCode[]
  appliedControls: Partial<Record<ControlKind, ControlStatus>>
  controlResults: ControlResult[]
  banner: string
  requestId: string
  ingressKind?: IngressKind
  duplicateDelivery?: boolean
  identity?: IdentityView
  authorization?: AuthorizationView
  httpStatus?: number
}

export const CAPABILITY_STATUSES = ['PLANNED', 'IMPLEMENTED', 'TESTED', 'LIVE VERIFIED'] as const
export type CapabilityStatus = (typeof CAPABILITY_STATUSES)[number]

export interface CapabilityClaim {
  id: string
  label: string
  status: CapabilityStatus
  notes: string
}
