import { describe, expect, it } from 'vitest'
import { buildEvaluateRequest, EVALUATION_SCENARIOS, scenarioById } from '@/domain/scenarios'

describe('evaluation scenarios', () => {
  it('covers the four recruiter outcomes without encoding the engine', () => {
    const ids = EVALUATION_SCENARIOS.map((scenario) => scenario.id)
    expect(ids).toEqual([
      'valid-crossing',
      'invalid-identity',
      'destination-unavailable',
      'transformation-required',
    ])
    expect(scenarioById('valid-crossing').expectedOutcome).toBe('ALLOW')
    expect(scenarioById('invalid-identity').controls.identity?.status).toBe('FAIL')
    expect(scenarioById('destination-unavailable').controls.connector_safety?.status).toBe(
      'UNAVAILABLE',
    )
    expect(scenarioById('transformation-required').controls.egress?.reason_codes).toContain(
      'EGRESS_TRANSFORMATION_REQUIRED',
    )
    const request = buildEvaluateRequest(
      scenarioById('valid-crossing'),
      '11111111-2222-3333-4444-555555555555',
    )
    expect(request.synthetic).toBe(true)
    expect(request.source_system).toBe('HELIX_365')
  })
})
