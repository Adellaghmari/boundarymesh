/// <reference types="vite/client" />

interface ImportMetaEnv {
  /**
   * Public browser configuration only. Never put secrets in VITE_* variables.
   * Empty means same-origin / Vite proxy.
   */
  readonly VITE_API_BASE_URL?: string
  readonly VITE_API_TIMEOUT_MS?: string
  readonly VITE_APP_MODE?: 'local' | 'cloud'
  /** Firebase web configuration is public; it is not an Admin credential. */
  readonly VITE_FIREBASE_API_KEY?: string
  readonly VITE_FIREBASE_AUTH_DOMAIN?: string
  readonly VITE_FIREBASE_PROJECT_ID?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
