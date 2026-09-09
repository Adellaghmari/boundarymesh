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
