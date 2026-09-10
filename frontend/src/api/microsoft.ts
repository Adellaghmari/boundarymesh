import { apiClient } from '@/api/client'

export interface MicrosoftSession {
  authenticated: boolean
  entraEnabled: boolean
  loginUrl: string
  logoutUrl: string
  roleLabel: string
  graphMode: string
  graphModeLabel: string
}

export interface MicrosoftConnector {
  mode: 'NOT_CONFIGURED' | 'CONTRACT_HARNESS' | 'LIVE_CONFIGURED' | 'LIVE_VERIFIED'
  permission: string
  permissionKind: string
  live: boolean
  banner: string
}

export function normalizeMicrosoftMode(mode: unknown): MicrosoftConnector['mode'] {
  if (mode === 'LIVE_VERIFIED' || mode === 'LIVE_CONFIGURED' || mode === 'NOT_CONFIGURED') {
    return mode
  }
  return 'CONTRACT_HARNESS'
}

export async function fetchMicrosoftSession(): Promise<MicrosoftSession> {
  const response = await apiClient.request<Record<string, unknown>>('/api/v1/admin/session')
  const body = response.data
  return {
    authenticated: body.authenticated === true,
    entraEnabled: body.entra_enabled === true,
    loginUrl: typeof body.login_url === 'string' ? body.login_url : '/oauth2/authorization/entra',
    logoutUrl: typeof body.logout_url === 'string' ? body.logout_url : '/logout',
    roleLabel: typeof body.role_label === 'string' ? body.role_label : 'Recruiter',
    graphMode: typeof body.graph_mode === 'string' ? body.graph_mode : 'CONTRACT_HARNESS',
    graphModeLabel:
      typeof body.graph_mode_label === 'string' ? body.graph_mode_label : 'Microsoft Graph contract harness',
  }
}

export async function fetchMicrosoftConnector(): Promise<MicrosoftConnector> {
  const response = await apiClient.request<Record<string, unknown>>('/api/v1/microsoft/connector')
  const body = response.data
  const mode = normalizeMicrosoftMode(body.mode)
  return {
    mode,
    permission: typeof body.permission === 'string' ? body.permission : 'Calendars.Read',
    permissionKind: typeof body.permission_kind === 'string' ? body.permission_kind : 'delegated',
    live: body.live === true,
    banner: typeof body.banner === 'string' ? body.banner : 'CONTRACT HARNESS',
  }
}

export async function runHarnessCalendarCrossing(): Promise<unknown> {
  const response = await apiClient.request<unknown>('/api/v1/dev/microsoft-lab/harness-crossing', {
    method: 'POST',
    body: {},
  })
  return response.data
}
