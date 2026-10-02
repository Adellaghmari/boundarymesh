export const IDENTITY_LAB_SCENARIOS = [
  {
    id: 'VALID_MACHINE',
    label: 'Valid machine identity',
    summary: 'Asymmetric JWT, allowed issuer/audience, boundary.evaluate granted.',
  },
  {
    id: 'MISSING_PERMISSION',
    label: 'Missing permission',
    summary: 'Token is authentic. Required evaluate permission is missing.',
  },
  {
    id: 'EXPIRED',
    label: 'Expired identity',
    summary: 'Authentication cannot be established. Expect HTTP 401.',
  },
  {
    id: 'WRONG_ISSUER',
    label: 'Wrong issuer',
    summary: 'Authentication cannot be established. Expect HTTP 401.',
  },
  {
    id: 'WRONG_AUDIENCE',
    label: 'Wrong audience',
    summary: 'Authentication cannot be established. Expect HTTP 401.',
  },
] as const

export const WEBHOOK_LAB_SCENARIOS = [
  { id: 'valid', label: 'Valid signed webhook', summary: 'HMAC, freshness, and new nonce.' },
  { id: 'invalid-signature', label: 'Invalid signature', summary: 'Authenticity fails.' },
  { id: 'expired-timestamp', label: 'Expired timestamp', summary: 'Freshness fails.' },
  { id: 'replay', label: 'Replay attempt', summary: 'Same nonce reused. Security reject.' },
  { id: 'duplicate', label: 'Legitimate duplicate', summary: 'New nonce, same event_id. Not a replay.' },
  { id: 'missing-signature', label: 'Missing signature', summary: 'Authenticity fails.' },
] as const

export const CONTRACT_LAB_SCENARIOS = [
  { id: 'valid-v1', label: 'Valid v1 contract', summary: 'Supported version. Payload matches the schema.' },
  { id: 'missing-required', label: 'Missing required field', summary: 'Supported v1. Schema violation. REJECT.' },
  { id: 'wrong-type', label: 'Wrong type', summary: 'amount is a string. Schema violation. REJECT.' },
  { id: 'unsupported-version', label: 'Unsupported v3', summary: 'Version is registered as unsupported. QUARANTINE.' },
  {
    id: 'contract-drift',
    label: 'Contract drift',
    summary: 'Declares v1 but the body matches supported v2. QUARANTINE.',
  },
  { id: 'too-many-violations', label: 'Violation truncation', summary: 'Many extra fields. First 20 violations only.' },
] as const

export const EGRESS_LAB_DESTINATIONS = [
  { id: 'COVECRM', label: 'CoveCRM', summary: 'Contact sync. Email may leave.' },
  { id: 'RIVERTIDE', label: 'Rivertide Settlements', summary: 'Settlement. Email redacted. Risk score denied.' },
  { id: 'LUMENWAY', label: 'Lumenway Fulfillment', summary: 'Fulfillment. Shipping allowed. Email redacted.' },
] as const

export const EGRESS_LAB_SCENARIOS = [
  { id: 'allow', label: 'All permitted fields', summary: 'Only explicitly allowed fields are present.' },
  { id: 'email-only', label: 'Email contrast', summary: 'Same email field. The action depends on the destination.' },
  { id: 'redact', label: 'Multiple redactions', summary: 'Email and name require transformation.' },
  { id: 'deny', label: 'Forbidden critical field', summary: 'internal_risk_score is present. REJECT.' },
  { id: 'nested', label: 'Nested field rule', summary: 'JSON Pointer /customer/email.' },
  { id: 'implicit-deny', label: 'Implicit deny', summary: 'Unlisted legacy_region is omitted.' },
  { id: 'minimization', label: 'Data minimization', summary: 'Destination receives only what policy permits.' },
] as const

export const CROSSING_LAB_SCENARIOS = [
  {
    id: 'composed-allow',
    label: 'All controls pass',
    summary: 'Valid identity, contract v1, no egress transformation. ALLOW.',
  },
  {
    id: 'composed-transform',
    label: 'Egress redaction',
    summary: 'Authenticated crossing. Two fields redacted. ALLOW_WITH_TRANSFORMATION.',
  },
  {
    id: 'composed-quarantine',
    label: 'Contract drift hold',
    summary: 'Same identity. Declared v1 matches v2. QUARANTINE.',
  },
  {
    id: 'composed-reject-egress',
    label: 'Forbidden outbound field',
    summary: 'The payload passes the contract. A critical egress rule denies it. REJECT.',
  },
] as const

export const DELIVERY_LAB_SCENARIOS = [
  {
    id: 'transformed-delivery',
    label: 'Successful transformed delivery',
    summary: 'Allow with redaction, credential v2, consent granted, Camel Lumenway route.',
  },
  {
    id: 'covecrm-delivery',
    label: 'QuayForge to CoveCRM',
    summary: 'Allowed delivery to CoveCRM. Email may leave that destination.',
  },
  {
    id: 'contract-drift',
    label: 'Contract drift hold',
    summary: 'Unsupported payload shape. QUARANTINE. No provider call.',
  },
  {
    id: 'replay-reject',
    label: 'Replay reject',
    summary: 'Replay control FAIL. REJECT. Delivery is not attempted.',
  },
  {
    id: 'credential-missing',
    label: 'Missing credential',
    summary: 'Consent exists. Credential reference is missing. No provider call.',
  },
  {
    id: 'consent-missing',
    label: 'Missing consent',
    summary: 'Credential exists. Connector permission is missing. No provider call.',
  },
  {
    id: 'circuit-open',
    label: 'Circuit open',
    summary: 'Connector safety UNAVAILABLE. Delivery is not attempted.',
  },
  {
    id: 'server-error',
    label: 'Provider outage',
    summary: 'Bounded 503 retries, then quarantine. Worker + WireMock.',
  },
  {
    id: 'duplicate',
    label: 'Duplicate delivery instruction',
    summary: 'Same instruction delivered twice. Second result is DUPLICATE_SUPPRESSED.',
  },
] as const

export const CREDENTIAL_LAB_SCENARIOS = [
  {
    id: 'transformed-delivery',
    label: 'Active credential + valid consent',
    summary: 'Reference resolves. Operation is permitted.',
  },
  {
    id: 'credential-missing',
    label: 'Missing credential',
    summary: 'No resolvable reference. QUARANTINE.',
  },
  {
    id: 'credential-revoked',
    label: 'Revoked credential version',
    summary: 'Retired version is not used. QUARANTINE.',
  },
  {
    id: 'credential-rotated',
    label: 'Rotated credential',
    summary: 'New deliveries use the active version reference only.',
  },
  {
    id: 'consent-missing',
    label: 'Valid credential + missing consent',
    summary: 'Secret availability is not permission.',
  },
  {
    id: 'consent-revoked',
    label: 'Consent revoked',
    summary: 'Previously granted connector permission is no longer valid.',
  },
] as const
