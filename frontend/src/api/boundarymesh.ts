import { apiClient, type ApiResponse } from '@/api/client'
import { assertEvaluateResponse } from '@/domain/mappers'
import type {
  EvaluateRequest,
  EvaluateResponse,
  HealthResponse,
  ReadyResponse,
} from '@/domain/types'
import { BoundaryMeshApiError } from '@/api/errors'

function assertHealth(value: unknown): HealthResponse {
  if (!value || typeof value !== 'object') {
    throw new BoundaryMeshApiError('malformed', 'Health response was not valid JSON.')
  }
  const body = value as Record<string, unknown>
  if (typeof body.status !== 'string' || typeof body.service !== 'string') {
    throw new BoundaryMeshApiError('malformed', 'Health response was missing required fields.')
  }
  return {
    status: body.status,
    service: body.service,
    truth: typeof body.truth === 'string' ? body.truth : 'LOCAL',
  }
}

function assertReady(value: unknown): ReadyResponse {
  if (!value || typeof value !== 'object') {
    throw new BoundaryMeshApiError('malformed', 'Readiness response was not valid JSON.')
  }
  const body = value as Record<string, unknown>
  if (typeof body.status !== 'string') {
    throw new BoundaryMeshApiError('malformed', 'Readiness response was missing required fields.')
  }
  return {
    status: body.status,
    detail: typeof body.detail === 'string' ? body.detail : '',
  }
}

export async function fetchHealth(): Promise<ApiResponse<HealthResponse>> {
  const response = await apiClient.request<unknown>('/api/v1/health')
  return { ...response, data: assertHealth(response.data) }
}

export async function fetchReady(): Promise<ApiResponse<ReadyResponse>> {
  const response = await apiClient.request<unknown>('/api/v1/ready')
  return { ...response, data: assertReady(response.data) }
}

export async function evaluateBoundary(
  request: EvaluateRequest,
): Promise<ApiResponse<EvaluateResponse>> {
  const response = await apiClient.request<unknown>('/api/v1/dev/evaluate', {
    method: 'POST',
    body: request,
  })
  try {
    return { ...response, data: assertEvaluateResponse(response.data) }
  } catch {
    throw new BoundaryMeshApiError(
      'malformed',
      'The evaluate response did not match the expected contract.',
      {
        requestId: response.requestId,
        status: response.status,
      },
    )
  }
}
