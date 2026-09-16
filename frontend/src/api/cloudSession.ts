import { initializeApp } from 'firebase/app'
import {
  browserPopupRedirectResolver,
  getIdToken,
  GoogleAuthProvider,
  inMemoryPersistence,
  initializeAuth,
  signInWithPopup,
  signOut,
} from 'firebase/auth'
import { apiClient } from '@/api/client'
import { BoundaryMeshApiError } from '@/api/errors'
import { frontendEnv } from '@/config/env'

export function firebaseConfigured(): boolean {
  return Boolean(frontendEnv.firebaseApiKey && frontendEnv.firebaseAuthDomain && frontendEnv.firebaseProjectId)
}

let auth: ReturnType<typeof initializeAuth> | undefined

function firebaseAuth() {
  if (!firebaseConfigured()) throw new Error('Firebase sign-in is not configured for this deployment.')
  if (!auth) {
    const app = initializeApp({
      apiKey: frontendEnv.firebaseApiKey,
      authDomain: frontendEnv.firebaseAuthDomain,
      projectId: frontendEnv.firebaseProjectId,
    })
    auth = initializeAuth(app, {
      persistence: inMemoryPersistence,
      popupRedirectResolver: browserPopupRedirectResolver,
    })
  }
  return auth
}

export async function initializeCloudSession(): Promise<string | null> {
  await apiClient.request('/session/bootstrap')
  try {
    const response = await apiClient.request<{ email?: string }>('/session/me')
    return response.data.email ?? ''
  } catch (error) {
    if (error instanceof BoundaryMeshApiError && error.status === 401) return null
    throw error
  }
}

export async function signInOperator(): Promise<string> {
  const instance = firebaseAuth()
  const result = await signInWithPopup(instance, new GoogleAuthProvider())
  try {
    const idToken = await getIdToken(result.user)
    await apiClient.request('/session/login', { method: 'POST', body: { idToken } })
    const session = await apiClient.request<{ email?: string }>('/session/me')
    return session.data.email ?? ''
  } finally {
    await signOut(instance)
  }
}

export async function signOutOperator(): Promise<void> {
  await apiClient.request('/session/logout', { method: 'POST', body: {} })
}
