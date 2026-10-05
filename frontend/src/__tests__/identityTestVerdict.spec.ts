import { describe, expect, it } from 'vitest'
import { classifyIdentityTest } from '@/domain/identityTestVerdict'

describe('identity test verdict', () => {
  it.each(['EXPIRED', 'WRONG_ISSUER', 'WRONG_AUDIENCE'])(
    'passes %s when the observed response is HTTP 401',
    (scenarioId) => {
      const verdict = classifyIdentityTest(scenarioId, { type: 'http', status: 401 })
      expect(verdict.result).toBe('PASS')
      expect(verdict.observedResult).toBe('HTTP 401 Unauthorized')
      expect(verdict.httpStatus).toBe(401)
    },
  )

  it('explains a wrong audience rejection as audience validation', () => {
    const verdict = classifyIdentityTest('WRONG_AUDIENCE', { type: 'http', status: 401 })
    expect(verdict.reason).toBe('The token audience does not match BoundaryMesh.')
    expect(verdict.boundaryAction).toBe(
      'The crossing was blocked before authorization and policy evaluation.',
    )
  })

  it('fails when an expected 401 scenario is unexpectedly accepted', () => {
    const verdict = classifyIdentityTest('WRONG_AUDIENCE', { type: 'decision', status: 200, outcome: 'ALLOW' })
    expect(verdict.result).toBe('FAIL')
    expect(verdict.reason).toBe('The invalid identity was unexpectedly accepted.')
    expect(verdict.boundaryAction).toBe('The observed response does not match the security contract.')
  })

  it('fails when the identity service returns an unexpected server error', () => {
    const verdict = classifyIdentityTest('EXPIRED', { type: 'http', status: 500 })
    expect(verdict.result).toBe('FAIL')
    expect(verdict.observedResult).toBe('HTTP 500 Server Error')
    expect(verdict.reason).toBe('An unexpected system or server error occurred.')
  })

  it('passes a missing permission when the crossing is rejected', () => {
    const verdict = classifyIdentityTest('MISSING_PERMISSION', { type: 'decision', status: 200, outcome: 'REJECT' })
    expect(verdict.result).toBe('PASS')
    expect(verdict.observedResult).toBe('HTTP 200 REJECT')
  })

  it('fails when a valid machine identity is rejected', () => {
    const verdict = classifyIdentityTest('VALID_MACHINE', { type: 'decision', status: 200, outcome: 'REJECT' })
    expect(verdict.result).toBe('FAIL')
  })
})
