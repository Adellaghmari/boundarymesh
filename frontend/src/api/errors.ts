export type ApiErrorKind =
  | 'unavailable'
  | 'timeout'
  | 'bad-request'
  | 'unauthorized'
  | 'forbidden'
  | 'server'
  | 'malformed'

export class BoundaryMeshApiError extends Error {
  readonly kind: ApiErrorKind
  readonly status?: number
  readonly requestId?: string
  readonly errorCode?: string

  constructor(
    kind: ApiErrorKind,
    message: string,
    options: { status?: number; requestId?: string; errorCode?: string } = {},
  ) {
    super(message)
    this.name = 'BoundaryMeshApiError'
    this.kind = kind
    this.status = options.status
    this.requestId = options.requestId
    this.errorCode = options.errorCode
  }
}

export function userMessageFor(error: unknown): string {
  if (error instanceof BoundaryMeshApiError) {
    return error.message
  }
  return 'The local backend could not be reached.'
}

export function isRetryable(error: unknown): boolean {
  if (!(error instanceof BoundaryMeshApiError)) {
    return true
  }
  return error.kind === 'unavailable' || error.kind === 'timeout' || error.kind === 'server'
}
