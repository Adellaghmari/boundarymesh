import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { BoundaryMeshApiClient } from '@/api/client'
import { BoundaryMeshApiError } from '@/api/errors'
import { server } from './mswServer'

const client = new BoundaryMeshApiClient('', 200)

describe('API client', () => {
  it('returns typed JSON on success and sends a request id', async () => {
    let requestId: string | null = null
    server.use(
      http.get('/api/v1/health', ({ request }) => {
        requestId = request.headers.get('X-Request-Id')
        return HttpResponse.json({ status: 'UP', service: 'boundarymesh-api', truth: 'LOCAL' })
      }),
    )

    const result = await client.request('/api/v1/health')
    expect(result.status).toBe(200)
    expect(result.data).toMatchObject({ status: 'UP' })
    expect(requestId).toMatch(/^bm-/)
  })

  it('accepts a successful empty 204 response for session logout', async () => {
    server.use(http.post('/session/logout', () => new HttpResponse(null, { status: 204 })))
    const result = await client.request<void>('/session/logout', { method: 'POST' })
    expect(result.status).toBe(204)
    expect(result.data).toBeUndefined()
  })

  it('maps timeout, 400, 401, 5xx, and malformed bodies', async () => {
    const originalFetch = globalThis.fetch
    try {
      globalThis.fetch = () =>
        new Promise((_, reject) => {
          const abortError = new Error('Aborted')
          abortError.name = 'AbortError'
          reject(abortError)
        })
      await expect(client.request('/api/v1/timeout')).rejects.toMatchObject({ kind: 'timeout' })
    } finally {
      globalThis.fetch = originalFetch
    }

    server.use(
      http.get('/api/v1/bad', () =>
        HttpResponse.json(
          { error_code: 'SYNTHETIC_REQUIRED', message: 'Need synthetic.' },
          { status: 400 },
        ),
      ),
    )
    await expect(client.request('/api/v1/bad')).rejects.toBeInstanceOf(BoundaryMeshApiError)
    await expect(client.request('/api/v1/bad')).rejects.toMatchObject({ kind: 'bad-request' })

    server.use(
      http.get('/api/v1/denied', () =>
        HttpResponse.json({ error_code: 'UNAUTHORIZED', message: 'No' }, { status: 401 }),
      ),
    )
    await expect(client.request('/api/v1/denied')).rejects.toMatchObject({
      kind: 'unauthorized',
      message: 'Authentication could not be established.',
    })

    server.use(
      http.get('/api/v1/forbidden', () =>
        HttpResponse.json({ error_code: 'FORBIDDEN', message: 'No' }, { status: 403 }),
      ),
    )
    await expect(client.request('/api/v1/forbidden')).rejects.toMatchObject({
      kind: 'forbidden',
      message: 'The authenticated principal is not authorized for this boundary crossing.',
    })

    server.use(
      http.get('/api/v1/boom', () => HttpResponse.json({ message: 'hidden' }, { status: 500 })),
    )
    await expect(client.request('/api/v1/boom')).rejects.toMatchObject({
      kind: 'server',
      message: 'The BoundaryMesh service returned an unexpected error.',
    })

    server.use(
      http.get('/api/v1/html', () =>
        HttpResponse.text('<html>Whitelabel Error</html>', { status: 200 }),
      ),
    )
    await expect(client.request('/api/v1/html')).rejects.toMatchObject({ kind: 'malformed' })
  })
})
