/**
 * Frontend-safe Vite environment.
 *
 * VITE_* values are compiled into the browser bundle. They are public
 * configuration, not secrets. Do not place tokens, credentials, or keys here.
 */
export interface FrontendEnv {
  apiBaseUrl: string
  apiTimeoutMs: number
  mode: 'local' | 'cloud'
  firebaseApiKey: string
  firebaseAuthDomain: string
  firebaseProjectId: string
  firebaseStorageBucket: string
  firebaseMessagingSenderId: string
  firebaseAppId: string
}

const mode = import.meta.env.VITE_APP_MODE === 'cloud' ? 'cloud' : 'local'

function readPublicBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
  if (raw === '') {
    return ''
  }
  let parsed: URL
  try {
    parsed = new URL(raw)
  } catch {
    throw new Error('VITE_API_BASE_URL must be an absolute http(s) URL or empty.')
  }
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    throw new Error('VITE_API_BASE_URL must use http or https.')
  }
  return parsed.origin
}

function readTimeoutMs(): number {
  const raw = import.meta.env.VITE_API_TIMEOUT_MS?.trim()
  if (!raw) {
    return 8000
  }
  const value = Number.parseInt(raw, 10)
  if (!Number.isFinite(value) || value < 1000 || value > 60000) {
    return 8000
  }
  return value
}

export const frontendEnv: FrontendEnv = {
  apiBaseUrl: mode === 'cloud' ? '' : readPublicBaseUrl(),
  apiTimeoutMs: readTimeoutMs(),
  mode,
  firebaseApiKey: mode === 'cloud' ? firebaseProductionConfig.apiKey : import.meta.env.VITE_FIREBASE_API_KEY?.trim() ?? '',
  firebaseAuthDomain: mode === 'cloud' ? firebaseProductionConfig.authDomain : import.meta.env.VITE_FIREBASE_AUTH_DOMAIN?.trim() ?? '',
  firebaseProjectId: mode === 'cloud' ? firebaseProductionConfig.projectId : import.meta.env.VITE_FIREBASE_PROJECT_ID?.trim() ?? '',
  firebaseStorageBucket: mode === 'cloud' ? firebaseProductionConfig.storageBucket : import.meta.env.VITE_FIREBASE_STORAGE_BUCKET?.trim() ?? '',
  firebaseMessagingSenderId: mode === 'cloud' ? firebaseProductionConfig.messagingSenderId : import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID?.trim() ?? '',
  firebaseAppId: mode === 'cloud' ? firebaseProductionConfig.appId : import.meta.env.VITE_FIREBASE_APP_ID?.trim() ?? '',
}
import { firebaseProductionConfig } from './firebaseProductionConfig'
