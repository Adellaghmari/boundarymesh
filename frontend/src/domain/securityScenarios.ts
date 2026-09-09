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
