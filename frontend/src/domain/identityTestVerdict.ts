export interface IdentityObservation {
  type: 'http' | 'decision'
  status: number
  outcome?: string
}

export interface IdentityVerdict {
  result: 'PASS' | 'FAIL'
  observedResult: string
  reason: string
  boundaryAction: string
  httpStatus?: number
}

interface IdentityExpectation {
  httpStatus: number
  outcome?: string
  reason: string
  boundaryAction: string
}

const EXPECTED: Readonly<Record<string, IdentityExpectation>> = {
  EXPIRED: {
    httpStatus: 401,
    reason: 'The identity token is no longer valid.',
    boundaryAction: 'The request was stopped before a crossing was created.',
  },
  WRONG_ISSUER: {
    httpStatus: 401,
    reason: 'The token issuer is not trusted by BoundaryMesh.',
    boundaryAction: 'The request was stopped at authentication.',
  },
  WRONG_AUDIENCE: {
    httpStatus: 401,
    reason: 'The token audience does not match BoundaryMesh.',
    boundaryAction: 'The crossing was blocked before authorization and policy evaluation.',
  },
  MISSING_PERMISSION: {
    httpStatus: 200,
    outcome: 'REJECT',
    reason: 'The identity is valid but does not have the required permission.',
    boundaryAction: 'The crossing was blocked at authorization.',
  },
  VALID_MACHINE: {
    httpStatus: 200,
    outcome: 'ALLOW',
    reason: 'The machine identity satisfies the authentication contract.',
    boundaryAction: 'The request continued to authorization and policy evaluation.',
  },
}

function observedResult(status: number, outcome?: string): string {
  if (status === 401) return 'HTTP 401 Unauthorized'
  if (status === 403) return 'HTTP 403 Forbidden'
  if (status >= 500) return `HTTP ${status} Server Error`
  if (status >= 200 && status < 300 && outcome) return `HTTP ${status} ${outcome}`
  return `HTTP ${status}`
}

function fail(
  observation: IdentityObservation,
  reason: string,
  boundaryAction: string,
): IdentityVerdict {
  return {
    result: 'FAIL',
    observedResult: observedResult(observation.status, observation.outcome),
    reason,
    boundaryAction,
    httpStatus: observation.status > 0 ? observation.status : undefined,
  }
}

/**
 * A negative identity scenario passes when the observed response is the expected rejection.
 * An unexpected acceptance or a server failure fails the test.
 */
export function classifyIdentityTest(scenarioId: string, observation: IdentityObservation): IdentityVerdict {
  const expected = EXPECTED[scenarioId]
  if (!expected) {
    return fail(
      observation,
      'No security contract is defined for this scenario.',
      'The security check could not be completed.',
    )
  }
  if (observation.status >= 500 || observation.status <= 0) {
    return fail(
      observation,
      'An unexpected system or server error occurred.',
      'BoundaryMesh could not confirm the expected security behavior.',
    )
  }
  if (expected.httpStatus === 401) {
    if (observation.status === 401) {
      return {
        result: 'PASS',
        observedResult: 'HTTP 401 Unauthorized',
        reason: expected.reason,
        boundaryAction: expected.boundaryAction,
        httpStatus: 401,
      }
    }
    if (observation.status >= 200 && observation.status < 300) {
      return fail(
        observation,
        'The invalid identity was unexpectedly accepted.',
        'The observed response does not match the security contract.',
      )
    }
    return fail(
      observation,
      'The observed response does not match the expected authentication rejection.',
      'BoundaryMesh could not confirm the expected security behavior.',
    )
  }
  if (observation.status !== expected.httpStatus || observation.outcome !== expected.outcome) {
    return fail(
      observation,
      'The observed response does not match the security contract.',
      'BoundaryMesh could not confirm the expected security behavior.',
    )
  }
  return {
    result: 'PASS',
    observedResult: observedResult(observation.status, observation.outcome),
    reason: expected.reason,
    boundaryAction: expected.boundaryAction,
    httpStatus: observation.status,
  }
}
