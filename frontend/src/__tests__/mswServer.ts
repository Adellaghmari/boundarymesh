import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { allowResponse } from './fixtures'

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
]

export const server = setupServer(...handlers)
