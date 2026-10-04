import { useEventsStore } from '@/stores/events'

/** Fills the shared console with the four boundary outcomes before first paint. */
export async function seedPublicDemo(): Promise<void> {
  const events = useEventsStore()
  await events.evaluateScenario('invalid-identity')
  await events.evaluateScenario('destination-unavailable')
  await events.evaluateScenario('transformation-required')
  await events.evaluateScenario('valid-crossing')
}
