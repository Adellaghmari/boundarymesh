import { apiClient, type ApiResponse } from '@/api/client'
import {
  assertCredentialCatalog,
  assertDeliveryLabResponse,
  assertEvaluateResponse,
  assertQuarantineItem,
  assertQuarantineList,
} from '@/domain/mappers'
import type {
  CredentialCatalog,
  DeliveryLabRecord,
  EvaluateRequest,
  EvaluateResponse,
  HealthResponse,
  QuarantineItemRecord,
  QuarantineListRecord,
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

export async function issueLocalToken(scenario: string): Promise<string> {
  const response = await apiClient.request<{ access_token?: string }>('/api/v1/dev/tokens', {
    method: 'POST',
    body: { scenario },
  })
  if (typeof response.data.access_token !== 'string' || response.data.access_token.length === 0) {
    throw new BoundaryMeshApiError('malformed', 'Local token helper did not return a token.')
  }
  return response.data.access_token
}

export async function evaluateBoundary(
  request: EvaluateRequest,
): Promise<ApiResponse<EvaluateResponse>> {
  const token = await issueLocalToken('VALID_MACHINE')
  const response = await apiClient.request<unknown>('/api/v1/dev/evaluate', {
    method: 'POST',
    body: request,
    authorization: `Bearer ${token}`,
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

export async function evaluateIdentity(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  const token = await issueLocalToken(scenario)
  const response = await apiClient.request<unknown>('/api/v1/identity/evaluate', {
    method: 'POST',
    authorization: `Bearer ${token}`,
  })
  try {
    return { ...response, data: assertEvaluateResponse(response.data) }
  } catch {
    throw new BoundaryMeshApiError('malformed', 'The identity evaluate response was not valid.', {
      requestId: response.requestId,
      status: response.status,
    })
  }
}

export async function runContractLab(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  const response = await apiClient.request<unknown>(`/api/v1/dev/contract-lab/${scenario}`, {
    method: 'POST',
  })
  return asDecision(response, 'The contract lab response was not valid.')
}

export async function runEgressLab(
  destination: string,
  scenario: string,
): Promise<ApiResponse<EvaluateResponse>> {
  const response = await apiClient.request<unknown>(`/api/v1/dev/egress-lab/${destination}/${scenario}`, {
    method: 'POST',
  })
  return asDecision(response, 'The data boundary lab response was not valid.')
}

export async function evaluateCrossing(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  const token = await issueLocalToken('VALID_MACHINE')
  const response = await apiClient.request<unknown>('/api/v1/dev/crossings/evaluate', {
    method: 'POST',
    body: { scenario },
    authorization: `Bearer ${token}`,
  })
  return asDecision(response, 'The composed crossing response was not valid.')
}

function asDecision(
  response: ApiResponse<unknown>,
  message: string,
): ApiResponse<EvaluateResponse> {
  try {
    return { ...response, data: assertEvaluateResponse(response.data) }
  } catch {
    throw new BoundaryMeshApiError('malformed', message, {
      requestId: response.requestId,
      status: response.status,
    })
  }
}

export async function runDeliveryLab(scenario: string): Promise<DeliveryLabRecord> {
  const token = await issueLocalToken('VALID_MACHINE')
  const response = await apiClient.request<unknown>(`/api/v1/dev/delivery-lab/${scenario}`, {
    method: 'POST',
    body: {},
    authorization: `Bearer ${token}`,
  })
  try {
    return assertDeliveryLabResponse(response.data, response.requestId)
  } catch {
    throw new BoundaryMeshApiError('malformed', 'The delivery lab response was not valid.', {
      requestId: response.requestId,
      status: response.status,
    })
  }
}

export async function fetchCredentialCatalog(): Promise<CredentialCatalog> {
  const response = await apiClient.request<unknown>('/api/v1/dev/credential-lab')
  try {
    return assertCredentialCatalog(response.data)
  } catch {
    throw new BoundaryMeshApiError('malformed', 'The credential catalog was not valid.', {
      requestId: response.requestId,
      status: response.status,
    })
  }
}

export async function runCredentialLab(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  const response = await apiClient.request<unknown>(`/api/v1/dev/credential-lab/${scenario}`, {
    method: 'POST',
  })
  return asDecision(response, 'The credential lab response was not valid.')
}

export async function rotateCoveCredential(): Promise<CredentialCatalog> {
  const response = await apiClient.request<unknown>('/api/v1/dev/credential-lab/rotate/covecrm', {
    method: 'POST',
  })
  try {
    return assertCredentialCatalog(response.data)
  } catch {
    throw new BoundaryMeshApiError('malformed', 'The rotation response was not valid.', {
      requestId: response.requestId,
      status: response.status,
    })
  }
}

export async function fetchQuarantineList(): Promise<QuarantineListRecord> {
  const response = await apiClient.request<unknown>('/api/v1/quarantine')
  try {
    return assertQuarantineList(response.data)
  } catch {
    throw new BoundaryMeshApiError('malformed', 'The quarantine list was not valid.', {
      requestId: response.requestId,
      status: response.status,
    })
  }
}

export async function fetchQuarantineItem(id: string): Promise<QuarantineItemRecord> {
  const response = await apiClient.request<unknown>(`/api/v1/quarantine/${id}`)
  try {
    return assertQuarantineItem(response.data)
  } catch {
    throw new BoundaryMeshApiError('malformed', 'The quarantine item was not valid.', {
      requestId: response.requestId,
      status: response.status,
    })
  }
}

export async function reprocessQuarantine(id: string): Promise<DeliveryLabRecord> {
  const token = await issueLocalToken('VALID_ADMIN')
  const response = await apiClient.request<unknown>(`/api/v1/quarantine/${id}/reprocess`, {
    method: 'POST',
    authorization: `Bearer ${token}`,
  })
  try {
    return assertDeliveryLabResponse(response.data, response.requestId)
  } catch {
    throw new BoundaryMeshApiError('malformed', 'The reprocess response was not valid.', {
      requestId: response.requestId,
      status: response.status,
    })
  }
}

export async function runWebhookLab(scenario: string): Promise<ApiResponse<EvaluateResponse>> {
  const response = await apiClient.request<unknown>(`/api/v1/dev/webhook-lab/${scenario}`, {
    method: 'POST',
  })
  try {
    return { ...response, data: assertEvaluateResponse(response.data) }
  } catch {
    throw new BoundaryMeshApiError('malformed', 'The webhook lab response was not valid.', {
      requestId: response.requestId,
      status: response.status,
    })
  }
}
