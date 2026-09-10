import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { allowResponse, attestationItem, auditChain, boundaryGraph, credentialCatalog, deliverySuccess, quarantineItem } from './fixtures'

export const handlers = [
  http.get('/api/v1/health', () =>
    HttpResponse.json({
      status: 'UP',
      service: 'boundarymesh-api',
      truth: 'SYNTHETIC DEMO / LOCAL',
    }),
  ),
  http.get('/api/v1/ready', () =>
    HttpResponse.json({
      status: 'READY',
      detail: 'Phase 1 local foundation has no downstream dependencies.',
    }),
  ),
  http.post('/api/v1/dev/evaluate', () => HttpResponse.json(allowResponse)),
  http.post('/api/v1/dev/tokens', () => HttpResponse.json({ access_token: 'test.jwt.token' })),
  http.post('/api/v1/identity/evaluate', () =>
    HttpResponse.json({
      ...allowResponse,
      ingress_kind: 'TOKEN_PROTECTED_API',
      identity: {
        status: 'PASS',
        principal: 'app-quayforge-connector',
        issuer: 'http://127.0.0.1:8080/local-issuer',
        method: 'JWT_BEARER',
        keyId: 'local-1',
        explanation: 'Issuer accepted. Audience matches.',
      },
      authorization: {
        status: 'PASS',
        grantedScopes: ['boundary.evaluate'],
        explanation: 'Required permission boundary.evaluate granted.',
      },
    }),
  ),
  http.post('/api/v1/dev/contract-lab/:scenario', () =>
    HttpResponse.json({
      ...allowResponse,
      ingress_kind: 'SYNTHETIC_EVALUATE',
      classification: 'VALID',
      contract: {
        id: 'quayforge-orders',
        version: 'v1',
        type: 'JSON_SCHEMA',
        status: 'SUPPORTED',
        classification: 'VALID',
        truncated_count: 0,
        violations: [],
      },
      applied_controls: {
        ...allowResponse.applied_controls,
        CONTRACT: 'PASS',
      },
    }),
  ),
  http.post('/api/v1/dev/egress-lab/:destination/:scenario', () =>
    HttpResponse.json({
      ...allowResponse,
      outcome: 'ALLOW_WITH_TRANSFORMATION',
      ingress_kind: 'SYNTHETIC_EVALUATE',
      reason_codes: [
        { code: 'EGRESS_REDACTION_REQUIRED', description: 'Destination policy requires redaction.' },
      ],
      egress: {
        policy_id: 'rivertide-settlements',
        policy_version: 'v1',
        destination: 'RIVERTIDE',
        fields: [
          { path: '/customer_id', action: 'ALLOW', rule_id: 'EGRESS-RT-005', explanation: 'Copied.' },
          { path: '/email', action: 'REDACT', rule_id: 'EGRESS-RT-011', explanation: 'Redacted.' },
          {
            path: '/internal_risk_score',
            action: 'DENY',
            rule_id: 'EGRESS-RT-013',
            explanation: 'Critical deny.',
          },
        ],
        redacted_paths: ['/email'],
        denied_paths: ['/internal_risk_score'],
        implicit_deny_paths: [],
        transformation_count: 2,
      },
      source_payload: '{"customer_id":"cust-9","email":"ada.lovelace@example.test"}',
      transformed_payload: '{"customer_id":"cust-9","email":"[REDACTED]"}',
    }),
  ),
  http.post('/api/v1/dev/crossings/evaluate', () =>
    HttpResponse.json({
      ...allowResponse,
      outcome: 'ALLOW_WITH_TRANSFORMATION',
      ingress_kind: 'TOKEN_PROTECTED_API',
      classification: 'VALID',
      reason_codes: [
        { code: 'EGRESS_REDACTION_REQUIRED', description: 'Destination policy requires redaction.' },
      ],
      identity: {
        status: 'PASS',
        principal: 'velora-quayforge-connector',
        issuer: 'http://127.0.0.1:8080/local-issuer',
        method: 'JWT_BEARER',
        keyId: 'local-1',
        explanation: 'Issuer accepted. Audience matches.',
      },
      authorization: {
        status: 'PASS',
        grantedScopes: ['boundary.evaluate'],
        explanation: 'Required permission boundary.evaluate granted.',
      },
      enforcement_trace: [
        { kind: 'IDENTITY', status: 'PASS', explanation: 'Issuer accepted.', reference: '' },
        { kind: 'AUTHORIZATION', status: 'PASS', explanation: 'Scope granted.', reference: '' },
        { kind: 'AUTHENTICITY', status: 'PASS', explanation: 'Synthetic authenticity.', reference: '' },
        { kind: 'FRESHNESS', status: 'PASS', explanation: 'Synthetic freshness.', reference: '' },
        { kind: 'REPLAY', status: 'PASS', explanation: 'No replay.', reference: '' },
        { kind: 'CONTRACT', status: 'PASS', explanation: 'v2 VALID', reference: 'quayforge-orders:v1' },
        { kind: 'EGRESS', status: 'PASS', explanation: '2 fields redacted.', reference: '' },
      ],
      egress: {
        policy_id: 'rivertide-settlements',
        policy_version: 'v1',
        destination: 'RIVERTIDE',
        fields: [
          { path: '/email', action: 'REDACT', rule_id: 'EGRESS-RT-011', explanation: 'Redacted.' },
        ],
        redacted_paths: ['/email'],
        denied_paths: [],
        implicit_deny_paths: [],
        transformation_count: 2,
      },
      transformed_payload: '{"email":"[REDACTED]"}',
    }),
  ),
  http.post('/api/v1/dev/delivery-lab/:scenario', ({ params }) => {
    const scenario = String(params.scenario)
    if (scenario === 'credential-missing') {
      return HttpResponse.json({
        ...deliverySuccess,
        decision: {
          ...deliverySuccess.decision,
          outcome: 'QUARANTINE',
          applied_controls: {
            ...deliverySuccess.decision.applied_controls,
            CREDENTIAL_RESOLVABILITY: 'FAIL',
          },
        },
        instruction: null,
        delivery: {
          ...deliverySuccess.delivery,
          kind: 'NOT_ATTEMPTED',
          route_id: null,
          connector_id: null,
          provider_called: false,
          explanation: 'QUARANTINE before delivery never reaches Camel.',
        },
        credential: {
          reference: null,
          status: 'FAIL',
          explanation: 'Credential reference is missing.',
        },
        quarantine: {
          id: 'q-cred',
          reason: 'CREDENTIAL_UNAVAILABLE',
          status: 'OPEN',
          attempts: 0,
          eligible: true,
          destination: 'COVECRM',
        },
      })
    }
    if (scenario === 'consent-missing') {
      return HttpResponse.json({
        ...deliverySuccess,
        decision: { ...deliverySuccess.decision, outcome: 'QUARANTINE' },
        instruction: null,
        delivery: {
          ...deliverySuccess.delivery,
          kind: 'NOT_ATTEMPTED',
          route_id: null,
          connector_id: null,
          provider_called: false,
          explanation: 'QUARANTINE before delivery never reaches Camel.',
        },
        consent: {
          id: null,
          status: 'MISSING',
          permissions: [],
          explanation: 'Connector permission is not granted.',
        },
        quarantine: {
          id: 'q-consent',
          reason: 'CONSENT_INVALID',
          status: 'OPEN',
          attempts: 0,
          eligible: true,
          destination: 'RIVERTIDE',
        },
      })
    }
    if (scenario === 'circuit-open') {
      return HttpResponse.json({
        ...deliverySuccess,
        decision: {
          ...deliverySuccess.decision,
          outcome: 'QUARANTINE',
          applied_controls: {
            ...deliverySuccess.decision.applied_controls,
            CONNECTOR_SAFETY: 'UNAVAILABLE',
          },
        },
        instruction: null,
        delivery: {
          ...deliverySuccess.delivery,
          kind: 'NOT_ATTEMPTED',
          route_id: null,
          connector_id: null,
          circuit_state: 'OPEN',
          provider_called: false,
        },
        quarantine: {
          id: 'q-circuit',
          reason: 'CONNECTOR_UNAVAILABLE',
          status: 'OPEN',
          attempts: 0,
          eligible: true,
          destination: 'COVECRM',
        },
      })
    }
    if (scenario === 'server-error') {
      return HttpResponse.json({
        ...deliverySuccess,
        delivery: {
          ...deliverySuccess.delivery,
          kind: 'QUARANTINED',
          attempt_count: 3,
          provider_category: 'TEMPORARY_FAILURE',
          http_status: 503,
          explanation: 'Bounded retries exhausted.',
        },
        quarantine: {
          id: 'q-503',
          reason: 'DELIVERY_RETRIES_EXHAUSTED',
          status: 'OPEN',
          attempts: 0,
          eligible: true,
          destination: 'COVECRM',
        },
      })
    }
    if (scenario === 'duplicate') {
      return HttpResponse.json({
        ...deliverySuccess,
        delivery: {
          ...deliverySuccess.delivery,
          kind: 'DUPLICATE_SUPPRESSED',
          provider_called: false,
          explanation: 'Idempotent consumer recognized a duplicate delivery instruction.',
        },
      })
    }
    return HttpResponse.json(deliverySuccess)
  }),
  http.get('/api/v1/dev/credential-lab', () => HttpResponse.json(credentialCatalog)),
  http.post('/api/v1/dev/credential-lab/rotate/covecrm', () =>
    HttpResponse.json({
      ...credentialCatalog,
      credentials: [
        ...credentialCatalog.credentials,
        {
          id: 'covecrm-api',
          version: '3',
          status: 'ACTIVE',
          destination: 'COVECRM',
          connector: 'covecrm',
          rotated_at: '2026-09-10T00:00:00Z',
        },
      ],
    }),
  ),
  http.post('/api/v1/dev/credential-lab/:scenario', ({ params }) => {
    const scenario = String(params.scenario)
    const outcome =
      scenario === 'transformed-delivery' || scenario === 'credential-rotated' ? 'ALLOW' : 'QUARANTINE'
    return HttpResponse.json({
      ...allowResponse,
      outcome,
      applied_controls: {
        ...allowResponse.applied_controls,
        CREDENTIAL_RESOLVABILITY: scenario.startsWith('credential') && scenario !== 'credential-rotated' ? 'FAIL' : 'PASS',
      },
    })
  }),
  http.get('/api/v1/quarantine', () =>
    HttpResponse.json({
      banner: 'LOCAL / SYNTHETIC SESSION DATA. Not Firestore. Does not survive restart.',
      count: 1,
      items: [quarantineItem],
    }),
  ),
  http.get('/api/v1/quarantine/:id', () => HttpResponse.json(quarantineItem)),
  http.post('/api/v1/quarantine/:id/reprocess', () =>
    HttpResponse.json({
      ...deliverySuccess,
      delivery: { ...deliverySuccess.delivery, kind: 'DELIVERED' },
      quarantine: {
        id: quarantineItem.id,
        reason: quarantineItem.reason,
        status: 'RESOLVED',
        attempts: 1,
        eligible: false,
        destination: 'COVECRM',
      },
    }),
  ),
  http.post('/api/v1/dev/webhook-lab/:scenario', () =>
    HttpResponse.json({
      ...allowResponse,
      ingress_kind: 'SIGNED_WEBHOOK',
      duplicate_delivery: false,
      applied_controls: {
        ...allowResponse.applied_controls,
        IDENTITY: 'NOT_APPLICABLE',
        AUTHORIZATION: 'NOT_APPLICABLE',
      },
      controls: [
        {
          kind: 'AUTHENTICITY',
          status: 'PASS',
          reasonCodes: [],
          safeExplanation: 'HMAC-SHA256 signature matched the raw body.',
        },
        {
          kind: 'FRESHNESS',
          status: 'PASS',
          reasonCodes: [],
          safeExplanation: 'Timestamp is inside the accepted window.',
        },
        {
          kind: 'REPLAY',
          status: 'PASS',
          reasonCodes: [],
          safeExplanation: 'No prior nonce within the protected replay window.',
        },
      ],
    }),
  ),
  http.get('/api/v1/audit/chains', () =>
    HttpResponse.json({
      banner: 'LOCAL / SYNTHETIC SESSION DATA. One hash chain per integration. Not Firestore.',
      chains: ['quayforge-orders'],
    }),
  ),
  http.get('/api/v1/audit/chains/:id/verify', () =>
    HttpResponse.json({
      banner: 'Verification is computed by Java SHA-256 over RFC 8785 canonical bytes.',
      chain_id: 'quayforge-orders',
      status: 'VALID',
      entries_checked: 2,
      break_sequence: null,
      explanation: 'Sequence, previous-hash linkage, and SHA-256 hashes match.',
    }),
  ),
  http.get('/api/v1/audit/chains/:id', () => HttpResponse.json(auditChain)),
  http.get('/api/v1/attestations', () =>
    HttpResponse.json({
      banner: 'LOCAL / SYNTHETIC SESSION DATA. Local JCA P-256 signatures. Cloud KMS remains PLANNED.',
      items: [attestationItem],
    }),
  ),
  http.get('/api/v1/attestations/:id/verify', () =>
    HttpResponse.json({
      id: 'att-test-1',
      status: 'VALID',
      explanation: 'ECDSA P-256 signature matches the canonical attestation bytes.',
    }),
  ),
  http.post('/api/v1/attestations/:id/verify', () =>
    HttpResponse.json({
      id: 'att-test-1',
      status: 'VALID',
      explanation: 'ECDSA P-256 signature matches the canonical attestation bytes.',
    }),
  ),
  http.get('/api/v1/attestations/:id', () => HttpResponse.json(attestationItem)),
  http.get('/api/v1/boundary-graph', () => HttpResponse.json(boundaryGraph)),
  http.get('/api/v1/dev/audit-lab/tamper-demo', () =>
    HttpResponse.json({
      banner: 'SYNTHETIC DEMO. Isolated copy only. The live audit repository was not mutated.',
      original: {
        banner: 'Verification is computed by Java SHA-256 over RFC 8785 canonical bytes.',
        chain_id: 'quayforge-orders',
        status: 'VALID',
        entries_checked: 2,
        break_sequence: null,
        explanation: 'ok',
      },
      tampered: {
        banner: 'Verification is computed by Java SHA-256 over RFC 8785 canonical bytes.',
        chain_id: 'quayforge-orders',
        status: 'HASH_MISMATCH',
        entries_checked: 0,
        break_sequence: 1,
        explanation: 'Recalculated SHA-256 does not match stored current_hash.',
      },
    }),
  ),
  http.get('/api/v1/dev/attestation-lab/tamper-demo', () =>
    HttpResponse.json({
      banner: 'SYNTHETIC DEMO. Isolated copy only. The live attestation repository was not mutated.',
      original: { id: 'att-test-1', status: 'VALID', explanation: 'ok' },
      tampered: { id: 'att-test-1', status: 'INVALID', explanation: 'changed destination' },
      attestation_id: 'att-test-1',
    }),
  ),
]

export const server = setupServer(...handlers)
