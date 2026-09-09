import { createPinia, setActivePinia } from 'pinia'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { useEventsStore } from '@/stores/events'
import { allowResponse } from './fixtures'
import { server } from './mswServer'

describe('events store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('records a backend ALLOW decision in session history', async () => {
    const store = useEventsStore()
    const event = await store.evaluateScenario('valid-crossing')
    expect(event?.outcome).toBe('ALLOW')
    expect(store.events).toHaveLength(1)
    expect(store.eventById(event!.transactionId)?.scenarioLabel).toBe('Valid crossing')
  })

  it('keeps the backend outcome even when the scenario expected something else', async () => {
    server.use(
      http.post('/api/v1/dev/evaluate', () =>
        HttpResponse.json({
          ...allowResponse,
          transaction_id: 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
          outcome: 'REJECT',
          reason_codes: [{ code: 'IDENTITY_INVALID', description: 'Invalid identity.' }],
        }),
      ),
    )
    const store = useEventsStore()
    const event = await store.evaluateScenario('invalid-identity')
    expect(event?.outcome).toBe('REJECT')
    expect(event?.reasonCodes[0]?.code).toBe('IDENTITY_INVALID')
  })

  it('surfaces API failures without inventing an outcome', async () => {
    server.use(
      http.post('/api/v1/dev/evaluate', () =>
        HttpResponse.json({ message: 'down' }, { status: 500 }),
      ),
    )
    const store = useEventsStore()
    const event = await store.evaluateScenario('valid-crossing')
    expect(event).toBeNull()
    expect(store.events).toHaveLength(0)
    expect(store.errorMessage).toContain('unexpected error')
    expect(store.retryable).toBe(true)
  })
})
