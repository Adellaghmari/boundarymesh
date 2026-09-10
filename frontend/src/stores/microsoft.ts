import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  fetchMicrosoftConnector,
  fetchMicrosoftSession,
  runHarnessCalendarCrossing,
  type MicrosoftConnector,
  type MicrosoftSession,
} from '@/api/microsoft'

export const useMicrosoftStore = defineStore('microsoft', () => {
  const session = ref<MicrosoftSession | null>(null)
  const connector = ref<MicrosoftConnector | null>(null)
  const lastHarness = ref<unknown>(null)
  const errorMessage = ref('')
  const loading = ref(false)

  async function loadSession(): Promise<void> {
    try {
      session.value = await fetchMicrosoftSession()
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
    try {
      connector.value = await fetchMicrosoftConnector()
    } catch (error) {
      errorMessage.value = error instanceof Error ? error.message : 'Connector status unavailable.'
    }
  }

  async function runHarness(): Promise<void> {
    loading.value = true
    try {
      lastHarness.value = await runHarnessCalendarCrossing()
    } finally {
      loading.value = false
    }
  }

  return { session, connector, lastHarness, errorMessage, loading, loadSession, loadConnector, runHarness }
})
