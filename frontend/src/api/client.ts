import { frontendEnv } from '@/config/env'
import { BoundaryMeshApiError } from '@/api/errors'

export interface RequestOptions {
  method?: 'GET' | 'POST'
  body?: unknown
  timeoutMs?: number
  signal?: AbortSignal
}

export interface ApiResponse<T> {
  data: T
  requestId: string
  status: number
}

function createRequestId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return `bm-${crypto.randomUUID()}`
  }
  return `bm-${Date.now()}`
}

export function resolveApiUrl(path: string, baseUrl = frontendEnv.apiBaseUrl): string {
  if (!path.startsWith('/')) {
    throw new BoundaryMeshApiError('malformed', 'API paths must start with /.')
  }
  if (baseUrl === '') {
    return path
  }
  const base = new URL(baseUrl)
  if (base.protocol !== 'http:' && base.protocol !== 'https:') {
    throw new BoundaryMeshApiError('malformed', 'API base URL must use http or https.')
  }
  return new URL(path, base).toString()
}

function sanitizeApiMessage(raw: unknown, fallback: string): string {
  if (typeof raw !== 'string' || raw.trim() === '') {
    return fallback
  }
  const text = raw.trim()
  if (
    text.includes('<html') ||
    text.includes('Whitelabel') ||
    text.includes('at org.springframework')
  ) {
    return fallback
  }
  if (text.length > 240) {
    return fallback
  }
  return text
}

function classifyHttpError(status: number, body: unknown, requestId: string): BoundaryMeshApiError {
  const record = body && typeof body === 'object' ? (body as Record<string, unknown>) : {}
  const errorCode = typeof record.error_code === 'string' ? record.error_code : undefined
  const backendRequestId = typeof record.request_id === 'string' ? record.request_id : requestId

  if (status === 400) {
    return new BoundaryMeshApiError(
      'bad-request',
      sanitizeApiMessage(record.message, 'The evaluation request was rejected by the backend.'),
      { status, requestId: backendRequestId, errorCode },
    )
  }
  if (status === 401) {
    return new BoundaryMeshApiError(
      'unauthorized',
      'The local API refused this request. Phase 2 has no operator login.',
      { status, requestId: backendRequestId, errorCode },
    )
  }
  if (status >= 500) {
    return new BoundaryMeshApiError('server', 'The local backend returned an unexpected error.', {
      status,
      requestId: backendRequestId,
      errorCode,
    })
  }
  return new BoundaryMeshApiError('server', 'The local backend returned an unexpected response.', {
    status,
    requestId: backendRequestId,
    errorCode,
  })
}

export class BoundaryMeshApiClient {
  constructor(
    private readonly baseUrl = frontendEnv.apiBaseUrl,
    private readonly timeoutMs = frontendEnv.apiTimeoutMs,
  ) {}

  async request<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
    const requestId = createRequestId()
    const controller = new AbortController()
    const timeout = window.setTimeout(() => controller.abort(), options.timeoutMs ?? this.timeoutMs)
    const onAbort = () => controller.abort()
    options.signal?.addEventListener('abort', onAbort)

    try {
      const headers: Record<string, string> = {
        Accept: 'application/json',
        'X-Request-Id': requestId,
      }
      if (options.body !== undefined) {
        headers['Content-Type'] = 'application/json'
      }

      const response = await fetch(resolveApiUrl(path, this.baseUrl), {
        method: options.method ?? 'GET',
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
        signal: controller.signal,
      })

      const rawText = await response.text()
      let parsed: unknown = undefined
      if (rawText.trim() !== '') {
        try {
          parsed = JSON.parse(rawText)
        } catch {
          throw new BoundaryMeshApiError(
            'malformed',
            'The backend returned a response that was not valid JSON.',
            {
              status: response.status,
              requestId,
            },
          )
        }
      }

      if (!response.ok) {
        throw classifyHttpError(response.status, parsed, requestId)
      }
      if (parsed === undefined) {
        throw new BoundaryMeshApiError('malformed', 'The backend returned an empty response.', {
          status: response.status,
          requestId,
        })
      }

      return {
        data: parsed as T,
        requestId,
        status: response.status,
      }
    } catch (error) {
      if (error instanceof BoundaryMeshApiError) {
        throw error
      }
      if (error instanceof Error && error.name === 'AbortError') {
        throw new BoundaryMeshApiError('timeout', 'The local backend did not respond in time.', {
          requestId,
        })
      }
      throw new BoundaryMeshApiError('unavailable', 'The local Java backend is not reachable.', {
        requestId,
      })
    } finally {
      window.clearTimeout(timeout)
      options.signal?.removeEventListener('abort', onAbort)
    }
  }
}

export const apiClient = new BoundaryMeshApiClient()
