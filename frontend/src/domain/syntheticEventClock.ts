import { formatTimestamp } from '@/domain/timestamp'

/**
 * Visible decision time from the stored instant.
 * Synthetic history is stamped once when the session is created.
 * This function does not move a stored timestamp.
 */
export function presentBoundaryEventTime(_scenarioId: string, decidedAt: string, now = new Date()): string {
  return formatTimestamp(decidedAt, now)?.primary ?? decidedAt
}
