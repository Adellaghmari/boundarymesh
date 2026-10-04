import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MicrosoftConnector, MicrosoftSession } from '@/api/microsoft'

const PUBLIC_SESSION: MicrosoftSession = {
  authenticated: false,
  entraEnabled: false,
  loginUrl: '/operator',
  logoutUrl: '/operator',
  roleLabel: 'Visitor',
  graphMode: 'CONTRACT_HARNESS',
  graphModeLabel: 'Synthetic portfolio',
}

const PUBLIC_CONNECTOR: MicrosoftConnector = {
  mode: 'CONTRACT_HARNESS',
  permission: 'Calendars.Read',
  permissionKind: 'DELEGATED',
  live: false,
  banner: 'SYNTHETIC DEMO',
}

export const useMicrosoftStore = defineStore('microsoft', () => {
  const session = ref<MicrosoftSession | null>(null)
  const connector = ref<MicrosoftConnector | null>(null)
  const lastHarness = ref<unknown>(null)
  const errorMessage = ref('')
  const loading = ref(false)

  async function loadSession(): Promise<void> {
    if (import.meta.env.VITE_APP_MODE === 'cloud') {
      session.value = PUBLIC_SESSION
      return
    }
    try {
      const api = await import('@/api/microsoft')
      session.value = await api.fetchMicrosoftSession()
    } catch {
      session.value = {
        authenticated: false,
        entraEnabled: false,
        loginUrl: '/oauth2/authorization/entra',
        logoutUrl: '/logout',
        roleLabel: 'Recruiter',
        graphMode: 'CONTRACT_HARNESS',
        graphModeLabel: 'Microsoft Graph contract harness',
      }
    }
  }

  async function loadConnector(): Promise<void> {
    if (import.meta.env.VITE_APP_MODE === 'cloud') {
      connector.value = PUBLIC_CONNECTOR
      return
    }
    try {
      const api = await import('@/api/microsoft')
      connector.value = await api.fetchMicrosoftConnector()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Connector status unavailable.'
    }
  }

  async function runHarness(): Promise<void> {
    if (import.meta.env.VITE_APP_MODE === 'cloud') {
      lastHarness.value = { outcome: 'ALLOW', banner: 'SYNTHETIC DEMO' }
      return
    }
    loading.value = true
    try {
      const api = await import('@/api/microsoft')
      lastHarness.value = await api.runHarnessCalendarCrossing()
    } finally {
      loading.value = false
    }
  }

  return { session, connector, lastHarness, errorMessage, loading, loadSession, loadConnector, runHarness }
})
