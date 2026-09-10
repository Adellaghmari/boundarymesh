import type { CapabilityClaim } from '@/domain/types'

/**
 * Manual frontend mirror of CV_CLAIMS_MATRIX.md for Phase 13.
 * Not fetched from Git. Do not invent progress.
 */
export const CAPABILITY_CLAIMS: readonly CapabilityClaim[] = [
  {
    id: 'java-backend',
    label: 'Java backend',
    status: 'TESTED',
    notes: 'Java 25 / Spring Boot 4.1.1 local API. Maven verify.',
  },
  {
    id: 'enforcement-engine',
    label: 'Boundary Enforcement Engine',
    status: 'TESTED',
    notes: 'All nine ControlVector fields are real local controls, including credential and connector safety.',
  },
  {
    id: 'vue-frontend',
    label: 'Vue frontend',
    status: 'TESTED',
    notes: 'Vue 3 recruiter console against the local backend. No public deploy.',
  },
  {
    id: 'identity-federation',
    label: 'Identity Federation Gateway',
    status: 'TESTED',
    notes:
      'Local JWT/JWKS resource server and AuthN vs AuthZ evidence. Microsoft Entra remains PLANNED.',
  },
  {
    id: 'webhook-defense',
    label: 'Webhook & Subscription Defense',
    status: 'TESTED',
    notes:
      'QuayForge HMAC, freshness, replay, and idempotency. Microsoft Graph subscriptions remain PLANNED.',
  },
  {
    id: 'contract-firewall',
    label: 'Contract Drift Firewall',
    status: 'TESTED',
    notes: 'Versioned OpenAPI/AsyncAPI/JSON Schema artifacts. Runtime validation and drift quarantine.',
  },
  {
    id: 'data-firewall',
    label: 'Data Boundary Firewall',
    status: 'TESTED',
    notes: 'Destination-specific ALLOW/REDACT/DENY with real outbound transformation.',
  },
  {
    id: 'credential-vault',
    label: 'Credential & Consent Vault',
    status: 'TESTED',
    notes:
      'Local reference/version/consent model. Google Secret Manager-backed vault remains PLANNED.',
  },
  {
    id: 'quarantine-orchestrator',
    label: 'Quarantine & Recovery',
    status: 'TESTED',
    notes: 'Local in-memory orchestrator and admin reprocess. Firestore persistence remains PLANNED.',
  },
  {
    id: 'audit-chain',
    label: 'Tamper-Evident Audit Chain',
    status: 'TESTED',
    notes: 'SHA-256 hash chain with RFC 8785 canonicalization. Local in-memory. Not immutable. Not Firestore.',
  },
  {
    id: 'exchange-attestation',
    label: 'Cryptographic Exchange Attestation',
    status: 'TESTED',
    notes: 'Local JCA ECDSA P-256. Cloud KMS remains PLANNED. Does not prove business data is true.',
  },
  {
    id: 'boundary-graph',
    label: 'Integration Boundary Graph',
    status: 'TESTED',
    notes: 'Vue SVG from the Java graph API. Configured plus observed crossings. Not a blast-radius graph.',
  },
  {
    id: 'apache-camel',
    label: 'Apache Camel',
    status: 'TESTED',
    notes: 'Worker-only Camel 4.22.0 routes. The public API artifact does not depend on Camel.',
  },
  {
    id: 'm365-graph',
    label: 'M365 / Graph',
    status: 'PLANNED',
    notes: 'Helix 365 is fictional demo context. Graph is not integrated.',
  },
  {
    id: 'gcp',
    label: 'GCP',
    status: 'PLANNED',
    notes: 'No Cloud Run, Pub/Sub, Firestore, Secret Manager, or KMS. Local JCA signer only.',
  },
]
