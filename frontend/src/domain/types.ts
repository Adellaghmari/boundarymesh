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

export interface ViolationView {
  path: string
  keyword: string
  expected: string
  actual: string
}

export interface ContractView {
  id?: string | null
  version?: string | null
  type?: string | null
  status?: string | null
  classification?: string | null
  truncatedCount: number
  violations: ViolationView[]
}

export interface FieldActionView {
  path: string
  action: string
  ruleId: string
  explanation: string
}

export interface EgressView {
  policyId: string
  policyVersion: string
  destination: string
  fields: FieldActionView[]
  redactedPaths: string[]
  deniedPaths: string[]
  implicitDenyPaths: string[]
  transformationCount: number
}

export interface TraceStepView {
  kind: string
  status: ControlStatus
  explanation: string
  reference: string
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
  contract?: ContractView
  egress?: EgressView
  enforcement_trace?: TraceStepView[]
  classification?: string
  source_payload?: string
  transformed_payload?: string
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
  contract?: ContractView
  egress?: EgressView
  enforcementTrace?: TraceStepView[]
  classification?: string
  sourcePayload?: string
  transformedPayload?: string
}

export const CAPABILITY_STATUSES = ['PLANNED', 'IMPLEMENTED', 'TESTED', 'LIVE VERIFIED'] as const
export type CapabilityStatus = (typeof CAPABILITY_STATUSES)[number]

export interface CapabilityClaim {
  id: string
  label: string
  status: CapabilityStatus
  notes: string
}

export const DELIVERY_RESULT_KINDS = [
  'DELIVERED',
  'DUPLICATE_SUPPRESSED',
  'QUARANTINED',
  'PERMANENT_FAILURE',
  'NOT_ATTEMPTED',
] as const

export type DeliveryResultKind = (typeof DELIVERY_RESULT_KINDS)[number]

export interface DeliveryInstructionView {
  eventId: string
  transactionId: string
  destination: string
  outcome: string
  payloadJson: string
  contractReference: string
}

export interface DeliveryView {
  kind: DeliveryResultKind
  attemptCount: number
  providerCategory: string
  reason?: string | null
  explanation: string
  connectorId?: string | null
  routeId?: string | null
  circuitState?: string | null
  httpStatus?: number | null
  providerCalled: boolean
}

export interface CredentialEvidenceView {
  reference?: string | null
  status: string
  explanation: string
}

export interface ConsentEvidenceView {
  id?: string | null
  status: string
  permissions: string[]
  explanation: string
}

export interface QuarantineSummaryView {
  id: string
  reason: string
  status: string
  attempts: number
  eligible: boolean
  destination: string
}

export interface DeliveryLabRecord {
  banner: string
  stages: string[]
  decision: EvaluateResponse
  instruction?: DeliveryInstructionView | null
  delivery?: DeliveryView | null
  credential: CredentialEvidenceView
  consent: ConsentEvidenceView
  quarantine?: QuarantineSummaryView | null
  requestId: string
}

export interface CredentialCatalogRow {
  id: string
  version: string
  status: string
  destination: string
  connector: string
  rotatedAt: string
}

export interface ConsentCatalogRow {
  id: string
  destination: string
  operation: string
  status: string
  kind: string
  permissions: string[]
}

export interface CredentialCatalog {
  banner: string
  credentials: CredentialCatalogRow[]
  consents: ConsentCatalogRow[]
}

export interface QuarantineItemRecord {
  id: string
  transactionId: string
  destination: string
  reason: string
  status: string
  attempts: number
  maxAttempts: number
  eligible: boolean
  credentialReference?: string | null
  connectorState?: string | null
  createdAt: string
}

export interface QuarantineListRecord {
  banner: string
  count: number
  items: QuarantineItemRecord[]
}
