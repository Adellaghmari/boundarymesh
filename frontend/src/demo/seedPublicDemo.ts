import { syntheticHistoryInstant } from '@/domain/timestamp'
import { useEventsStore } from '@/stores/events'

/** Oldest first. Minutes are measured from the moment this session is seeded. */
const SEEDED_HISTORY = [
  ['invalid-identity', 41],
  ['destination-unavailable', 19],
  ['transformation-required', 8],
  ['valid-crossing', 2],
] as const

/** Fills the shared console with the four boundary outcomes before first paint. */
export async function seedPublicDemo(origin = new Date()): Promise<void> {
  const events = useEventsStore()
  for (const [scenarioId, minutesAgo] of SEEDED_HISTORY) {
    const record = await events.evaluateScenario(scenarioId)
    if (!record) {
      continue
    }
    events.stampDecidedAt(record.transactionId, syntheticHistoryInstant(minutesAgo, origin))
  }
}
