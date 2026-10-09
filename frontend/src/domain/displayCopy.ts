/**
 * Presentation labels for the website.
 * Technical identifiers stay unchanged in routes, requests, and storage.
 */
import { formatTimestamp, isTimestamp } from '@/domain/timestamp'
const PHRASES: readonly (readonly [string, string])[] = [
  ['m365-calendar-egress/v1', 'Calendar egress policy v1'],
  ['m365-calendar/v1', 'Calendar contract v1'],
  ['m365-calendar-egress', 'Calendar egress policy'],
  ['m365-calendar', 'Calendar contract'],
  ['lumenway-api:v1', 'Lumenway credential v1'],
  ['covecrm-api:v2', 'CoveCRM credential v2'],
  ['covecrm-api:2', 'CoveCRM credential v2'],
  ['lumenway-api:1', 'Lumenway credential v1'],
  ['quayforge-orders:v1', 'QuayForge orders v1'],
  ['local-jca-p256', 'Local signing key'],
  ['boundarymesh-worker', 'BoundaryMesh Worker'],
  ['boundarymesh-web', 'BoundaryMesh Web'],
  ['boundarymesh-api', 'BoundaryMesh API'],
  ['rivertide-settlements', 'Rivertide settlements'],
  ['helix-calendar-crm', 'Helix calendar to CoveCRM'],
  ['quayforge-orders', 'QuayForge orders'],
  ['covecrm-api', 'CoveCRM credential'],
  ['lumenway-api', 'Lumenway credential'],
  ['rivertide-api', 'Rivertide credential'],
  ['HMAC-SHA256', 'HMAC SHA256'],
  ['SHA-256', 'SHA256'],
  ['P-256', 'P256'],
  ['Firestore-backed', 'stored in Firestore'],
  ['destination-specific', 'specific to each destination'],
  ['Destination-specific', 'Specific to each destination'],
  ['destination-denied', 'denied for this destination'],
  ['contract-checked', 'checked against the contract'],
  ['contract-valid', 'valid for the contract'],
  ['Contract-valid', 'Valid for the contract'],
  ['system-to-system', 'system to system'],
  ['data-governed', 'governed before it can leave'],
  ['data-boundary', 'data boundary'],
  ['Data-boundary', 'Data boundary'],
  ['tamper-evident', 'tamper evident'],
  ['Tamper-evident', 'Tamper evident'],
  ['Tamper-Evident', 'Tamper evident'],
  ['Microsoft-365-like', 'similar to Microsoft 365'],
  ['personal-account', 'personal account'],
  ['High-sensitivity', 'High sensitivity'],
  ['previous-hash', 'previous hash'],
  ['browser-held', 'held in the browser'],
  ['Audit-chain', 'Audit chain'],
  ['audit-chain', 'audit chain'],
  ['pre-delivery', 'before delivery'],
  ['live-state', 'live state'],
  ['blast-radius', 'blast radius'],
  ['IAM-private', 'private'],
  ['Worker-only', 'Worker only'],
  ['KMS-signed', 'signed by Cloud KMS'],
  ['fail-closed', 'closed when a check fails'],
  ['end-to-end', 'from start to finish'],
  ['full-stack', 'full stack'],
  ['real-time', 'in real time'],
  ['zero-drift', 'zero drift'],
  ['read-only', 'view only'],
  ['sign-in', 'sign in'],
  ['Sign-in', 'Sign in'],
  ['Cross-system', 'Cross system'],
  ['cross-system', 'cross system'],
  ['One-minute', 'One minute'],
]

const ORDERED = PHRASES.slice().sort((left, right) => right[0].length - left[0].length)

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

function formatWhen(value: string): string | null {
  if (!isTimestamp(value)) {
    return null
  }
  return formatTimestamp(value, new Date())?.primary ?? null
}

function tidy(value: string): string {
  return value
    .replace(/\s+/g, ' ')
    .replace(/\s+([.,:;])/g, '$1')
    .replace(/([.!?]\s+)([a-z])/g, (_, lead: string, letter: string) => lead + letter.toUpperCase())
    .trim()
}

/** Human text for anything that can appear on screen. */
export function show(value: unknown): string {
  if (value == null || value === '') {
    return ''
  }
  const raw = String(value).trim()
  if (raw === '—' || raw === '–' || raw === '-') {
    return 'None'
  }
  if (UUID.test(raw)) {
    return 'Recorded crossing'
  }
  const when = formatWhen(raw)
  if (when) {
    return when
  }

  let text = raw
  for (const [from, to] of ORDERED) {
    text = text.split(from).join(to)
  }
  text = text.replace(/\s*—\s*/g, '. ').replace(/\s*–\s*/g, ', ').replace(/-/g, ' ')
  return tidy(text)
}
